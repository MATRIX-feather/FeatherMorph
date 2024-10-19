package xyz.nifeather.morph.misc.integrations.towny;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoTownBorderEvent;
import com.palmergames.bukkit.towny.event.player.PlayerExitsFromTownBorderEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Command.IPluginCommand;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.abilities.AbilityType;
import xyz.nifeather.morph.abilities.impl.FlyAbility;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.events.api.gameplay.PlayerMorphEvent;
import xyz.nifeather.morph.events.api.gameplay.PlayerUnMorphEvent;

import java.util.List;
import java.util.Objects;

public class TownyAdapter extends MorphPluginObject implements Listener
{
    private final TownyAPI townyAPI = TownyAPI.getInstance();

    private final Bindable<Boolean> allowFlyInWilderness = new Bindable<>(false);

    private final List<Player> blockedPlayers = ObjectLists.synchronize(new ObjectArrayList<>());

    public static final BooleanDataField allowMorphFlight = new BooleanDataField("allow_morph_flight");

    public boolean registerCommand(IPluginCommand command)
    {
        if (Objects.equals(command.getCommandName(), ""))
        {
            return false;
        }
        else
        {
            PluginCommand cmd = Bukkit.getPluginCommand(command.getCommandName());

            if (cmd != null && cmd.getExecutor().equals(plugin))
            {
                cmd.setExecutor(command);
                cmd.setTabCompleter(command);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(allowFlyInWilderness, ConfigOption.TOWNY_ALLOW_FLY_IN_WILDERNESS);

        allowFlyInWilderness.onValueChanged((o, n) ->
        {
            Bukkit.getOnlinePlayers().forEach(p ->
                    this.scheduleOn(p, () -> updatePlayer(p, null)));
        });

        if (!registerCommand(new TownyToggleFlightCommand(this)))
            logger.warn("Can't register flight toggle command for towny integration, expect problems!");
    }

    private boolean allowFlightAt(Player player, @Nullable Town town)
    {
        // Town == null -> Wilderness
        if (town == null)
            return allowFlyInWilderness.get();

        var resident = townyAPI.getResident(player);
        if (resident == null) return false;

        // 玩家城镇
        var playerTown = resident.getTownOrNull();

        // 如果这个town不支持飞行
        if (MetaDataUtil.hasMeta(town, allowMorphFlight) && !MetaDataUtil.getBoolean(town, allowMorphFlight))
            return false;

        // MetaDataUtil.setBoolean(town, allowMorphFlight, true, true);

        // 如果这个town信任这个玩家，那么true
        if (town.getTrustedResidents().contains(resident))
            return true;

        // 玩家城镇

        // 如果玩家没有城镇，返回false
        // 因为上面检查了野外和城镇的信任，这里应该没有问题。
        if (playerTown == null)
            return false;

        // 玩家就是城镇成员
        if (playerTown.getUUID() == town.getUUID())
            return true;

        // 盟友
        if (CombatUtil.isAlly(town, playerTown))
            return true;

        // 国家
        if (CombatUtil.isSameNation(town, playerTown))
            return true;

        return false;
    }

    @EventHandler
    public void onEnterPlot(PlayerEntersIntoTownBorderEvent e)
    {
        var player = e.getPlayer();
        updatePlayer(player, e.getEnteredTown());
    }

    @EventHandler
    public void onLeavePlot(PlayerExitsFromTownBorderEvent e)
    {
        if (allowFlyInWilderness.get()) return;

        updatePlayer(e.getPlayer(), null, true);
    }

    @EventHandler
    public void onPlayerMorph(PlayerMorphEvent e)
    {
        if (e.getState().containsAbility(AbilityType.CAN_FLY))
            updatePlayer(e.getPlayer(), null);
    }

    @EventHandler
    public void onPlayerUnmorph(PlayerUnMorphEvent e)
    {
        var player = e.getPlayer();
        this.blockedPlayers.remove(player);
        FlyAbility.unBlockPlayer(player, this);
    }

    // Folia没有提供监听玩家改变世界的事件，所以我们只能监听EntityAddToWorldEvent
    @EventHandler
    public void onPlayerChangeWorld(EntityAddToWorldEvent e)
    {
        if (!(e.getEntity() instanceof Player player)) return;

        updatePlayer(player, null);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        updatePlayer(e.getPlayer(), null);
    }

    @EventHandler
    public void onPlayerExit(PlayerQuitEvent e)
    {
        this.unblockPlayer(e.getPlayer());
    }

    public void updatePlayer(Player player, @Nullable Town town)
    {
        this.updatePlayer(player, town, false);
    }

    public void updatePlayer(Player player, @Nullable Town town, boolean noLookup)
    {
        if (!worldUsingTowny(player.getWorld()))
        {
            unblockPlayer(player);
            return;
        }

        if (town == null && !noLookup)
            town = townyAPI.getTown(player.getLocation());

        if (allowFlightAt(player, town))
            unblockPlayer(player);
        else
            blockPlayer(player);
    }

    private boolean worldUsingTowny(World world)
    {
        var townyWorld = townyAPI.getTownyWorld(world);
        if (townyWorld == null) return false;

        return townyWorld.isUsingTowny();
    }

    private void blockPlayer(Player player)
    {
        var playerAlreadyBlocked = blockedPlayers.contains(player);
        if (playerAlreadyBlocked) return;

        blockedPlayers.add(player);
        FlyAbility.blockPlayer(player, this);
    }

    private void unblockPlayer(Player player)
    {
        FlyAbility.unBlockPlayer(player, this);
        blockedPlayers.remove(player);
    }
}

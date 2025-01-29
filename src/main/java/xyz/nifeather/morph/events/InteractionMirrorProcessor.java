package xyz.nifeather.morph.events;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.events.api.gameplay.PlayerJoinedWithDisguiseEvent;
import xyz.nifeather.morph.events.api.gameplay.PlayerMorphEvent;
import xyz.nifeather.morph.events.api.gameplay.PlayerUnMorphEvent;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.DisguiseTypes;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class InteractionMirrorProcessor extends MorphPluginObject implements Listener
{
    public final Bindable<Boolean> allowSimulation = new Bindable<>(false);
    public final Bindable<Boolean> allowSneak = new Bindable<>(false);
    public final Bindable<Boolean> allowSwap = new Bindable<>(false);
    public final Bindable<Boolean> allowDrop = new Bindable<>(false);
    public final Bindable<Boolean> allowHotBar = new Bindable<>(false);
    //public final Bindable<Boolean> ignoreDisguised = new Bindable<>(false);
    public final Bindable<String> selectionMode = new Bindable<>(InteractionMirrorProcessor.InteractionMirrorSelectionMode.BY_NAME);
    public final Bindable<Boolean> debugOutput = new Bindable<>(false);

    @Initializer
    private void load(MorphConfigManager config)
    {
        config.bind(allowSimulation, ConfigOption.MIRROR_BEHAVIOR_DO_SIMULATION);
        config.bind(allowSneak, ConfigOption.MIRROR_BEHAVIOR_SNEAK);
        config.bind(allowSwap, ConfigOption.MIRROR_BEHAVIOR_SWAP_HAND);
        config.bind(allowDrop, ConfigOption.MIRROR_BEHAVIOR_DROP);
        config.bind(allowHotBar, ConfigOption.MIRROR_BEHAVIOR_HOTBAR);
        //config.bind(ignoreDisguised, ConfigOption.MIRROR_IGNORE_DISGUISED);

        config.bind(selectionMode, ConfigOption.MIRROR_SELECTION_MODE);

        config.bind(debugOutput, ConfigOption.DEBUG_OUTPUT);
    }

    @Resolved(shouldSolveImmediately = true)
    private ExecutorHub executorHub;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSneak(PlayerToggleSneakEvent e)
    {
        if (!allowSneak.get()) return;

        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
            executor.onSneak(e.getPlayer(), e.isSneaking());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent e)
    {
        if (!allowSwap.get()) return;

        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
            executor.onSwapHand(e.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHotbarChange(PlayerItemHeldEvent e)
    {
        if (!allowHotBar.get()) return;

        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
            executor.onHotbarChange(e.getPlayer(), e.getNewSlot());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerStopUsingItem(PlayerStopUsingItemEvent e)
    {
        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
            executor.onStopUsingItem(e.getPlayer(), e.getItem());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHurtEntity(EntityDamageByEntityEvent e)
    {
        if (!allowSimulation.get()) return;

        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getEntity() instanceof Player hurted)) return;

        AtomicBoolean shouldCancel = new AtomicBoolean(false);

        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
           shouldCancel.set(executor.onHurtEntity(damager, hurted));
        });

        if (shouldCancel.get())
            e.setCancelled(true);
    }

    /**
     * todo: SwingEvent应当只相应方块破坏
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwing(PlayerArmSwingEvent e)
    {
        if (!allowSimulation.get()) return;

        AtomicBoolean shouldCancel = new AtomicBoolean(false);

        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
            shouldCancel.set(executor.onSwing(e.getPlayer()));
        });

        if (shouldCancel.get())
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        if (!allowSimulation.get()) return;

        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
            executor.onInteract(e.getPlayer(), e.getAction());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e)
    {
        if (!allowSimulation.get()) return;

        executorHub.executeIfExists(selectionMode.get(), executor ->
        {
            executor.onInteract(e.getPlayer(), Action.RIGHT_CLICK_AIR);
        });
    }

    public static class InteractionMirrorSelectionMode
    {
        public static final String BY_NAME = "BY_NAME";
        public static final String BY_SIGHT = "BY_SIGHT";
        public static final String BY_RANGE = "BY_RANGE";

        public static List<String> values()
        {
            return List.of(BY_NAME, BY_SIGHT, BY_RANGE);
        }

        public static List<String> valuesLowerCase()
        {
            return values().stream().map(String::toLowerCase).toList();
        }
    }

    public record PlayerInfo(@Nullable Player target, @NotNull String targetName)
    {
        public static final String notSetStr = "~NOTSET";
    }

    //region Morph events

    @EventHandler
    public void onPlayerExit(PlayerQuitEvent e)
    {
        executorHub.unregisterControl(e.getPlayer());
    }

    @EventHandler
    public void onPlayerMorph(PlayerMorphEvent e)
    {
        addOrRemoveFromMirrorMap(e.state, e.getPlayer());
    }

    @EventHandler
    public void onPlayerUnMorph(PlayerUnMorphEvent e)
    {
        executorHub.unregisterControl(e.getPlayer());
    }

    @EventHandler
    public void onJoinedWithState(PlayerJoinedWithDisguiseEvent e)
    {
        addOrRemoveFromMirrorMap(e.state, e.getPlayer());
    }

    private void addOrRemoveFromMirrorMap(DisguiseState state, Player player)
    {
        var id = state.getDisguiseIdentifier();

        if (DisguiseTypes.fromId(id) == DisguiseTypes.PLAYER)
            executorHub.registerControl(player, DisguiseTypes.PLAYER.toStrippedId(id));
        else
            executorHub.unregisterControl(player);
    }

    //endregion Morph events
}

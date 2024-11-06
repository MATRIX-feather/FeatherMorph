package xyz.nifeather.morph.events;

import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.RevealingHandler;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.api.v0.events.gameplay.PlayerMorphEvent;
import xyz.nifeather.morph.api.v0.events.gameplay.PlayerUnMorphEvent;
import xyz.nifeather.morph.misc.DisguiseTypes;
import xyz.nifeather.morph.misc.NmsRecord;

public class RevealingEventProcessor extends MorphPluginObject implements Listener
{
    @Resolved(shouldSolveImmediately = true)
    private RevealingHandler handler;

    @Resolved(shouldSolveImmediately = true)
    private PlayerTracker tracker;

    private final Bindable<Boolean> doRevealing = new Bindable<>(true);

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(doRevealing, ConfigOption.REVEALING);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        handler.updateStatePlayerInstance(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        if (!e.hasBlock()) return;
        if (e.getClickedBlock() == null) return;
        if (!doRevealing.get()) return;

        if (!e.getClickedBlock().getType().isInteractable()) return;

        var player = e.getPlayer();
        if (tracker.isDuplicatedRightClick(player)) return;

        var revealingState = handler.getRevealingState(player);
        if (!revealingState.haveBindingState()) return;
        if (revealingState.bindingState.getDisguiseType() == DisguiseTypes.PLAYER) return;

        revealingState.addBaseValue(RevealingHandler.RevealingDiffs.INTERACT);
    }

    @EventHandler
    public void onPlayerDeath(EntityDeathEvent e)
    {
        if (!(e.getEntity() instanceof Player player)) return;

        var revState = handler.getRevealingState(player);
        revState.setBaseValue(0);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e)
    {
        if (!doRevealing.get()) return;
        var revState = handler.getRevealingState(e.getPlayer());

        if (!revState.haveBindingState()) return;
        if (revState.bindingState.getDisguiseType() == DisguiseTypes.PLAYER) return;
        revState.addBaseValue(RevealingHandler.RevealingDiffs.INTERACT_ENTITY);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e)
    {
        if (!doRevealing.get()) return;
        if (!(e.getEntity() instanceof Player player)) return;

        var revState = handler.getRevealingState(player);

        if (!revState.haveBindingState()) return;
        if (revState.bindingState.getDisguiseType() == DisguiseTypes.PLAYER) return;
        revState.addBaseValue(RevealingHandler.RevealingDiffs.TAKE_DAMAGE);
    }

    @EventHandler
    public void onPlayerDamageEntities(EntityDamageByEntityEvent e)
    {
        if (!doRevealing.get()) return;
        if (!(e.getDamager() instanceof Player player)) return;

        var revState = handler.getRevealingState(player);

        if (!revState.haveBindingState()) return;
        if (revState.bindingState.getDisguiseType() == DisguiseTypes.PLAYER) return;
        revState.addBaseValue(RevealingHandler.RevealingDiffs.DEAL_DAMAGE);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e)
    {
        if (!doRevealing.get()) return;

        // 玩家破坏方块 -> 揭示值+5
        var revealingState = handler.getRevealingState(e.getPlayer());

        if (!revealingState.haveBindingState()) return;
        if (revealingState.bindingState.getDisguiseType() == DisguiseTypes.PLAYER) return;
        revealingState.addBaseValue(RevealingHandler.RevealingDiffs.BLOCK_BREAK);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e)
    {
        if (!doRevealing.get()) return;
        var revealingState = handler.getRevealingState(e.getPlayer());

        if (!revealingState.haveBindingState()) return;
        if (revealingState.bindingState.getDisguiseType() == DisguiseTypes.PLAYER) return;
        revealingState.addBaseValue(RevealingHandler.RevealingDiffs.BLOCK_PLACE);
    }

    @EventHandler
    public void onPlayerMorph(PlayerMorphEvent e)
    {
        if (!doRevealing.get()) return;

        var player = e.getPlayer();
        var mobsNearby = player.getNearbyEntities(16, 16, 16);
        var nmsPlayer = NmsRecord.ofPlayer(player);

        var revealingState = this.handler.getRevealingState(player);
        revealingState.bindingState = e.getState();

        if (revealingState.bindingState.getDisguiseType() == DisguiseTypes.PLAYER) return;
        revealingState.addBaseValue(RevealingHandler.RevealingLevel.SUSPECT.getValue() * 0.1f, true);

        if (!mobsNearby.isEmpty())
        {
            // 如果变形时附近有生物在向着玩家，并且距离过近，那么直接拉满揭示值
            for (var entity : mobsNearby)
            {
                if (!(entity instanceof CraftMob craftMob)) continue;
                if (craftMob.getHandle().getTarget() == nmsPlayer)
                {
                    var base = revealingState.getBaseValue();
                    revealingState.setBaseValue(Math.max(base, RevealingHandler.RevealingDiffs.ALREADY_TARGETED));
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerUnmorph(PlayerUnMorphEvent e)
    {
        var revealingState = this.handler.getRevealingState(e.getPlayer());
        revealingState.bindingState = null;
    }
}

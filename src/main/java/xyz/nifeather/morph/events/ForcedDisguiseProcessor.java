package xyz.nifeather.morph.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.api.v0.events.gameplay.PlayerMorphEarlyEvent;
import xyz.nifeather.morph.api.v0.events.gameplay.PlayerUnMorphEarlyEvent;
import xyz.nifeather.morph.api.v0.disguise.MorphParameters;

public class ForcedDisguiseProcessor extends MorphPluginObject implements Listener
{
    @Initializer
    private void load()
    {
        config.bind(forcedId, ConfigOption.FORCED_DISGUISE);

        forcedId.onValueChanged((o, n) ->
        {
            doForcedDisguise.set(!n.equals(MorphManager.forcedDisguiseNoneId));

            if (doForcedDisguise.get())
            {
                logger.info("Config changed, re-disguising players...");
                this.addSchedule(() ->
                {
                    var players = Bukkit.getOnlinePlayers();
                    players.forEach(p -> this.doDisguise(p, n));
                });
            }
        }, true);
    }

    private final Bindable<String> forcedId = new Bindable<>(MorphManager.forcedDisguiseNoneId);

    private final Bindable<Boolean> doForcedDisguise = new Bindable<>(false);

    @Resolved(shouldSolveImmediately = true)
    private MorphConfigManager config;

    @Resolved(shouldSolveImmediately = true)
    private MorphManager manager;

    @EventHandler
    public void onJoin(PlayerJoinEvent e)
    {
        if (doForcedDisguise.get())
            doDisguise(e.getPlayer(), forcedId.get());
    }

    private void doDisguise(Player player, String targetId)
    {
        logger.info("Trying to disguise %s as %s".formatted(player.getName(), targetId));

        var state = manager.getDisguiseStateFor(player);

        if (state != null)
            manager.unMorph(Bukkit.getConsoleSender(), player, true, true);

        var parameters = MorphParameters.create(player, targetId)
                .setSource(Bukkit.getConsoleSender())
                .setBypassAvailableCheck(true)
                .setBypassPermission(true);

        var success = manager.morph(parameters);

        if (!success)
            logger.error("Unable to disguise %s as %s".formatted(player.getName(), targetId));
    }

    @EventHandler
    public void onUnmorph(PlayerUnMorphEarlyEvent e)
    {
        if (!doForcedDisguise.get()) return;

        e.setCancelled(true);
        //var player = e.getPlayer();

        //logger.info("%s undisguised themselves, re-disguising...".formatted(player.getName()));
        //doDisguise(player, forcedId.get());
    }

    @EventHandler
    public void onMorph(PlayerMorphEarlyEvent e)
    {
        if (!doForcedDisguise.get()) return;

        var id = e.getTargetId();
        if (id.equals(forcedId.get())) return;

        e.setCancelled(true);
    }
}

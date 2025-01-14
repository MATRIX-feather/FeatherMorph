package xyz.nifeather.morph.abilities.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.abilities.MorphAbility;
import xyz.nifeather.morph.abilities.options.ReduceDamageOption;
import xiamomc.pluginbase.Annotations.Resolved;

public abstract class DamageReducingAbility<T extends ReduceDamageOption> extends MorphAbility<T>
{
    protected abstract EntityDamageEvent.DamageCause getTargetCause();

    @Resolved
    private MorphManager morphs;

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTookDamage(EntityDamageEvent e)
    {
        if (!(e.getEntity() instanceof Player player) || !isPlayerApplied(player))
            return;

        if (e.getCause() != getTargetCause())
            return;

        var state = morphs.getDisguiseStateFor(player);
        assert state != null;

        var dmgOption = this.getOptionFor(state);

        if (dmgOption == null)
            return;

        var damage = e.getDamage();
        var percentage = dmgOption.isPercentage();

        damage = percentage
                ? damage * (1 - dmgOption.getReduceAmount())
                : damage - dmgOption.getReduceAmount();

        e.setDamage(Math.max(0d, damage));

        if (damage <= 0d)
            e.setCancelled(true);
    }
}

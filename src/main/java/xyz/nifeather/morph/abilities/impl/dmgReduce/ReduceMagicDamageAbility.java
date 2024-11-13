package xyz.nifeather.morph.abilities.impl.dmgReduce;

import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.abilities.AbilityType;
import xyz.nifeather.morph.abilities.impl.DamageReducingAbility;
import xyz.nifeather.morph.abilities.options.ReduceDamageOption;

public class ReduceMagicDamageAbility extends DamageReducingAbility<ReduceDamageOption>
{
    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return AbilityType.REDUCES_MAGIC_DAMAGE;
    }

    @Override
    protected @NotNull ReduceDamageOption createOption()
    {
        return new ReduceDamageOption();
    }

    @Override
    protected EntityDamageEvent.DamageCause getTargetCause()
    {
        return EntityDamageEvent.DamageCause.MAGIC;
    }
}

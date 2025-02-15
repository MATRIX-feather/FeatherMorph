package xyz.nifeather.morph.abilities.impl.onAttack;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.abilities.AbilityType;
import xyz.nifeather.morph.abilities.impl.OnAttackAbility;
import xyz.nifeather.morph.abilities.options.PotionEffectOption;
import xiamomc.pluginbase.Annotations.Resolved;

public class PotionOnAttackAbility extends OnAttackAbility<PotionEffectOption>
{
    /**
     * 获取此被动技能的ID
     *
     * @return {@link NamespacedKey}
     */
    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return AbilityType.POTION_ON_ATTACK;
    }

    @Override
    protected @NotNull PotionEffectOption createOption()
    {
        return new PotionEffectOption();
    }

    @Resolved
    private MorphManager manager;

    @Override
    protected boolean requireValidOption()
    {
        return true;
    }

    @Override
    protected void onAttack(LivingEntity hurt, Player source)
    {
        var option = this.getOptionFor(manager.getDisguiseStateFor(source));

        if (option == null || !option.isValid()) return;

        var effectType = PotionEffectType.getByName(option.effectId.replace("minecraft:", ""));
        if (effectType == null) return;

        var effect = new PotionEffect(effectType, option.duration, option.amplifier, false);
        hurt.addPotionEffect(effect);
    }
}

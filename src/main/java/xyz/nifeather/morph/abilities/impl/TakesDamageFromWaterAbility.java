package xyz.nifeather.morph.abilities.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.abilities.AbilityType;
import xyz.nifeather.morph.abilities.MorphAbility;
import xyz.nifeather.morph.abilities.options.TakesDamageFromWaterOption;
import xyz.nifeather.morph.misc.DisguiseState;

public class TakesDamageFromWaterAbility extends MorphAbility<TakesDamageFromWaterOption>
{
    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return AbilityType.TAKES_DAMAGE_FROM_WATER;
    }

    @Override
    public boolean handle(Player player, DisguiseState state)
    {
        if (player.isInWaterOrRainOrBubbleColumn())
        {
            var dmgOption = this.getOptionFor(state);

            player.damage(dmgOption == null ? 1d : dmgOption.damageAmount);
        }

        return true;
    }

    @Override
    protected @NotNull TakesDamageFromWaterOption createOption()
    {
        return new TakesDamageFromWaterOption();
    }
}

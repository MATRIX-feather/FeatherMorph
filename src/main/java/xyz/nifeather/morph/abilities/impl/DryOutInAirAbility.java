package xyz.nifeather.morph.abilities.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.abilities.AbilityType;
import xyz.nifeather.morph.abilities.MorphAbility;
import xyz.nifeather.morph.abilities.options.DryoutAbilityOption;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.utilities.DamageSourceUtils;
import xyz.nifeather.morph.utilities.MathUtils;

public class DryOutInAirAbility extends MorphAbility<DryoutAbilityOption>
{
    /**
     * 获取此被动技能的ID
     *
     * @return {@link NamespacedKey}
     */
    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return AbilityType.DRYOUT_IN_AIR;
    }

    @Override
    public boolean handle(Player player, DisguiseState state)
    {
        this.updateOxygen(player, this.getOptionFor(state));
        return super.handle(player, state);
    }

    @Override
    protected @NotNull DryoutAbilityOption createOption()
    {
        return new DryoutAbilityOption();
    }

    private void updateOxygen(Player player, @Nullable DryoutAbilityOption option)
    {
        if (option == null)
            return;

        var nmsPlayer = NmsRecord.ofPlayer(player);
        var air = nmsPlayer.getAirSupply();

        //LivingEntity#increaseAirSupply()
        air -= (option.includeRain ? nmsPlayer.isInWater() : nmsPlayer.isInWaterOrRain()) ? (-5) : 5;
        air = MathUtils.clamp(-20, nmsPlayer.getMaxAirSupply(), air);

        if (air <= -20)
        {
            var sr = DamageSourceUtils.toNotScalable(nmsPlayer.level().damageSources().dryOut())
                            .bypassEverything();

            nmsPlayer.hurt(sr, 2);
            air = 0;
        }

        nmsPlayer.setAirSupply(air);
    }
}

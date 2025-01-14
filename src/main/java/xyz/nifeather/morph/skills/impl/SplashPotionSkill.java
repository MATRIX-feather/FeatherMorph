package xyz.nifeather.morph.skills.impl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.skills.MorphSkill;
import xyz.nifeather.morph.skills.SkillType;
import xyz.nifeather.morph.skills.options.NoOpConfiguration;
import xyz.nifeather.morph.storage.skill.ISkillOption;
import xyz.nifeather.morph.storage.skill.SkillAbilityConfiguration;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class SplashPotionSkill extends MorphSkill<NoOpConfiguration>
{
    /**
     * 执行伪装的主动技能
     *
     * @param player        玩家
     * @param state         {@link DisguiseState}
     * @param configuration 此技能的整体配置，包括ID、冷却等
     * @param option        此技能的详细设置
     * @return 执行后的冷却长度
     */
    @Override
    public int executeSkill(Player player, DisguiseState state, SkillAbilityConfiguration configuration, NoOpConfiguration option)
    {
        var launchedProjectile = launchProjectile(player, EntityType.POTION, 0.4f);

        if (launchedProjectile == null)
        {
            logger.error("Error summoning splash potion: null");
            return configuration.getCooldown();
        }

        if (!(launchedProjectile instanceof ThrownPotion thrownPotion))
        {
            logger.error("Error summoning splash potion: Excepted ThrownPotion, but get %s".formatted(launchedProjectile.getClass()));
            return configuration.getCooldown();
        }

        var meta = thrownPotion.getPotionMeta();
        var info = validTypes[random.nextInt(0, validTypes.length)];
        var potionEffect = new PotionEffect(info.type, info.duration, info.amplifier, false, true);
        meta.addCustomEffect(potionEffect, true);

        Potion targetPotion = null;
        var potionRef = BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(info.type.getKey().asString()))
                .orElse(null);

        if (potionRef != null && potionRef.isBound())
            targetPotion = potionRef.value();

        if (info.type == PotionEffectType.INSTANT_HEALTH)
            targetPotion = Potions.HEALING.value();
        else if (info.type == PotionEffectType.INSTANT_DAMAGE)
            targetPotion = Potions.HARMING.value();

        var color = new AtomicInteger();

        if (targetPotion != null)
            targetPotion.getEffects().stream().findFirst().ifPresent(o -> color.set(o.getEffect().value().getColor()));

        meta.setColor(Color.fromRGB(color.get()));

        thrownPotion.setPotionMeta(meta);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 1, 1);

        return configuration.getCooldown();
    }

    private final PotionInfo[] validTypes = new PotionInfo[]
    {
            PotionInfo.of(PotionEffectType.REGENERATION, 45 * 20, 0),
            PotionInfo.of(PotionEffectType.REGENERATION, 20, 0),
            PotionInfo.of(PotionEffectType.SLOWNESS, 90 * 20, 0),
            PotionInfo.of(PotionEffectType.POISON, 45 * 20, 0),
            PotionInfo.of(PotionEffectType.WEAKNESS, 90 * 20, 0),
            PotionInfo.of(PotionEffectType.INSTANT_DAMAGE, 20, 0)
    };

    private record PotionInfo(PotionEffectType type, int duration, int amplifier)
    {
        public static PotionInfo of(PotionEffectType type, int duration, int amp)
        {
            return new PotionInfo(type, duration, amp);
        }
    }

    private final Random random = new Random();

    /**
     * 获取要应用的技能ID
     *
     * @return 技能ID
     */
    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return SkillType.WITCH;
    }

    /**
     * 获取和此技能对应的{@link ISkillOption}实例
     *
     * @return {@link ISkillOption}
     */
    @Override
    public NoOpConfiguration getOptionInstance()
    {
        return NoOpConfiguration.instance;
    }
}

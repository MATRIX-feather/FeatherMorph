package xyz.nifeather.morph.abilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.abilities.impl.*;
import xyz.nifeather.morph.abilities.impl.potion.*;
import xyz.nifeather.morph.abilities.impl.onAttack.ExtraKnockbackAbility;
import xyz.nifeather.morph.abilities.impl.onAttack.PotionOnAttackAbility;
import xyz.nifeather.morph.events.api.lifecycle.AbilitiesFinishedInitializeEvent;
import xyz.nifeather.morph.storage.skill.ISkillOption;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.storage.skill.SkillsConfigurationStoreNew;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbilityManager extends MorphPluginObject
{
    private final List<IMorphAbility<?>> registedAbilities = new ObjectArrayList<>();

    @Resolved
    private SkillsConfigurationStoreNew store;

    /**
     * 注册一个被动技能
     *
     * @param ability 技能ID
     * @return 操作是否成功
     */
    public boolean registerAbility(IMorphAbility<?> ability)
    {
        //logger.info("Registering ability: " + ability.getIdentifier().asString());

        if (registedAbilities.stream().anyMatch(a -> a.getIdentifier().equals(ability.getIdentifier())))
        {
            logger.error("Can't register ability: Another ability instance has already registered as " + ability.getIdentifier().asString() + "!");
            return false;
        }

        registedAbilities.add(ability);

        Bukkit.getPluginManager().registerEvents(ability, plugin);
        return true;
    }

    /**
     * 获取所有已注册的被动
     *
     * @return 被动技能列表
     */
    public List<IMorphAbility<?>> getRegistedAbilities()
    {
        return new ObjectArrayList<>(registedAbilities);
    }

    /**
     * 注册一批被动技能
     *
     * @param abilities ID列表
     * @return 操作是否成功
     */
    public boolean registerAbilities(List<IMorphAbility<?>> abilities)
    {
        var success = new AtomicBoolean(false);

        abilities.forEach(a ->
        {
            if (!registerAbility(a))
                success.set(false);
        });

        return success.get();
    }

    @Initializer
    private void load()
    {
        registerAbilities(ObjectList.of(
                new BreatheUnderWaterAbility(),
                new BurnsUnderSunAbility(),
                new FeatherFallingAbility(),
                new FireResistanceAbility(),
                new FlyAbility(),
                new JumpBoostAbility(),
                new NightVisionAbility(),
                new NoFallDamageAbility(),
                new ReduceFallDamageAbility(),
                new ReduceMagicDamageAbility(),
                new SmallJumpBoostAbility(),
                new SnowyAbility(),
                new SpeedBoostAbility(),
                new TakesDamageFromWaterAbility(),
                new WardenLessAwareAbility(),
                new WardenAbility(),
                new ChatOverrideAbility(),
                new BossbarAbility(),
                new NoSweetBushDamageAbility(),
                new AttributeModifyingAbility(),
                new HealsFromEntityAbility(),
                new ExtraKnockbackAbility(),
                new DryOutInAirAbility(),
                new PotionOnAttackAbility(),
                new SpiderAbility()
        ));

        Bukkit.getPluginManager().callEvent(new AbilitiesFinishedInitializeEvent(this));
    }

    @NotNull
    public Map<NamespacedKey, ISkillOption> getOptionsFor(String disguiseIdentifier)
    {
        var configuration = store.get(disguiseIdentifier);
        if (configuration == null) return new Object2ObjectOpenHashMap<>();

        Map<NamespacedKey, ISkillOption> optionMap = new ConcurrentHashMap<>();
        configuration.getAbilitiyIdentifiers().forEach(a ->
        {
            var idKey = NamespacedKey.fromString(a);

            if (idKey == null)
            {
                logger.warn("Invalid ability ID: %s".formatted(a));
                return;
            }

            var abilityInstance = this.getAbility(idKey);
            if (abilityInstance == null) return;

            var options = configuration.getAbilityOptions(abilityInstance);

            if (options != null)
                optionMap.put(idKey, options);
            else
                logger.warn("Null option for ability %s under %s, is everything correct?".formatted(abilityInstance.getIdentifier(), disguiseIdentifier));
        });

        return optionMap;
    }

    @Nullable
    public IMorphAbility<?> getAbility(@Nullable NamespacedKey abilityIdentifier)
    {
        if (abilityIdentifier == null) return null;

        var val = registedAbilities.stream()
                .filter(a -> a.getIdentifier().equals(abilityIdentifier)).findFirst().orElse(null);

        if (val == null)
            logger.warn("Unknown ability: " + abilityIdentifier.asString());

        return val;
    }

    /**
     * 为某个伪装ID获取被动技能
     *
     * @param disguiseIdentifier 伪装ID
     * @return 被动技能列表
     */
    @NotNull
    public List<IMorphAbility<?>> getAbilitiesFor(String disguiseIdentifier)
    {
        return this.getAbilitiesFor(disguiseIdentifier, false);
    }

    /**
     * @param disguiseIdentifier 目标伪装
     * @param noFallback 是否要搜索命名空间的默认配置
     */
    @NotNull
    public List<IMorphAbility<?>> getAbilitiesFor(String disguiseIdentifier, boolean noFallback)
    {
        var configuration = store.get(disguiseIdentifier);

        if (configuration != null)
        {
            List<IMorphAbility<?>> abilities = new ObjectArrayList<>();
            configuration.getAbilitiyIdentifiers().forEach(id ->
            {
                var instance = this.getAbility(NamespacedKey.fromString(id));
                if (instance != null) abilities.add(instance);
            });

            return abilities;
        }
        else if (!noFallback)
        {
            var idSpilt = disguiseIdentifier.split(":", 2);
            if (idSpilt.length < 1) return List.of();

            var idNew = idSpilt[0] + ":" + MorphManager.disguiseFallbackName;

            return getAbilitiesFor(idNew, true);
        }

        return List.of();
    }
}

package xyz.nifeather.morph.utilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.MorphPlugin;
import xyz.nifeather.morph.misc.DisguiseTypes;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class EntityTypeUtils
{
    @Nullable
    public static EntityType fromString(String key, boolean nullWhenUnknown)
    {
        var val = fromString(key);

        return val == EntityType.UNKNOWN ? null : val;
    }

    public static EntityType fromString(String key)
    {
        if (key.startsWith(DisguiseTypes.PLAYER.getNameSpace() + ":")) return EntityType.PLAYER;

        return Arrays.stream(EntityType.values())
                .filter(t -> !t.equals(EntityType.UNKNOWN) && t.getKey().asString().equals(key))
                .findFirst().orElse(EntityType.UNKNOWN);
    }

    private static final Map<EntityType, Class<? extends Entity>> nmsClassMap = new Object2ObjectOpenHashMap<>();
    private static final Map<EntityType, SoundInfo> typeSoundMap = new Object2ObjectArrayMap<>();

    static
    {
        nmsClassMap.put(EntityType.PLAYER, Player.class);
        typeSoundMap.put(EntityType.BEE, new SoundInfo(SoundEvents.BEE_LOOP, SoundSource.NEUTRAL, 120, 1));
        typeSoundMap.put(EntityType.ENDER_DRAGON, new SoundInfo(SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.HOSTILE,100, 5));
        typeSoundMap.put(EntityType.WOLF, new SoundInfo(SoundEvents.WOLF_AMBIENT, SoundSource.NEUTRAL, 80, 1));
    }

    public record SoundInfo(@Nullable SoundEvent sound, SoundSource source, int interval, float volume)
    {
    }

    // Hope this will work with Folia
    // I guess it will...
    public static <T extends Entity> T createEntityThenDispose(net.minecraft.world.entity.EntityType<T> nmsType, World world)
    {
        var serverWorld = ((CraftWorld) world).getHandle();

        return nmsType.create(serverWorld, EntityTypeUtils::scheduleEntityDiscard, BlockPos.ZERO, EntitySpawnReason.COMMAND, false, false);
    }

    @NotNull
    public static SoundInfo getAmbientSound(EntityType bukkitType, World tickingWorld)
    {
        if (bukkitType == EntityType.UNKNOWN)
            return new SoundInfo(null, SoundSource.PLAYERS, Integer.MAX_VALUE, 1);

        var cache = typeSoundMap.getOrDefault(bukkitType, null);
        if (cache != null) return cache;

        var entity = createEntityThenDispose(getNmsType(bukkitType), tickingWorld);

        if (entity instanceof Mob mob)
        {
            var source = mob.getSoundSource();
            var sound = mob.getAmbientSound0();
            var interval = mob.getAmbientSoundInterval();

            var rec = new SoundInfo(sound, source, interval, mob.getSoundVolume());
            typeSoundMap.put(bukkitType, rec);

            return rec;
        }

        return new SoundInfo(null, SoundSource.PLAYERS, Integer.MAX_VALUE, 1);
    }

    private static final Map<EntityType, net.minecraft.world.entity.EntityType<?>> nmsTypeMap = new Object2ObjectArrayMap<>();

    @Nullable
    public static net.minecraft.world.entity.EntityType<?> getNmsType(@NotNull EntityType bukkitType)
    {
        var cachedResult = nmsTypeMap.get(bukkitType);
        if (cachedResult != null) return cachedResult;

        if (bukkitType == EntityType.UNKNOWN) return null;

        var result = net.minecraft.world.entity.EntityType.byString(bukkitType.key().asString())
                .orElse(null);

        nmsTypeMap.put(bukkitType, result);

        return result;
    }

    @Nullable
    public static Class<? extends Entity> getNmsClass(@NotNull EntityType type, World tickingWorld)
    {
        var cache = nmsClassMap.getOrDefault(type, null);
        if (cache != null) return cache;

        var nmsType = net.minecraft.world.entity.EntityType.byString(type.key().asString())
                .orElse(null);

        if (nmsType == null)
        {
            nmsClassMap.put(type, null);
            return null;
        }

        var entity = createEntityThenDispose(nmsType, tickingWorld);

        if (entity == null)
        {
            nmsClassMap.put(type, null);
            return null;
        }

        nmsClassMap.put(type, entity.getClass());
        return entity.getClass();
    }

    private static void scheduleEntityDiscard(Entity nmsEntity)
    {
        var entity = nmsEntity.getBukkitEntity();
        entity.getScheduler()
                .run(MorphPlugin.getInstance(), retiredTask -> {}, entity::remove);
    }

    public static boolean hasBabyVariant(EntityType type)
    {
        return switch (type)
        {
            case COW, SHEEP, BEE, CAMEL, CAT, CHICKEN, DONKEY,
                 FOX, GOAT, HORSE, LLAMA, MOOSHROOM, MULE, TRADER_LLAMA, VILLAGER,
                    OCELOT, PANDA, PIG, POLAR_BEAR, RABBIT, SNIFFER, TURTLE, WOLF,
                    HOGLIN, ZOMBIE, ZOMBIE_VILLAGER, PIGLIN, HUSK, DROWNED, ZOMBIFIED_PIGLIN, STRIDER,
                    SKELETON_HORSE, ZOMBIE_HORSE, ZOGLIN -> true;

            default -> false;
        };
    }

    public static boolean isZombie(EntityType type)
    {
        return type == EntityType.ZOMBIE
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.DROWNED
                || type == EntityType.HUSK;
    }

    public static boolean isSkeleton(EntityType type)
    {
        return type == EntityType.SKELETON
                || type == EntityType.STRAY
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.BOGGED;
    }

    public static boolean isZombiesHostile(EntityType type)
    {
        return isGolem(type)
                || type == EntityType.VILLAGER
                || type == EntityType.WANDERING_TRADER
                || type == EntityType.PLAYER;
    }

    public static boolean isGolem(EntityType type)
    {
        return type == EntityType.IRON_GOLEM || type == EntityType.SNOW_GOLEM;
    }

    public static boolean isBruteHostile(EntityType type)
    {
        return isGolem(type)
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.PLAYER;
    }

    public static boolean isPiglinHostile(EntityType type)
    {
        return type == EntityType.WITHER
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.PLAYER;
    }

    public static boolean isGuardianHostile(EntityType type)
    {
        return type == EntityType.PLAYER
                || type == EntityType.AXOLOTL
                || type == EntityType.SQUID
                || type == EntityType.GLOW_SQUID;
    }

    public static boolean isWitherSkeletonHostile(EntityType type)
    {
        return type == EntityType.PLAYER
                || type == EntityType.PIGLIN
                || type == EntityType.PIGLIN_BRUTE
                || isGolem(type);

        //todo: 小海龟
    }

    public static boolean isZoglinHostile(EntityType type)
    {
        return type != EntityType.CREEPER
                && type != EntityType.GHAST
                && type != EntityType.ZOGLIN;
    }

    public static boolean isWitherHostile(EntityType type)
    {
        return type == EntityType.ZOMBIE
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.DROWNED
                || type == EntityType.HUSK
                || type == EntityType.SKELETON
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.STRAY
                || type == EntityType.SKELETON_HORSE
                || type == EntityType.ZOMBIE_HORSE
                || type == EntityType.PHANTOM
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.WITHER
                || type == EntityType.GHAST;
    }

    public static boolean isRaiderHostile(EntityType type)
    {
        return type == EntityType.PLAYER
                || type == EntityType.VILLAGER
                || type == EntityType.WANDERING_TRADER
                || isGolem(type);
    }

    public static Set<EntityType> wardenLessAware()
    {
        return ObjectSet.of(EntityType.ARMOR_STAND, EntityType.WARDEN);
    }

    public static Set<EntityType> canFly()
    {
        return ObjectSet.of(EntityType.ALLAY, EntityType.ENDER_DRAGON,
                EntityType.BAT, EntityType.BEE, EntityType.BLAZE,
                EntityType.GHAST, EntityType.VEX, EntityType.PHANTOM, EntityType.WITHER,
                EntityType.PARROT);
    }

    public static Set<EntityType> hasFireResistance()
    {
        return ObjectSet.of(EntityType.MAGMA_CUBE, EntityType.BLAZE, EntityType.WITHER_SKELETON,
                EntityType.WITHER, EntityType.STRIDER, EntityType.ZOMBIFIED_PIGLIN, EntityType.GHAST,
                EntityType.WARDEN, EntityType.ENDER_DRAGON, EntityType.ZOGLIN);
    }

    public static Set<EntityType> takesDamageFromWater()
    {
        return ObjectSet.of(EntityType.ENDERMAN, EntityType.BLAZE, EntityType.SNOW_GOLEM, EntityType.STRIDER);
    }

    public static Set<EntityType> canBreatheUnderWater()
    {
        return ObjectSet.of(EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH,
                EntityType.SQUID, EntityType.GLOW_SQUID,
                EntityType.AXOLOTL, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.DOLPHIN,
                EntityType.TADPOLE, EntityType.DROWNED);
    }

    public static Set<EntityType> dryOutInAir()
    {
        return ObjectSet.of(EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH,
                EntityType.SQUID, EntityType.GLOW_SQUID,
                EntityType.AXOLOTL, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.DOLPHIN,
                EntityType.TADPOLE);
    }

    public static Set<EntityType> burnsUnderSun()
    {
        return ObjectSet.of(EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER,
                EntityType.SKELETON, EntityType.STRAY,
                EntityType.DROWNED, EntityType.PHANTOM);
    }

    public static Set<EntityType> spider()
    {
        return ObjectSet.of(EntityType.SPIDER, EntityType.CAVE_SPIDER);
    }

    public static Set<EntityType> alwaysNightVision()
    {
        return ObjectSet.of(EntityType.BAT, EntityType.ENDERMAN);
    }

    public static Set<EntityType> hasJumpBoost()
    {
        return Set.of(EntityType.MAGMA_CUBE);
    }

    public static Set<EntityType> hasSmallJumpBoost()
    {
        return Set.of(EntityType.RABBIT);
    }

    public static EntityType hasSpeedBoost()
    {
        return EntityType.HORSE;
    }

    public static Set<EntityType> noFallDamage()
    {
        return ObjectSet.of(EntityType.IRON_GOLEM, EntityType.CAT,
                EntityType.OCELOT, EntityType.SNOW_GOLEM, EntityType.MAGMA_CUBE,
                EntityType.CHICKEN, EntityType.SHULKER);
    }

    public static Set<EntityType> noFallDamage1()
    {
        return ObjectSet.of(EntityType.BAT, EntityType.BLAZE, EntityType.ENDER_DRAGON,
                EntityType.GHAST, EntityType.PARROT, EntityType.VEX,
                EntityType.WITHER);
    }

    public static EntityType reducesMagicDamage()
    {
        return EntityType.WITCH;
    }

    public static EntityType reducesFallDamage()
    {
        return EntityType.GOAT;
    }

    public static Set<EntityType> hasFeatherFalling()
    {
        return Set.of(EntityType.CHICKEN);
    }

    public static EntityType hasSnowTrail()
    {
        return EntityType.SNOW_GOLEM;
    }

    public static boolean saddleable(EntityType type)
    {
        return type == EntityType.HORSE || type == EntityType.MULE || type == EntityType.DONKEY
                || type == EntityType.CAMEL || type == EntityType.SKELETON_HORSE || type == EntityType.ZOMBIE_HORSE;
    }

    public static boolean hasBossBar(EntityType type)
    {
        return type == EntityType.ENDER_DRAGON || type == EntityType.WITHER;
    }

    public static float getDefaultFlyingSpeed(@Nullable EntityType type)
    {
        if (type == null) return 0.1f;

        return switch (type)
        {
            case ALLAY, BEE, BLAZE, VEX, BAT, PARROT -> 0.05f;
            case GHAST, PHANTOM -> 0.06f;
            case ENDER_DRAGON -> 0.15f;
            default -> 0.1f;
        };
    }

    @Deprecated
    @Nullable
    public static String getStepSound(EntityType type)
    {
        if (type == EntityType.PLAYER) return null;
        return "entity.%s.step".formatted(type.getKey().getKey());
    }

    @Nullable
    public static String getDamageSound(EntityType type)
    {
        if (type == EntityType.PLAYER) return null;

        if (type == EntityType.ARMOR_STAND)
            return Sound.ENTITY_ARMOR_STAND_HIT.key().asString();

        if (type == EntityType.TRADER_LLAMA)
            return Sound.ENTITY_LLAMA_HURT.key().asString();

        return "entity.%s.hurt".formatted(type.getKey().getKey());
    }

    public static boolean isEnemy(EntityType type)
    {
        if (type == EntityType.PLAYER) return false;
        if (type.getEntityClass() == null) return false;

        return Enemy.class.isAssignableFrom(type.getEntityClass());
    }
}

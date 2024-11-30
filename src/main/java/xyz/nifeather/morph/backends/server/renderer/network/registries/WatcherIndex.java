package xyz.nifeather.morph.backends.server.renderer.network.registries;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.SingleWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.*;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.horses.AbstractHorseWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.horses.HorseWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.horses.SkeletonHorseWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.horses.ZombieHorseWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.llama.LlamaWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.llama.TraderLlamaWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.slimemagma.MagmaWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.slimemagma.SlimeWatcher;

import java.util.Map;
import java.util.function.Function;

public class WatcherIndex
{
    public static WatcherIndex getInstance()
    {
        return WatcherInstanceLazyHolder.instance;
    }

    private static class WatcherInstanceLazyHolder
    {
        public static final WatcherIndex instance = new WatcherIndex();
    }

    public WatcherIndex()
    {
        setTypeWatcher(EntityType.PLAYER, PlayerWatcher::new);
        setTypeWatcher(EntityType.ALLAY, AllayWatcher::new);
        setTypeWatcher(EntityType.ARMOR_STAND, ArmorStandWatcher::new);
        setTypeWatcher(EntityType.SLIME, SlimeWatcher::new);
        setTypeWatcher(EntityType.MAGMA_CUBE, MagmaWatcher::new);
        setTypeWatcher(EntityType.GHAST, GhastWatcher::new);

        setTypeWatcher(EntityType.HORSE, HorseWatcher::new);
        setTypeWatcher(EntityType.SKELETON_HORSE, SkeletonHorseWatcher::new);
        setTypeWatcher(EntityType.ZOMBIE_HORSE, ZombieHorseWatcher::new);
        setTypeWatcher(EntityType.DONKEY, DonkeyWatcher::new);

        setTypeWatcher(EntityType.CAMEL, p -> new AbstractHorseWatcher(p, EntityType.CAMEL));

        setTypeWatcher(EntityType.LLAMA, LlamaWatcher::new);
        setTypeWatcher(EntityType.TRADER_LLAMA, TraderLlamaWatcher::new);

        setTypeWatcher(EntityType.FOX, FoxWatcher::new);
        setTypeWatcher(EntityType.PARROT, ParrotWatcher::new);
        setTypeWatcher(EntityType.CAT, CatWatcher::new);
        setTypeWatcher(EntityType.GOAT, GoatWatcher::new);

        setTypeWatcher(EntityType.RABBIT, RabbitWatcher::new);
        setTypeWatcher(EntityType.TROPICAL_FISH, TropicalFishWatcher::new);
        setTypeWatcher(EntityType.FROG, FrogWatcher::new);
        setTypeWatcher(EntityType.PANDA, PandaWatcher::new);

        setTypeWatcher(EntityType.VILLAGER, VillagerWatcher::new);
        setTypeWatcher(EntityType.SNOW_GOLEM, SnowGolemWatcher::new);
        setTypeWatcher(EntityType.CREEPER, CreeperWatcher::new);
        setTypeWatcher(EntityType.PIGLIN, PiglinWatcher::new);

        setTypeWatcher(EntityType.ZOMBIE, ZombieWatcher::new);
        setTypeWatcher(EntityType.ZOMBIE_VILLAGER, ZombieVillagerWatcher::new);
        setTypeWatcher(EntityType.ZOGLIN, ZoglinWatcher::new);
        setTypeWatcher(EntityType.HOGLIN, HoglinWatcher::new);
        setTypeWatcher(EntityType.GUARDIAN, GuardianWatcher::new);

        setTypeWatcher(EntityType.AXOLOTL, AxolotlWatcher::new);

        setTypeWatcher(EntityType.MOOSHROOM, MooshroomWatcher::new);
        setTypeWatcher(EntityType.SHEEP, SheepWatcher::new);

        setTypeWatcher(EntityType.WOLF, WolfWatcher::new);
        setTypeWatcher(EntityType.PHANTOM, PhantomWatcher::new);

        setTypeWatcher(EntityType.WARDEN, WardenWatcher::new);

        setTypeWatcher(EntityType.IRON_GOLEM, p -> new EHasAttackAnimationWatcher(p, EntityType.IRON_GOLEM));
        setTypeWatcher(EntityType.RAVAGER, p -> new EHasAttackAnimationWatcher(p, EntityType.RAVAGER));

        setTypeWatcher(EntityType.SNIFFER, SnifferWatcher::new);
        setTypeWatcher(EntityType.ARMADILLO, ArmadilloWatcher::new);
        setTypeWatcher(EntityType.SHULKER, ShulkerWatcher::new);
        setTypeWatcher(EntityType.PUFFERFISH, PufferfishWatcher::new);

        setTypeWatcher(EntityType.CREAKING, CreakingWatcher::new);
    }

    private void setTypeWatcher(EntityType type, Function<Player, SingleWatcher> func)
    {
        typeWatcherMap.put(type, func);
    }

    private final Map<EntityType, Function<Player, SingleWatcher>> typeWatcherMap = new Object2ObjectOpenHashMap<>();

    public SingleWatcher getWatcherForType(Player bindingPlayer, EntityType entityType)
    {
        var watcherFunc = typeWatcherMap.getOrDefault(entityType, null);

        if (watcherFunc == null)
            return new LivingEntityWatcher(bindingPlayer, entityType);

        return watcherFunc.apply(bindingPlayer);
    }
}

package xyz.nifeather.morph.backends.server.renderer.network.registries;

import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.*;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.*;

public class ValueIndex
{
    public static final EntityValues BASE_ENTITY = new EntityValues();
    public static final LivingEntityValues BASE_LIVING = new LivingEntityValues();
    public static final AgeableMobValues AGEABLE_MOB = new AgeableMobValues();
    public static final TameableAnimalValues TAMEABLE = new TameableAnimalValues();

    public static final ArmorStandValues ARMOR_STAND = new ArmorStandValues();
    public static final AllayValues ALLAY = new AllayValues();
    public static final PlayerValues PLAYER = new PlayerValues();
    public static final SlimeValues SLIME_MAGMA = new SlimeValues();
    public static final GhastValues GHAST = new GhastValues();
    public static final AbstractHorseValues ABSTRACT_HORSE = new AbstractHorseValues();
    public static final HorseValues HORSE = new HorseValues();
    public static final ChestedHorseValues CHESTED_HORSE = new ChestedHorseValues();
    public static final LlamaValues LLAMA = new LlamaValues();
    public static final FoxValues FOX = new FoxValues();
    public static final ParrotValues PARROT = new ParrotValues();
    public static final CatValues CAT = new CatValues();
    public static final GoatValues GOAT = new GoatValues();
    public static final TropicalFishValues TROPICAL = new TropicalFishValues();
    public static final RabbitValues RABBIT = new RabbitValues();
    public static final FrogValues FROG = new FrogValues();
    public static final PandaValues PANDA = new PandaValues();
    public static final VillagerValues VILLAGER = new VillagerValues();
    public static final SnowGolemValues SNOW_GOLEM = new SnowGolemValues();
    public static final CreeperValues CREEPER = new CreeperValues();
    public static final PiglinValues PIGLIN = new PiglinValues();
    public static final ZombieValues ZOMBIE = new ZombieValues();
    public static final ZombieVillagerValues ZOMBIE_VILLAGER = new ZombieVillagerValues();
    public static final ZoglinValues ZOGLIN = new ZoglinValues();
    public static final GuardianValues GUARDIAN = new GuardianValues();
    public static final AxolotlValues AXOLOTL = new AxolotlValues();
    public static final MooshroomValues MOOSHROOM = new MooshroomValues();
    public static final SheepValues SHEEP = new SheepValues();
    public static final WolfValues WOLF = new WolfValues();
    public static final PhantomValues PHANTOM = new PhantomValues();
    public static final SnifferValues SNIFFER = new SnifferValues();
    public static final ArmadilloValues ARMADILLO = new ArmadilloValues();
    public static final ShulkerValues SHULKER = new ShulkerValues();
    public static final PufferfishValues PUFFERFISH = new PufferfishValues();

    public static final CreakingValues CREAKING = new CreakingValues();

    /*
    private static final Map<EntityType, AbstractValues> valuesMap = new ConcurrentHashMap<>();
    private static void register(EntityType type, AbstractValues values)
    {
        valuesMap.put(type, values);
    }

    @NotNull
    public static AbstractValues get(EntityType type)
    {
        return valuesMap.getOrDefault(type, BASE_ENTITY);
    }

    static
    {
    }
    */
}

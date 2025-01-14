package xyz.nifeather.morph.providers.animation.provider;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.providers.animation.AnimationSet;
import xyz.nifeather.morph.providers.animation.bundled.*;

public class VanillaAnimationProvider extends DefaultAnimationProvider
{
    public VanillaAnimationProvider()
    {
        this.registerAnimSet(EntityType.WARDEN, new WardenAnimationSet());
        this.registerAnimSet(EntityType.SNIFFER, new SnifferAnimationSet());
        this.registerAnimSet(EntityType.ALLAY, new AllayAnimationSet());
        this.registerAnimSet(EntityType.ARMADILLO, new ArmadilloAnimationSet());
        this.registerAnimSet(EntityType.SHULKER, new ShulkerAnimationSet());
        this.registerAnimSet(EntityType.CAT, new CatAnimationSet());

        // Disabled because parrot dancing is not controlled directly by the metadata or event
        // this.registerAnimSet(EntityType.PARROT.getKey().asString(), new ParrotAnimationSet());

        this.registerAnimSet(EntityType.PIGLIN, new PiglinAnimationSet());
        this.registerAnimSet(EntityType.PUFFERFISH, new PufferfishAnimationSet());
        this.registerAnimSet(EntityType.FOX, new FoxAnimationSet());
        this.registerAnimSet(EntityType.FROG, new FrogAnimationSet());
        this.registerAnimSet(EntityType.WOLF, new WolfAnimationSet());
        this.registerAnimSet(EntityType.PANDA, new PandaAnimationSet());
        this.registerAnimSet(EntityType.CREAKING, new CreakingAnimationSet());
    }

    @Override
    public @NotNull AnimationSet getAnimationSetFor(String disguiseID)
    {
        return animSets.getOrDefault(disguiseID, fallbackAnimationSet);
    }
}

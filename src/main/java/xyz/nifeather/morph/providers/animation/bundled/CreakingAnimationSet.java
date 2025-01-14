package xyz.nifeather.morph.providers.animation.bundled;

import xyz.nifeather.morph.misc.AnimationNames;
import xyz.nifeather.morph.providers.animation.AnimationSet;
import xyz.nifeather.morph.providers.animation.SingleAnimation;

import java.util.List;

public class CreakingAnimationSet extends AnimationSet
{
    public final SingleAnimation EYE_GLOW = new SingleAnimation(AnimationNames.MAKE_ACTIVE, 0, true);
    public final SingleAnimation DISABLE_EYE_GLOW = new SingleAnimation(AnimationNames.MAKE_INACTIVE, 0, true);

    public CreakingAnimationSet()
    {
        registerPersistent(AnimationNames.DISPLAY_EYE_GLOW, List.of(EYE_GLOW));
        registerPersistent(AnimationNames.DISPLAY_STOP_EYE_GLOW, List.of(DISABLE_EYE_GLOW));
    }
}

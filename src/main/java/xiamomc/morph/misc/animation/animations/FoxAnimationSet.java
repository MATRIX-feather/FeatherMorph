package xiamomc.morph.misc.animation.animations;

import xiamomc.morph.misc.animation.AnimationNames;
import xiamomc.morph.misc.animation.SingleAnimation;

import java.util.List;

public class FoxAnimationSet extends AnimationSet
{
    public final SingleAnimation SLEEP_START = new SingleAnimation(AnimationNames.SLEEP, 5, true);
    public final SingleAnimation SIT_START = new SingleAnimation(AnimationNames.SIT, 5, true);
    public final SingleAnimation STANDUP = new SingleAnimation(AnimationNames.STANDUP, 5, true);

    public FoxAnimationSet()
    {
        register(AnimationNames.SLEEP, List.of(SLEEP_START));
        register(AnimationNames.SIT, List.of(SIT_START));
        register(AnimationNames.STANDUP, List.of(STANDUP));
    }
}

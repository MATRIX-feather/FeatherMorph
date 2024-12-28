package xyz.nifeather.morph.transforms;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.transforms.easings.Easing;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;

public class Transformer extends MorphPluginObject
{
    private static long currentTime;

    @Initializer
    private void load()
    {
        this.addSchedule(this::onTickEnd);
    }

    public void onTickEnd()
    {
        this.addSchedule(this::onTickEnd);

        currentTime = System.currentTimeMillis();

        var transformList = new ObjectArrayList<>(transforms);
        for (Transform<?> t : transformList)
        {
            if (t.aborted)
            {
                transforms.remove(t);
                continue;
            }

            double timeProgress = (currentTime - t.startTime) * 1d / t.duration;

            t.applyProgress(timeProgress);

            //如果进度不等于1，那么继续
            if (timeProgress < 1) continue;

            transforms.remove(t);

            if (t.onFinish == null) continue;

            for (Runnable onFinish : t.onFinish)
            {
                try
                {
                    onFinish.run();
                }
                catch (Throwable throwable)
                {
                    FeatherMorphMain.getInstance().getSLF4JLogger().warn(throwable.getMessage());
                    throwable.printStackTrace();
                }
            }
        }
    }

    private static final List<Transform<?>> transforms = new ObjectArrayList<>();

    public static synchronized void startTransform(Transform<?> info)
    {
        transforms.add(info);
    }

    public static <T> GenericTransform<T> transform(Recorder<T> recorder, T endValue, long duration, Easing easing)
    {
        var prevTransform = (GenericTransform<T>) transforms.stream()
                .filter(t -> (t instanceof GenericTransform<?> tB && tB.val == recorder))
                .findFirst().orElse(null);

        if (prevTransform != null)
        {
            prevTransform.update(recorder, currentTime, duration, endValue, easing);
            return prevTransform;
        }
        else
        {
            var transform = new GenericTransform<>(recorder, currentTime, duration, endValue, easing);
            startTransform(transform);
            return transform;
        }
    }

    public static DelayTransform delay(int duration)
    {
        var transform = new DelayTransform(currentTime, duration);
        startTransform(transform);

        return transform;
    }

    public static <T> BindableTransform<T> transform(Bindable<T> bindable, T endValue, long duration, Easing easing)
    {
        var prevTransform = (BindableTransform<T>) transforms.stream()
                .filter(t -> (t instanceof BindableTransform<?> tB && tB.bindable == bindable))
                .findFirst().orElse(null);

        if (prevTransform != null)
        {
            prevTransform.update(bindable, currentTime, duration, endValue, easing);

            return prevTransform;
        }
        else
        {
            var info = new BindableTransform<>(bindable, currentTime, duration, endValue, easing);
            startTransform(info);
            return info;
        }
    }
}

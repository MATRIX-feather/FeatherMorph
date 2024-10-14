package xyz.nifeather.morph.backends.server.renderer.utilties;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import xyz.nifeather.morph.MorphPlugin;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.SingleValue;

import java.util.Optional;

public class ProtocolRegistryUtils
{
    public static WrappedDataWatcher.Serializer getSerializer(SingleValue<?> sv)
    {
        var s = sv.getSerializer();
        if (s != null)
            return s;

        var s1 = getSerializer(sv.defaultValue());
        if (s1 != null)
            sv.setSerializer(s1);

        return s1;
    }

    public static WrappedDataWatcher.Serializer getSerializer(Object instance)
    {
        var clazz = instance.getClass();

        if (instance instanceof Optional<?> optional)
        {
            if (optional.isEmpty())
                throw new IllegalArgumentException("An empty Optional is given");

            clazz = optional.get().getClass();
            instance = optional.get();
        }

        if (clazz == BlockPos.class)
            return WrappedDataWatcher.Registry.getBlockPositionSerializer(true);

        if (instance instanceof Component)
            return WrappedDataWatcher.Registry.getChatComponentSerializer(true);

        WrappedDataWatcher.Serializer ret = null;

        try
        {
            ret = WrappedDataWatcher.Registry.get(clazz);
        }
        catch (Throwable t)
        {
            var logger = MorphPlugin.getInstance().getSLF4JLogger();
            logger.error("Can't find serializer for value '%s': '%s'".formatted(instance, t.getMessage()));
            //t.printStackTrace();
        }

        return ret;
    }
}

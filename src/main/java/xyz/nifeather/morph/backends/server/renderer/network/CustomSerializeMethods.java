package xyz.nifeather.morph.backends.server.renderer.network;

import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Wolf;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;

public class CustomSerializeMethods
{
    private static final WrappedDataWatcher.Serializer NMS_WOLF_VARIANT_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.WOLF_VARIANT);
    public static ICustomSerializeMethod<Wolf.Variant> WOLF_VARIANT = (sv, bukkitVariant) ->
    {
        var rl = ResourceLocation.parse(bukkitVariant.getKey().asString());
        var holder = HolderUtils.getHolderOrThrow(rl, Registries.WOLF_VARIANT);

        return new WrappedDataValue(ValueIndex.WOLF.WOLF_VARIANT.index(), NMS_WOLF_VARIANT_SERIALIZER, holder);
    };

    private static final WrappedDataWatcher.Serializer NMS_CAT_VARIANT_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.CAT_VARIANT);
    public static ICustomSerializeMethod<Cat.Type> CAT_VARIANT = (sv, bukkitVariant) ->
    {
        var rl = ResourceLocation.parse(bukkitVariant.getKey().asString());
        var holder = HolderUtils.getHolderOrThrow(rl, Registries.CAT_VARIANT);

        return new WrappedDataValue(ValueIndex.CAT.CAT_VARIANT.index(), NMS_CAT_VARIANT_SERIALIZER, holder);
    };
}

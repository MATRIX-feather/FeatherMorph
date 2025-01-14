package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.CatVariant;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.TameableAnimalValues;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;

public class CatValues extends TameableAnimalValues
{
    public final SingleValue<Holder<CatVariant>> CAT_VARIANT = createSingle("cat_variant", getCatVariant(CatVariant.TABBY));
    public final SingleValue<Boolean> IS_LYING = createSingle("cat_is_lying", false);
    public final SingleValue<Boolean> RELAXED = createSingle("cat_relaxed", false);
    public final SingleValue<Integer> COLLAR_COLOR = createSingle("cat_collar_color", 14);

    private Holder<CatVariant> getCatVariant(ResourceKey<CatVariant> key)
    {
        return HolderUtils.getHolderOrThrow(key, Registries.CAT_VARIANT);
    }

    public CatValues()
    {
        super();

        var handle = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.CAT_VARIANT);
        CAT_VARIANT.setSerializer(handle);

        registerSingle(CAT_VARIANT, IS_LYING, RELAXED, COLLAR_COLOR);
    }
}

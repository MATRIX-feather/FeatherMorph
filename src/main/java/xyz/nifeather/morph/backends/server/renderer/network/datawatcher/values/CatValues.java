package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.CatVariant;
import org.bukkit.entity.Cat;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.TameableAnimalValues;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;

public class CatValues extends TameableAnimalValues
{
    public final SingleValue<Cat.Type> CAT_VARIANT = createSingle("cat_variant", Cat.Type.TABBY);
    public final SingleValue<Boolean> IS_LYING = createSingle("cat_is_lying", false);
    public final SingleValue<Boolean> RELAXED = createSingle("cat_relaxed", false);
    public final SingleValue<Integer> COLLAR_COLOR = createSingle("cat_collar_color", 14);

    public CatValues()
    {
        super();

        registerSingle(CAT_VARIANT, IS_LYING, RELAXED, COLLAR_COLOR);
    }
}

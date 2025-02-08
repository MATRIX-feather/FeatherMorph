package xyz.nifeather.morph.backends.server.renderer.network;

import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.palmergames.adventure.platform.bukkit.BukkitComponentSerializer;
import com.palmergames.adventure.platform.bukkit.MinecraftComponentSerializer;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Wolf;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;
import xyz.nifeather.morph.misc.ArmadilloState;

import java.util.List;
import java.util.Optional;

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

    private static final WrappedDataWatcher.Serializer NMS_FROG_VARIANT_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.FROG_VARIANT);
    public static ICustomSerializeMethod<Frog.Variant> FROG_VARIANT = (sv, bukkitVariant) ->
    {
        var rl = ResourceLocation.parse(bukkitVariant.getKey().asString());
        var holder = HolderUtils.getHolderOrThrow(rl, Registries.FROG_VARIANT);

        return new WrappedDataValue(ValueIndex.FROG.FROG_VARIANT.index(), NMS_FROG_VARIANT_SERIALIZER, holder);
    };

    public static ICustomSerializeMethod<Optional<Component>> COMPONENT_ADVENTURE_TO_NMS = (sv, val) ->
    {
        var serializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
        if (val.isEmpty())
            return new WrappedDataValue(ValueIndex.BASE_ENTITY.CUSTOM_NAME.index(), serializer, Optional.empty());

        var asVanilla = PaperAdventure.asVanilla(val.get());

        return new WrappedDataValue(ValueIndex.BASE_ENTITY.CUSTOM_NAME.index(), serializer, Optional.of(asVanilla));
    };

    // Bukkit's Armadillo don't have the ArmadilloState! :(
    public static ICustomSerializeMethod<ArmadilloState> ARMADILLO_STATE = (sv, val) ->
    {
        return new WrappedDataValue(ValueIndex.ARMADILLO.STATE.index(), WrappedDataWatcher.Registry.get(Armadillo.ArmadilloState.class), val.nmsState());
    };

    // WTF?
    public static ICustomSerializeMethod<List<ParticleOptions>> PARTICLE_OPTIONS = (sv, val) ->
    {
        return new WrappedDataValue(ValueIndex.BASE_LIVING.POTION_COLOR.index(), WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.PARTICLES), val);
    };
}

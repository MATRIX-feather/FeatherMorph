package xyz.nifeather.morph.backends.server.renderer.network;

import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.math.Rotations;
import net.kyori.adventure.text.Component;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.*;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;

import java.util.List;
import java.util.Optional;

public class CustomSerializeMethods
{
    private static final WrappedDataWatcher.Serializer NMS_WOLF_VARIANT_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.WOLF_VARIANT);
    public static ICustomSerializeMethod<Wolf.Variant> WOLF_VARIANT = (sv, bukkitVariant) ->
    {
        var rl = ResourceLocation.parse(bukkitVariant.getKey().asString());
        var holder = HolderUtils.getHolderOrThrow(rl, Registries.WOLF_VARIANT);

        return new WrappedDataValue(sv.index(), NMS_WOLF_VARIANT_SERIALIZER, holder);
    };

    private static final WrappedDataWatcher.Serializer NMS_CAT_VARIANT_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.CAT_VARIANT);
    public static ICustomSerializeMethod<Cat.Type> CAT_VARIANT = (sv, bukkitVariant) ->
    {
        var rl = ResourceLocation.parse(bukkitVariant.getKey().asString());
        var holder = HolderUtils.getHolderOrThrow(rl, Registries.CAT_VARIANT);

        return new WrappedDataValue(sv.index(), NMS_CAT_VARIANT_SERIALIZER, holder);
    };

    private static final WrappedDataWatcher.Serializer NMS_FROG_VARIANT_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.FROG_VARIANT);
    public static ICustomSerializeMethod<Frog.Variant> FROG_VARIANT = (sv, bukkitVariant) ->
    {
        var rl = ResourceLocation.parse(bukkitVariant.getKey().asString());
        var holder = HolderUtils.getHolderOrThrow(rl, Registries.FROG_VARIANT);

        return new WrappedDataValue(sv.index(), NMS_FROG_VARIANT_SERIALIZER, holder);
    };

    public static ICustomSerializeMethod<Optional<Component>> COMPONENT_ADVENTURE_TO_NMS = (sv, val) ->
    {
        var serializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
        if (val.isEmpty())
            return new WrappedDataValue(sv.index(), serializer, Optional.empty());

        var asVanilla = PaperAdventure.asVanilla(val.get());

        return new WrappedDataValue(sv.index(), serializer, Optional.of(asVanilla));
    };

    // Bukkit's Armadillo don't have the ArmadilloState! :(
    public static ICustomSerializeMethod<DataWrappers.ArmadilloState> ARMADILLO_STATE = (sv, val) ->
    {
        return new WrappedDataValue(sv.index(), WrappedDataWatcher.Registry.get(Armadillo.ArmadilloState.class), val.nmsState());
    };

    // WTF?
    public static ICustomSerializeMethod<List<ParticleOptions>> PARTICLE_OPTIONS = (sv, val) ->
    {
        return new WrappedDataValue(sv.index(), WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.PARTICLES), val);
    };

    private static final WrappedDataWatcher.Serializer ROTATIONS_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.ROTATIONS);
    public static ICustomSerializeMethod<Rotations> ROTATIONS = (sv, val) ->
    {
        var nmsRotation = new net.minecraft.core.Rotations((float)val.x(), (float)val.y(), (float)val.z());

        return new WrappedDataValue(sv.index(), ROTATIONS_SERIALIZER, nmsRotation);
    };

    private static final WrappedDataWatcher.Serializer VILLAGER_DATA_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.VILLAGER_DATA);
    public static ICustomSerializeMethod<DataWrappers.VillagerData> VILLAGER_DATA = (sv, val) ->
    {
        var bukkitVillagerType = val.type();
        var nmsVillagerTypeOptional = BuiltInRegistries.VILLAGER_TYPE
                .getOptional(ResourceLocation.parse(bukkitVillagerType.getKey().asString()));

        var bukkitVillagerProfession = val.profession();
        var nmsVillagerProfessionOptional = BuiltInRegistries.VILLAGER_PROFESSION
                .getOptional(ResourceLocation.parse(bukkitVillagerProfession.getKey().asString()));

        var nmsType = nmsVillagerTypeOptional.orElse(VillagerType.PLAINS);
        var nmsProfession = nmsVillagerProfessionOptional.orElse(VillagerProfession.NONE);
        var nmsData = new VillagerData(nmsType, nmsProfession, val.level());

        return new WrappedDataValue(sv.index(), VILLAGER_DATA_SERIALIZER, nmsData);
    };

    private static final WrappedDataWatcher.Serializer SHULKER_DIRECTION_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.DIRECTION);
    public static ICustomSerializeMethod<DataWrappers.ShulkerDirection> SHULKER_DIRECTION = (sv, val) ->
    {
        return new WrappedDataValue(sv.index(), SHULKER_DIRECTION_SERIALIZER, val.nmsDirection());
    };

    private static final WrappedDataWatcher.Serializer POSE_SERIALIZER = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.POSE);
    public static ICustomSerializeMethod<Pose> POSE = (sv, val) ->
    {
        var nmsPose = net.minecraft.world.entity.Pose.values()[val.ordinal()];

        return new WrappedDataValue(sv.index(), POSE_SERIALIZER, nmsPose);
    };
}

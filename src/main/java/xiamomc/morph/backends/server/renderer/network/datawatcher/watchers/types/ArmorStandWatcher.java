package xiamomc.morph.backends.server.renderer.network.datawatcher.watchers.types;

import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xiamomc.morph.backends.server.renderer.network.registries.ValueIndex;
import xiamomc.morph.misc.disguiseProperty.DisguiseProperties;
import xiamomc.morph.misc.disguiseProperty.SingleProperty;
import xiamomc.morph.misc.disguiseProperty.values.ArmorStandProperties;
import xiamomc.morph.misc.disguiseProperty.values.AxolotlProperties;

public class ArmorStandWatcher extends InventoryLivingWatcher
{
    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.ARMOR_STAND);
    }

    public ArmorStandWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.ARMOR_STAND);
    }

    public byte getArmorStandFlags(boolean small, boolean showArms, boolean noBasePlate)
    {
        var value = (byte)0x00;

        if (small)
            value |= (byte)0x01;

        if (showArms)
            value |= (byte)0x04;

        if (noBasePlate)
            value |= (byte)0x08;

        return value;
    }

    private boolean isSmall()
    {
        return (read(ValueIndex.ARMOR_STAND.DATA_FLAGS) & 0x01) == 0x01;
    }

    private boolean noBasePlate()
    {
        return (read(ValueIndex.ARMOR_STAND.DATA_FLAGS) & 0x08) == 0x08;
    }

    private boolean showArms()
    {
        return (read(ValueIndex.ARMOR_STAND.DATA_FLAGS) & 0x04) == 0x04;
    }

    private Rotations getVec3(ListTag listTag, Rotations defaultValue)
    {
        if (listTag.isEmpty())
        {
            logger.warn("Empty listTag! Using defaultValue...");
            listTag = defaultValue.save();
        }

        return new Rotations(listTag);
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(ArmorStandProperties.class);

        if (property.equals(properties.SHOW_ARMS))
        {
            var val = (Boolean) value;
            this.writePersistent(ValueIndex.ARMOR_STAND.DATA_FLAGS, getArmorStandFlags(this.isSmall(), val, this.noBasePlate()));
        }

        super.onPropertyWrite(property, value);
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);
        boolean small = isSmall();
        boolean noBasePlate = noBasePlate();
        boolean showArms = showArms();

        if (nbt.contains("Small"))
            small = nbt.getBoolean("Small");

        if (nbt.contains("NoBasePlate"))
            noBasePlate = nbt.getBoolean("NoBasePlate");

        if (nbt.contains("ShowArms"))
            showArms = nbt.getBoolean("ShowArms");

        //Tag "Invisible" is not supported as it's synced with the player

        writePersistent(ValueIndex.ARMOR_STAND.DATA_FLAGS, getArmorStandFlags(small, showArms, noBasePlate));

        if (nbt.contains("Pose"))
        {
            var poseCompound = nbt.getCompound("Pose");

            if (poseCompound.contains("Body"))
            {
                writePersistent(ValueIndex.ARMOR_STAND.BODY_ROTATION,
                        getVec3(poseCompound.getList("Body", CompoundTag.TAG_FLOAT),
                                ValueIndex.ARMOR_STAND.BODY_ROTATION.defaultValue()));
            }

            if (poseCompound.contains("Head"))
            {
                writePersistent(ValueIndex.ARMOR_STAND.HEAD_ROTATION,
                        getVec3(poseCompound.getList("Head", CompoundTag.TAG_FLOAT),
                                ValueIndex.ARMOR_STAND.HEAD_ROTATION.defaultValue()));
            }

            if (poseCompound.contains("LeftArm"))
            {
                writePersistent(ValueIndex.ARMOR_STAND.LEFT_ARM_ROTATION,
                        getVec3(poseCompound.getList("LeftArm", CompoundTag.TAG_FLOAT),
                                ValueIndex.ARMOR_STAND.LEFT_ARM_ROTATION.defaultValue()));
            }

            if (poseCompound.contains("RightArm"))
            {
                writePersistent(ValueIndex.ARMOR_STAND.RIGHT_ARM_ROTATION,
                        getVec3(poseCompound.getList("RightArm", CompoundTag.TAG_FLOAT),
                                ValueIndex.ARMOR_STAND.RIGHT_ARM_ROTATION.defaultValue()));
            }

            if (poseCompound.contains("LeftLeg"))
            {
                writePersistent(ValueIndex.ARMOR_STAND.LEFT_LEG_ROTATION,
                        getVec3(poseCompound.getList("LeftLeg", CompoundTag.TAG_FLOAT),
                                ValueIndex.ARMOR_STAND.LEFT_LEG_ROTATION.defaultValue()));
            }

            if (poseCompound.contains("RightLeg"))
            {
                writePersistent(ValueIndex.ARMOR_STAND.RIGHT_LEG_ROTATION,
                        getVec3(poseCompound.getList("RightLeg", CompoundTag.TAG_FLOAT),
                                ValueIndex.ARMOR_STAND.RIGHT_LEG_ROTATION.defaultValue()));
            }
        }
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        nbt.putBoolean("Small", this.isSmall());
        nbt.putBoolean("NoBasePlate", this.noBasePlate());
        nbt.putBoolean("ShowArms", this.showArms());

        var poseCompound = new CompoundTag();
        poseCompound.put("Head", read(ValueIndex.ARMOR_STAND.HEAD_ROTATION).save());
        poseCompound.put("Body", read(ValueIndex.ARMOR_STAND.BODY_ROTATION).save());
        poseCompound.put("LeftArm", read(ValueIndex.ARMOR_STAND.LEFT_ARM_ROTATION).save());
        poseCompound.put("RightArm", read(ValueIndex.ARMOR_STAND.RIGHT_ARM_ROTATION).save());
        poseCompound.put("LeftLeg", read(ValueIndex.ARMOR_STAND.LEFT_LEG_ROTATION).save());
        poseCompound.put("RightLeg", read(ValueIndex.ARMOR_STAND.RIGHT_LEG_ROTATION).save());

        nbt.put("Pose", poseCompound);
    }
}

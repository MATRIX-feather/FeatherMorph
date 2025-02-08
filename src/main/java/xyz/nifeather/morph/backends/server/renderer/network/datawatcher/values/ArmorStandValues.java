package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import io.papermc.paper.math.Rotations;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.LivingEntityValues;

public class ArmorStandValues extends LivingEntityValues
{
    public final SingleValue<Byte> DATA_FLAGS = createSingle("armor_stand_flags", (byte)0);
    public final SingleValue<Rotations> HEAD_ROTATION = createSingle("armor_stand_headRot", Rotations.ofDegrees(0, 0, 0));
    public final SingleValue<Rotations> BODY_ROTATION = createSingle("armor_stand_bodyRot", Rotations.ofDegrees(0, 0, 0));
    public final SingleValue<Rotations> LEFT_ARM_ROTATION = createSingle("armor_stand_leftArmRot", Rotations.ofDegrees(-10, 0, -10));
    public final SingleValue<Rotations> RIGHT_ARM_ROTATION = createSingle("armor_stand_rightArmRot", Rotations.ofDegrees(-15, 0, 10));
    public final SingleValue<Rotations> LEFT_LEG_ROTATION = createSingle("armor_stand_leftLegRot", Rotations.ofDegrees(-1, 0, -1));
    public final SingleValue<Rotations> RIGHT_LEG_ROTATION = createSingle("armor_stand_rightLeg", Rotations.ofDegrees(1, 0, 1));

    public ArmorStandValues()
    {
        super();

        setSeralizers(HEAD_ROTATION, BODY_ROTATION,
                LEFT_ARM_ROTATION, RIGHT_ARM_ROTATION,
                LEFT_LEG_ROTATION, RIGHT_LEG_ROTATION);

        registerSingle(DATA_FLAGS,
                HEAD_ROTATION, BODY_ROTATION,
                LEFT_ARM_ROTATION, RIGHT_ARM_ROTATION,
                LEFT_LEG_ROTATION, RIGHT_LEG_ROTATION);
    }

    @SafeVarargs
    private void setSeralizers(SingleValue<Rotations>... svs)
    {
        for (SingleValue<Rotations> sv : svs)
            sv.setSerializeMethod(CustomSerializeMethods.ROTATIONS);
    }
}

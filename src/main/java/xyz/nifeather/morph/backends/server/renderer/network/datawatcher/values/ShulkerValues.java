package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers;

public class ShulkerValues extends MonsterValues
{
    public final SingleValue<DataWrappers.ShulkerDirection> ATTACH_FACE_ID = createSingle("shulker_attach_face_id", DataWrappers.ShulkerDirection.DOWN);
    public final SingleValue<Byte> PEEK_ID = createSingle("shulker_peek_id", (byte)0);
    public final SingleValue<Byte> COLOR_ID = createSingle("shulker_color_id", (byte)16);

    public ShulkerValues()
    {
        ATTACH_FACE_ID.setSerializeMethod(CustomSerializeMethods.SHULKER_DIRECTION);

        registerSingle(ATTACH_FACE_ID, PEEK_ID, COLOR_ID);
    }
}

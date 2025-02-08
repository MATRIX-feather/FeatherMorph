package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Pose;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.AbstractValues;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.SingleValue;

import java.util.Optional;

public class EntityValues extends AbstractValues
{
    public final SingleValue<Byte> GENERAL = createSingle("entity_general", (byte)0);
    public final SingleValue<Integer> AIR_TICKS = createSingle("entity_air_ticks", 0);
    public final SingleValue<Optional<Component>> CUSTOM_NAME = createSingle("entity_custom_name", Optional.empty());
    public final SingleValue<Boolean> CUSTOM_NAME_VISIBLE = createSingle("entity_custom_name_visible", false);
    public final SingleValue<Boolean> SILENT = createSingle("entity_silent", false);
    public final SingleValue<Boolean> NO_GRAVITY = createSingle("entity_no_gravity", false);
    public final SingleValue<Pose> POSE = createSingle("entity_pose", Pose.STANDING);
    public final SingleValue<Integer> FROZEN_TICKS = createSingle("entity_frozen_ticks", 0);

    public EntityValues()
    {
        CUSTOM_NAME.setSerializeMethod(CustomSerializeMethods.COMPONENT_ADVENTURE_TO_NMS);
        POSE.setSerializeMethod(CustomSerializeMethods.POSE);

        registerSingle(GENERAL, AIR_TICKS, CUSTOM_NAME, CUSTOM_NAME_VISIBLE, SILENT, NO_GRAVITY,
                POSE, FROZEN_TICKS);
    }
}

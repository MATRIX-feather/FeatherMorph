package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.AnimalValues;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers.ArmadilloState;

public class ArmadilloValues extends AnimalValues
{
    public final SingleValue<DataWrappers.ArmadilloState> STATE = createSingle("armadillo_state", ArmadilloState.IDLE);

    public ArmadilloValues()
    {
        STATE.setSerializeMethod(CustomSerializeMethods.ARMADILLO_STATE);

        registerSingle(STATE);
    }
}

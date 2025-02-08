package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.AnimalValues;
import xyz.nifeather.morph.misc.ArmadilloState;

public class ArmadilloValues extends AnimalValues
{
    public final SingleValue<ArmadilloState> STATE = createSingle("armadillo_state", ArmadilloState.IDLE);

    public ArmadilloValues()
    {
        STATE.setSerializer(CustomSerializeMethods.ARMADILLO_STATE);

        registerSingle(STATE);
    }
}

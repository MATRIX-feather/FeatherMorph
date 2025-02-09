package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import org.bukkit.entity.Sniffer;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.AnimalValues;

public class SnifferValues extends AnimalValues
{
    public final SingleValue<Sniffer.State> SNIFFER_STATE = createSingle("sniffer_state", Sniffer.State.IDLING);
    public final SingleValue<Integer> DROP_SEED_AT_TICK = createSingle("drop_at_tick", 0);

    public SnifferValues()
    {
        SNIFFER_STATE.setSerializeMethod(CustomSerializeMethods.SNIFFER_STATE);

        registerSingle(SNIFFER_STATE, DROP_SEED_AT_TICK);
    }
}

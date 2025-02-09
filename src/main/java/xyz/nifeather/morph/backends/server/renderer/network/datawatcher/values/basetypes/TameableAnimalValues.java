package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes;

import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.SingleValue;

import java.util.Optional;
import java.util.UUID;

public class TameableAnimalValues extends AnimalValues
{
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public final SingleValue<Byte> TAMEABLE_FLAGS = createSingle("tameable_flags", (byte)0x00);
    public final SingleValue<Optional<UUID>> OWNER = createSingle("tameable_owner", Optional.of(NIL_UUID));

    public TameableAnimalValues()
    {
        super();

        registerSingle(TAMEABLE_FLAGS, OWNER);
    }
}

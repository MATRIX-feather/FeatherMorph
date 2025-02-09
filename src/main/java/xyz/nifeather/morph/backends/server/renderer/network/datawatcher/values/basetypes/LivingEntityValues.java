package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes;

import net.minecraft.core.particles.ParticleOptions;
import org.joml.Vector3i;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.SingleValue;

import java.util.List;
import java.util.Optional;

public class LivingEntityValues extends EntityValues
{
    public final SingleValue<Byte> LIVING_FLAGS = createSingle("living_flags", (byte)0);
    public final SingleValue<Float> HEALTH = createSingle("living_health", 1f);
    public final SingleValue<List<ParticleOptions>> POTION_COLOR = createSingle("living_option_color", List.of());
    public final SingleValue<Boolean> POTION_ISAMBIENT = createSingle("living_potion_is_ambient", false);
    public final SingleValue<Integer> STUCKED_ARROWS = createSingle("living_stucked_arrows", 0);
    public final SingleValue<Integer> BEE_STINGERS = createSingle("living_bee_stingers", 0);
    public final SingleValue<Optional<Vector3i>> BED_POS = createSingle("living_bed_pos", Optional.of(new Vector3i(0,0,0)));

    public LivingEntityValues()
    {
        super();

        POTION_COLOR.setSerializeMethod(CustomSerializeMethods.PARTICLE_OPTIONS);

        BED_POS.setSerializeMethod(CustomSerializeMethods.BLOCKPOS);

        registerSingle(LIVING_FLAGS, HEALTH, POTION_COLOR, POTION_ISAMBIENT, STUCKED_ARROWS, BED_POS, BEE_STINGERS);
    }
}

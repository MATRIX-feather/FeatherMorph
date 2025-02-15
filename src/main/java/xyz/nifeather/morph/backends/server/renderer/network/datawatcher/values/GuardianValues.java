package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

public class GuardianValues extends MonsterValues
{
    public final SingleValue<Boolean> RETRACING_SPIKES = createSingle("guardian_retracing_spikes", false);
    public final SingleValue<Integer> TARGET_ENTITY_ID = createSingle("guardian_target_entity", -1);

    public GuardianValues()
    {
        super();

        registerSingle(RETRACING_SPIKES, TARGET_ENTITY_ID);
    }
}

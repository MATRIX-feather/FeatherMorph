package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

public class CreeperValues extends MonsterValues
{
    public final SingleValue<Integer> STATE = createSingle("creeper_state", 0);
    public final SingleValue<Boolean> IS_CHARGED_CREEPER = createSingle("creeper_is_charged", false);
    public final SingleValue<Boolean> IGNITED = createSingle("creeper_ignited", false);

    public CreeperValues()
    {
        registerSingle(STATE, IS_CHARGED_CREEPER, IGNITED);
    }
}

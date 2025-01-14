package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

public class CreakingValues extends MonsterValues
{
    public final SingleValue<Boolean> CAN_MOVE = createSingle("creaking_can_move", false);
    public final SingleValue<Boolean> IS_ACTIVE = createSingle("creaking_is_active", false);

    public CreakingValues()
    {
        registerSingle(CAN_MOVE, IS_ACTIVE);
    }
}

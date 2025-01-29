package xyz.nifeather.morph.events.mirror;

public interface IExecutor<TPlayer, TItemStack, TAction>
{
    public void onSneak(TPlayer player, boolean sneaking);
    public void onSwapHand(TPlayer player);

    public void onHotbarChange(TPlayer player, int slot);
    public void onStopUsingItem(TPlayer player, TItemStack itemStack);

    /**
     *
     * @param source
     * @param target
     * @return Whether to cancel the event
     */
    public boolean onHurtEntity(TPlayer source, TPlayer target);

    /**
     *
     * @param player
     * @return Whether to cancel the event
     */
    public boolean onSwing(TPlayer player);

    public void onInteract(TPlayer player, TAction action);

    public void reset();
}

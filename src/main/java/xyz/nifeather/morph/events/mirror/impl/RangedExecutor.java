package xyz.nifeather.morph.events.mirror.impl;

import net.minecraft.world.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.utilities.NmsUtils;

import java.util.List;

public class RangedExecutor extends ChainedExecutor
{
    public RangedExecutor(ExecutorHub executorHub)
    {
        super(executorHub);
    }

    @Override
    protected @Nullable Player findNextControllablePlayerFrom(Player source, List<Player> pendingChain)
    {
        var range = executorHub.getControlDistance();

        var nmsPlayer = NmsRecord.ofPlayer(source).level()
                .getNearestPlayer(source.getX(), source.getY(), source.getZ(),
                range, entity ->
                {
                    var bukkitInstance = entity.getBukkitEntity();

                    if (!(bukkitInstance instanceof Player player))
                        return false;

                    return pendingChain.contains(player);
                });

        return nmsPlayer == null ? null : (Player) nmsPlayer.getBukkitEntity();
    }

    @Override
    protected boolean simulateOperation(Action action, Player targetPlayer, Player source)
    {
        return false;
    }

    @Override
    public void onSneak(Player player, boolean sneaking)
    {

    }

    @Override
    public void onSwapHand(Player player)
    {

    }

    @Override
    public void onHotbarChange(Player player, int slot)
    {

    }

    @Override
    public void onStopUsingItem(Player player, ItemStack itemStack)
    {

    }

    @Override
    public boolean onHurtEntity(Player source, Player target)
    {
        return false;
    }

    @Override
    public boolean onSwing(Player player)
    {
        return false;
    }

    @Override
    public void onInteract(Player player, Action action)
    {

    }

    @Override
    public void reset()
    {

    }
}

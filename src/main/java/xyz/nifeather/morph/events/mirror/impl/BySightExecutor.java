package xyz.nifeather.morph.events.mirror.impl;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.events.InteractionMirrorProcessor;
import xyz.nifeather.morph.events.PlayerTracker;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.storage.mirrorlogging.OperationType;
import xyz.nifeather.morph.utilities.NmsUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BySightExecutor extends ChainedExecutor
{
    public BySightExecutor(ExecutorHub executorHub)
    {
        super(executorHub);
    }

    @Override
    public void reset()
    {
    }

    /**
     * 寻找给定玩家的下一个可控制目标
     */
    @Override
    protected @Nullable Player findNextControllablePlayerFrom(Player source, List<Player> pendingChain)
    {
        var targetName = getTargetControlFor(source);
        if (targetName == null)
            return null;

        InteractionMirrorProcessor.PlayerInfo info;

        var targetEntity = source.getTargetEntity(5);

        if (!(targetEntity instanceof Player targetPlayer))
            return null;

        if (!NmsUtils.isTickThreadFor(targetPlayer))
            return null;

        if  (!playerInDistance(source, targetPlayer))
            return null;

        var state = morphManager().getDisguiseStateFor(targetPlayer);

        if (state != null && state.getDisguiseIdentifier().equals("player:" + targetName))
            return targetPlayer;
        else if (targetPlayer.getName().equals(targetName) && state == null)
            return targetPlayer;
        else
            return null;
    }

    @Override
    public boolean onHurtEntity(Player damager, Player hurted)
    {
        var simulateTarget = findNextControllablePlayerFrom(damager, currentSimulateChain.get());

        if (simulateTarget == null)
            return false;

        // 因为HurtEntity之后一定有次ArmSwing，所以不需要在这里进行模拟操作

        var damagerLookingAt = damager.getTargetEntity(5);
        var playerLookingAt = simulateTarget.getTargetEntity(5);

        //如果伪装的玩家想攻击的实体和被伪装的玩家一样，模拟左键并取消事件
        if (damagerLookingAt != null && damagerLookingAt.equals(playerLookingAt))
            return true;

        return hurted.equals(simulateTarget);
    }

    @Override
    public boolean onSwing(Player source)
    {
        var isInChain = isInChain(source);

        // 如果玩家在链条中，并且不是链条中的最后一个，则取消挥手的事件
        if (isInChain)
        {
            if (isLastInChain(source))
                return false;

            return true;
        }

        var tracker = tracker();

        //若源玩家正在丢出物品，不要处理
        if (tracker.droppingItemThisTick(source))
            return false;

        var lastAction = tracker.getLastInteractAction(source);

        //如果此时玩家没有触发Interaction, 那么默认设置为左键空气
        if (!tracker.interactingThisTick(source))
            lastAction = PlayerTracker.InteractType.LEFT_CLICK_AIR;

        if (lastAction == null) return false;

        //旁观者模式下左键方块不会产生Interact事件，我们得猜这个玩家现在是左键还是右键
        if (source.getGameMode() == GameMode.SPECTATOR)
        {
            if (lastAction.isRightClick())
                lastAction = PlayerTracker.InteractType.LEFT_CLICK_BLOCK;
        }

        PlayerTracker.InteractType finalLastAction = lastAction;
        AtomicBoolean simulateSuccess = new AtomicBoolean(false);

        runIfChainable(source, targetPlayer ->
        {
            simulateSuccess.set(true);

            simulateOperation(finalLastAction.toBukkitAction(), targetPlayer, source);
            logOperation(source, targetPlayer, finalLastAction.isLeftClick() ? OperationType.LeftClick : OperationType.RightClick);
        });

        return simulateSuccess.get();
    }
}

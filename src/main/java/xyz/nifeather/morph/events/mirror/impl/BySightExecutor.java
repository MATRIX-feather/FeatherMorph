package xyz.nifeather.morph.events.mirror.impl;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.S2C.set.S2CSetSneakingCommand;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.events.InteractionMirrorProcessor;
import xyz.nifeather.morph.events.PlayerTracker;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.storage.mirrorlogging.OperationType;
import xyz.nifeather.morph.utilities.ItemUtils;
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
     * 模拟玩家操作
     *
     * @param action 操作类型
     * @param targetPlayer 目标玩家
     * @return 操作是否成功
     */
    @Override
    protected boolean simulateOperation(Action action, Player targetPlayer, Player source)
    {
        // 如果栈内包含目标玩家，或者此玩家这个tick已经和环境互动过了一次，那么忽略此操作
        if (tracker().interactingThisTick(targetPlayer))
            return false;

        var isRightClick = action.isRightClick();
        var result = isRightClick
                ? operationSimulator().simulateRightClick(targetPlayer)
                : operationSimulator().simulateLeftClick(targetPlayer);

        boolean success = false;

        if (result.success())
        {
            var itemInUse = targetPlayer.getEquipment().getItem(result.hand()).getType();

            if (!isRightClick || !ItemUtils.isContinuousUsable(itemInUse) || result.forceSwing())
            {
                var allowed = new PlayerArmSwingEvent(targetPlayer, result.hand()).callEvent();

                if (allowed)
                    targetPlayer.swingHand(result.hand());
            }

            success = true;
        }

        return success;
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
    public void onSneak(Player source, boolean sneaking)
    {
        this.runIfChainable(source, p ->
        {
            p.setSneaking(sneaking);
            clientHandler().sendCommand(p, new S2CSetSneakingCommand(sneaking));

            logOperation(source, p, OperationType.ToggleSneak);
        });
    }

    @Override
    public void onSwapHand(Player player)
    {
        this.runIfChainable(player, targetPlayer ->
        {
            var equipment = targetPlayer.getEquipment();

            var mainHandItem = equipment.getItemInMainHand();
            var offhandItem = equipment.getItemInOffHand();

            equipment.setItemInMainHand(offhandItem);
            equipment.setItemInOffHand(mainHandItem);

            logOperation(player, targetPlayer, OperationType.SwapHand);
        });
    }

    @Override
    public void onHotbarChange(Player player, int slot)
    {
        this.runIfChainable(player, targetPlayer ->
        {
            targetPlayer.getInventory().setHeldItemSlot(slot);
            logOperation(player, targetPlayer, OperationType.HotbarChange);
        });
    }

    @Override
    public void onStopUsingItem(Player player, ItemStack itemStack)
    {
        var ourHandItem = itemStack.getType();

        this.runIfChainable(player, targetPlayer ->
        {
            //如果目标玩家正在使用的物品和我们当前释放的物品一样，并且释放的物品拥有使用动画，那么调用releaseUsingItem
            var nmsPlayer = NmsRecord.ofPlayer(targetPlayer);

            if (nmsPlayer.isUsingItem()
                    && ItemUtils.isContinuousUsable(ourHandItem)
                    && nmsPlayer.getUseItem().getBukkitStack().getType() == ourHandItem)
            {
                nmsPlayer.releaseUsingItem();

                logOperation(player, targetPlayer, OperationType.ReleaseUsingItem);
            }
        });
    }

    @Override
    public boolean onHurtEntity(Player damager, Player hurted)
    {
        var targetPlayer = findNextControllablePlayerFrom(damager, List.of());

        if (targetPlayer == null)
            return false;

        // 因为HurtEntity之后一定有次ArmSwing，所以不需要在这里进行模拟操作

        var damagerLookingAt = damager.getTargetEntity(5);
        var playerLookingAt = targetPlayer.getTargetEntity(5);

        //如果伪装的玩家想攻击的实体和被伪装的玩家一样，模拟左键并取消事件
        if (damagerLookingAt != null && damagerLookingAt.equals(playerLookingAt))
            return true;

        return hurted.equals(targetPlayer);
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

    @Override
    public void onInteract(Player source, Action action)
    {
        if (action == Action.PHYSICAL) return;

        //Sometimes right click fires PlayerInteractEvent for both left and right hand.
        //This prevents us from simulating the same operation twice.
        if (tracker().isDuplicatedRightClick(source))
        {
            if (FeatherMorphMain.getInstance().doInternalDebugOutput)
                logger.info("Interact, Duplicated RC " + System.currentTimeMillis());

            return;
        }

        runIfChainable(source, targetPlayer ->
        {
            simulateOperation(action, targetPlayer, source);
            logOperation(source, targetPlayer, action.isLeftClick() ? OperationType.LeftClick : OperationType.RightClick);
        });
    }
}

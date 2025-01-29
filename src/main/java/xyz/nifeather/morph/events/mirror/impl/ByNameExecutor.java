package xyz.nifeather.morph.events.mirror.impl;

import ca.spottedleaf.moonrise.common.util.TickThread;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.network.commands.S2C.set.S2CSetSneakingCommand;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.events.InteractionMirrorProcessor;
import xyz.nifeather.morph.events.PlayerTracker;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.storage.mirrorlogging.OperationType;
import xyz.nifeather.morph.utilities.ItemUtils;
import xyz.nifeather.morph.utilities.NmsUtils;

public class ByNameExecutor extends AbstractExecutor
{
    public ByNameExecutor(ExecutorHub executorHub)
    {
        super(executorHub);
    }

    @Override
    public void reset()
    {
    }

    /**
     * Search for a player that matches the target name.
     * @param player The {@link Player} who triggered this operation
     * @return A player who matches the target name
     * @apiNote If {@link ConfigOption#MIRROR_SELECTION_MODE} is set to {@link InteractionMirrorProcessor.InteractionMirrorSelectionMode#BY_SIGHT},
     *          the returned value might be a player who disguised as our searching target.
     */
    @NotNull
    private InteractionMirrorProcessor.PlayerInfo getMirrorTarget(Player player)
    {
        var targetName = getTargetControlFor(player);
        if (targetName == null)
            return new InteractionMirrorProcessor.PlayerInfo(null, InteractionMirrorProcessor.PlayerInfo.notSetStr);

        InteractionMirrorProcessor.PlayerInfo info;

        var targetPlayer = Bukkit.getPlayerExact(targetName);

        if (targetPlayer == null || !playerNotDisguised(targetPlayer))
            info = new InteractionMirrorProcessor.PlayerInfo(null, targetName);
        else
            info = new InteractionMirrorProcessor.PlayerInfo(targetPlayer, targetName);

        return info;
    }

    /**
     * 模拟玩家操作
     *
     * @param action 操作类型
     * @param targetPlayer 目标玩家
     * @return 操作是否成功
     */
    protected boolean simulateOperation(Action action, Player targetPlayer, Player source)
    {
        // 如果玩家这个tick已经和环境互动过了一次，那么忽略此操作
        if (tracker().interactingThisTick(targetPlayer)) return false;

        var isRightClick = action.isRightClick();
        var result = isRightClick
                ? operationSimulator().simulateRightClick(targetPlayer)
                : operationSimulator().simulateLeftClick(targetPlayer);

        boolean success = false;

        if (result.success())
        {
            var itemInUse = targetPlayer.getEquipment().getItem(result.hand()).getType();

            if (!isRightClick || !ItemUtils.isContinuousUsable(itemInUse) || result.forceSwing())
                targetPlayer.swingHand(result.hand());

            success = true;
        }

        return success;
    }

    private void scheduleIfNotInSameRegion(Player targetPlayer, Runnable consumer)
    {
        var nmsPlayer = NmsRecord.ofPlayer(targetPlayer);

        if (!TickThread.isTickThreadFor(nmsPlayer))
            scheduleOn(targetPlayer, consumer);
        else
            consumer.run();
    }

    @Override
    public void onSneak(Player player, boolean sneaking)
    {
        var playerInf = getMirrorTarget(player);
        var targetPlayer = playerInf.target();

        if (!playerInDistance(player, playerInf) || targetPlayer.isSneaking() == sneaking) return;

        scheduleIfNotInSameRegion(targetPlayer, () ->
        {
            targetPlayer.setSneaking(sneaking);
            clientHandler().sendCommand(targetPlayer, new S2CSetSneakingCommand(sneaking));

            logOperation(player, targetPlayer, OperationType.ToggleSneak);
        });
    }

    @Override
    public void onSwapHand(Player player)
    {
        var playerInf = getMirrorTarget(player);

        if (!playerInDistance(player, playerInf))
            return;

        var targetPlayer = playerInf.target();

        assert targetPlayer != null;

        scheduleIfNotInSameRegion(targetPlayer, () ->
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
        var inf = getMirrorTarget(player);

        if (!playerInDistance(player, inf))
            return;

        var targetPlayer = inf.target();
        assert targetPlayer != null;

        scheduleIfNotInSameRegion(targetPlayer, () ->
        {
            targetPlayer.getInventory().setHeldItemSlot(slot);
            logOperation(player, targetPlayer, OperationType.HotbarChange);
        });
    }

    @Override
    public void onStopUsingItem(Player player, ItemStack itemStack)
    {
        var inf = getMirrorTarget(player);

        if (!playerInDistance(player, inf))
            return;

        var targetPlayer = inf.target();
        assert targetPlayer != null;

        scheduleIfNotInSameRegion(targetPlayer, () ->
        {
            //如果目标玩家正在使用的物品和我们当前释放的物品一样，并且释放的物品拥有使用动画，那么调用releaseUsingItem
            var ourHandItem = itemStack.getType();
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
        var inf = getMirrorTarget(damager);

        if (!playerInDistance(damager, inf))
            return false;

        var targetPlayer = inf.target();
        assert targetPlayer != null;

        simulateOperationAsync(Action.LEFT_CLICK_AIR, targetPlayer, damager, success -> {});
        logOperation(damager, targetPlayer, OperationType.LeftClick);

        if (TickThread.isTickThreadFor(NmsRecord.ofPlayer(targetPlayer)))
        {
            var damagerLookingAt = damager.getTargetEntity(5);
            var playerLookingAt = targetPlayer.getTargetEntity(5);

            //如果伪装的玩家想攻击的实体和被伪装的玩家一样，模拟左键并取消事件
            if (damagerLookingAt != null && damagerLookingAt.equals(playerLookingAt))
                return true;
        }

        return hurted.equals(targetPlayer);
    }

    @Override
    public boolean onSwing(Player source)
    {
        var inf = getMirrorTarget(source);
        var targetPlayer = inf.target();
        if (targetPlayer == null) return false;

        var playerInDistance = playerInDistance(source, inf);

        //取消一定条件下源玩家的挥手动画
        if (targetPlayer.getLocation().getWorld() == source.getLocation().getWorld()
                && playerInDistance
                && NmsUtils.isTickThreadFor(targetPlayer)
                && NmsUtils.isTickThreadFor(source)
                && Math.abs(targetPlayer.getLocation().distance(source.getLocation())) <= 6)
        {
            var theirTarget = targetPlayer.getTargetEntity(5);
            var ourTarget = source.getTargetEntity(5);

            if ((ourTarget != null || theirTarget != null)
                    && (ourTarget == targetPlayer || ourTarget == theirTarget || theirTarget == source))
            {
                return true;
            }
        }

        if (!playerInDistance)
            return false;

        var tracker = tracker();

        //若源玩家正在丢出物品，不要处理
        //检查玩家在此tick内是否存在互动以避免重复镜像
        if (tracker.droppingItemThisTick(source))
        {
            return false;
        }

        var lastAction = tracker.getLastInteractAction(source);

        //如果此时玩家没有触发Interaction, 那么默认设置为左键空气
        if (!tracker.interactingThisTick(source))
            lastAction = PlayerTracker.InteractType.LEFT_CLICK_AIR;

        if (lastAction == null)
            return false;

        //旁观者模式下左键方块不会产生Interact事件，我们得猜这个玩家现在是左键还是右键
        if (source.getGameMode() == GameMode.SPECTATOR)
        {
            if (lastAction.isRightClick())
                lastAction = PlayerTracker.InteractType.LEFT_CLICK_BLOCK;
        }

        simulateOperationAsync(lastAction.toBukkitAction(), targetPlayer, source, success -> {});
        logOperation(source, targetPlayer, lastAction.isLeftClick() ? OperationType.LeftClick : OperationType.RightClick);

        return false;
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

        var inf = getMirrorTarget(source);

        if (!playerInDistance(source, inf))
            return;

        var targetPlayer = inf.target();
        assert targetPlayer != null;

        simulateOperationAsync(action, targetPlayer, source, success -> {});
        logOperation(source, targetPlayer, action.isLeftClick() ? OperationType.LeftClick : OperationType.RightClick);
    }
}

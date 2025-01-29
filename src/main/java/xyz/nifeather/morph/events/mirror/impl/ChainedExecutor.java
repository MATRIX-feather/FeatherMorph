package xyz.nifeather.morph.events.mirror.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.S2C.set.S2CSetSneakingCommand;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.events.PlayerTracker;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.storage.mirrorlogging.OperationType;
import xyz.nifeather.morph.utilities.ItemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class ChainedExecutor extends AbstractExecutor
{
    public ChainedExecutor(ExecutorHub executorHub)
    {
        super(executorHub);
    }

    protected final ThreadLocal<List<Player>> currentSimulateChain = ThreadLocal.withInitial(ArrayList::new);

    protected boolean isInChain(Player source)
    {
        var list = currentSimulateChain.get();
        return list != null && list.contains(source);
    }

    protected boolean isLastInChain(Player player)
    {
        var list = currentSimulateChain.get();
        return list != null && (list.indexOf(player) + 1 == list.size());
    }

    protected void runIfChainable(Player source, Consumer<Player> chainConsumer)
    {
        var currentChain = currentSimulateChain.get();

        if (currentChain == null)
            return;

        if (currentChain.contains(source))
            return;

        // Build the chain and execute
        var chain = buildSimulateChain(source);
        currentSimulateChain.set(chain);

        var first = chain.getFirst();

        chain.forEach(pl ->
        {
            // 跳过第一个（发起调用链）的玩家
            // 这样可能会导致以下情况的发生：
            //
            // 玩家A点击左键 -> 被加入模拟链条 -> 触发A的模拟 -> 继续被加入新的模拟链条
            if (pl != first)
                chainConsumer.accept(pl);
        });

        // Cleanup
        currentChain.clear();
        currentSimulateChain.remove();
    }

    @Nullable
    protected abstract Player findNextControllablePlayerFrom(Player source, List<Player> pendingChain);

    protected List<Player> buildSimulateChain(Player source)
    {
        List<Player> chain = new ObjectArrayList<>();

        chain.add(source);

        Player current = source;
        while (current != null)
        {
            var next = findNextControllablePlayerFrom(current, chain);

            // 我们找到了调用链中的玩家！退出以防止死循环
            if (chain.contains(next))
                break;

            if (next != null)
            {
                chain.add(next);
                current = next;
            }
            else
            {
                break;
            }
        }

        return chain;
    }

    //region Controller

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

    //endregion Controller
}

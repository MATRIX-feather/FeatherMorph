package xyz.nifeather.morph.events.mirror.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.events.InteractionMirrorProcessor;
import xyz.nifeather.morph.events.PlayerTracker;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.events.mirror.IExecutor;
import xyz.nifeather.morph.misc.PlayerOperationSimulator;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.storage.mirrorlogging.OperationType;
import xyz.nifeather.morph.utilities.DisguiseUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class AbstractExecutor extends MorphPluginObject implements IExecutor<Player, ItemStack, Action>
{
    protected final ExecutorHub executorHub;

    public AbstractExecutor(ExecutorHub executorHub)
    {
        this.executorHub = executorHub;
    }

    protected void logOperation(Player source, Player targetPlayer, OperationType type)
    {
        executorHub.logOperation(source, targetPlayer, type);
    }

    @Resolved(shouldSolveImmediately = true)
    private MorphManager manager;

    @Resolved(shouldSolveImmediately = true)
    private MorphClientHandler clientHandler;

    @Resolved
    private PlayerTracker tracker;

    @Resolved
    private PlayerOperationSimulator operationSimulator;

    protected PlayerOperationSimulator operationSimulator()
    {
        return operationSimulator;
    }

    protected PlayerTracker tracker()
    {
        return tracker;
    }

    protected MorphClientHandler clientHandler()
    {
        return clientHandler;
    }

    protected MorphManager morphManager()
    {
        return manager;
    }

    protected boolean playerNotDisguised(Player player)
    {
        //ignoreDisguised.get() ? false : !isDisguised;
        return manager.getDisguiseStateFor(player) == null;
    }

    @Nullable
    protected String getTargetControlFor(Player source)
    {
        return executorHub.getControl(source);
    }

    protected boolean playerInDistance(Player source, InteractionMirrorProcessor.PlayerInfo inf)
    {
        return playerInDistance(source, inf.target());
    }

    @Contract("_, null-> false; _, !null -> _")
    protected boolean playerInDistance(@NotNull Player source, @Nullable Player target)
    {
        if (target == null
                || !source.hasPermission(CommonPermissions.MIRROR) //检查来源是否有权限进行操控
                || target.hasPermission(CommonPermissions.MIRROR_IMMUNE) //检查目标是否免疫操控
                || target.getOpenInventory().getType() != InventoryType.CRAFTING //检查目标是否正和容器互动
                || target.isSleeping() //检查目标是否正在睡觉
                || target.isDead() //检查目标是否已经死亡
                || !DisguiseUtils.gameModeMirrorable(target)) //检查目标游戏模式是否满足操控条件
        {
            return false;
        }

        var isInSameWorld = target.getWorld().equals(source.getWorld());
        var normalDistance = executorHub.getControlDistance();

        //normalDistance为-1，总是启用，为0则禁用
        return normalDistance == -1
                || (normalDistance != 0 && isInSameWorld && target.getLocation().distance(source.getLocation()) <= normalDistance);
    }

    protected void simulateOperationAsync(Action action, Player targetPlayer, Player source, Consumer<Boolean> callback)
    {
        AtomicBoolean success = new AtomicBoolean(false);
        targetPlayer.getScheduler().run(plugin, task ->
        {
            success.set(simulateOperation(action, targetPlayer, source));
            callback.accept(success.get());
        }, () -> { /* retired */ });
    }

    protected abstract boolean simulateOperation(Action action, Player targetPlayer, Player source);
}

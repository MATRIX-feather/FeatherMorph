package xyz.nifeather.morph.events.mirror.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.events.mirror.ExecutorHub;

import java.util.ArrayList;
import java.util.List;
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
}

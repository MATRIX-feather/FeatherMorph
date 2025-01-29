package xyz.nifeather.morph.events.mirror.impl;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.events.InteractionMirrorProcessor;
import xyz.nifeather.morph.events.mirror.ExecutorHub;
import xyz.nifeather.morph.utilities.NmsUtils;

import java.util.List;

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
}

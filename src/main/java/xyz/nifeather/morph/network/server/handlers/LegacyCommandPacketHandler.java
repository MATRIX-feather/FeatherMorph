package xyz.nifeather.morph.network.server.handlers;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.network.server.handlers.results.CommandHandleResult;
import xyz.nifeather.morph.network.server.handlers.results.VersionHandleResult;

public class LegacyCommandPacketHandler implements ICommandPacketHandler
{
    @Override
    @NotNull
    public VersionHandleResult handleVersionData(Player player, byte @NotNull [] rawData)
    {
        throw new NotImplementedException();
    }

    @Override
    @NotNull
    public CommandHandleResult handleCommandData(Player player, byte @NotNull [] rawData)
    {
        throw new NotImplementedException();
    }
}

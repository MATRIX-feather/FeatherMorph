package xyz.nifeather.morph.network.server.handlers;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.network.server.handlers.results.CommandHandleResult;
import xyz.nifeather.morph.network.server.handlers.results.VersionHandleResult;

public interface ICommandPacketHandler
{
    @NotNull
    VersionHandleResult handleVersionData(Player player, byte @NotNull [] rawData);

    @NotNull
    CommandHandleResult handleCommandData(Player player, byte @NotNull [] rawData);
}

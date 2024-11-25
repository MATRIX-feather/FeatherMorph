package xyz.nifeather.morph.network.server.handlers;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.network.server.handlers.results.CommandHandleResult;
import xyz.nifeather.morph.network.server.handlers.results.VersionHandleResult;

import java.nio.charset.StandardCharsets;

public class LegacyCommandPacketHandler extends AbstractCommandPacketHandler
{
    public static final LegacyCommandPacketHandler INSTANCE = new LegacyCommandPacketHandler();

    @Override
    @NotNull
    public VersionHandleResult handleVersionData(Player player, byte @NotNull [] rawData)
    {
        try
        {
            var clientVersionStr = new String(rawData, StandardCharsets.UTF_8);

            return VersionHandleResult.from(Integer.parseInt(clientVersionStr));
        }
        catch (Throwable t)
        {
            logger.error("Failed to decode client version from legacy buffer: " + t.getMessage());
            return VersionHandleResult.fail();
        }
    }

    @Override
    @NotNull
    public CommandHandleResult handleCommandData(Player player, byte @NotNull [] rawData)
    {
        return CommandHandleResult.from(new String(rawData, StandardCharsets.UTF_8));
    }
}

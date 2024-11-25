package xyz.nifeather.morph.network.server.handlers;

import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.network.Constants;
import xiamomc.morph.network.InitializeState;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphStrings;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.network.server.handlers.results.CommandHandleResult;
import xyz.nifeather.morph.network.server.handlers.results.VersionHandleResult;

import java.util.Arrays;

public class CommandPacketHandler extends AbstractCommandPacketHandler
{
    public static final CommandPacketHandler INSTANCE = new CommandPacketHandler();

    @Override
    @NotNull
    public VersionHandleResult handleVersionData(@NotNull Player player, byte @NotNull [] data)
    {
        try
        {
            var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            var clientVersion = buf.readInt();

            return VersionHandleResult.from(clientVersion);
        }
        catch (Throwable t)
        {
            return VersionHandleResult.fail();
        }
/*
        try
        {
            if (clientVersion == -1)
                clientVersion = Integer.parseInt(new String(data, StandardCharsets.UTF_8));
        }
        catch (Throwable t)
        {
        }
*/
    }

    @Override
    public CommandHandleResult handleCommandData(@NotNull Player player, byte @NotNull [] data)
    {
        String input = null;

        try
        {
            var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));

            input = buf.readUtf();

            if (buf.readableBytes() > 0)
            {
                logger.error("Malformed buffer from '%s': still has readable bytes!".formatted(player.getName()));

                return CommandHandleResult.fail();
            }
        }
        catch (Throwable t)
        {
            logger.info("Failed to decode command from player '%s', rejecting: %s".formatted(player.getName(), t.getMessage()));

            return CommandHandleResult.fail();
        }

        return CommandHandleResult.from(input);
    }
}

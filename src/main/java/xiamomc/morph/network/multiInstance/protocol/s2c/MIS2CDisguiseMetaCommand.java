package xiamomc.morph.network.multiInstance.protocol.s2c;

import org.jetbrains.annotations.Nullable;
import xiamomc.morph.MorphPlugin;
import xiamomc.morph.network.multiInstance.protocol.IMasterHandler;
import xiamomc.morph.network.multiInstance.protocol.Operation;
import xiamomc.morph.network.multiInstance.protocol.SocketDisguiseMeta;

import java.util.List;
import java.util.UUID;

public class MIS2CDisguiseMetaCommand extends MIS2CCommand<SocketDisguiseMeta>
{

    public MIS2CDisguiseMetaCommand(SocketDisguiseMeta meta)
    {
        super("dmeta", meta);
    }

    public MIS2CDisguiseMetaCommand(Operation operation, List<String> identifiers, UUID bindingUUID)
    {
        this(new SocketDisguiseMeta(operation, identifiers, bindingUUID));
    }

    @Override
    public void onCommand(IMasterHandler handler)
    {
        handler.onDisguiseMetaCommand(this);
    }

    @Nullable
    public SocketDisguiseMeta getMeta()
    {
        return getArgumentAt(0);
    }

    public static MIS2CDisguiseMetaCommand from(String text)
    {
        try
        {
            return new MIS2CDisguiseMetaCommand(gson().fromJson(text, SocketDisguiseMeta.class));
        }
        catch (Throwable t)
        {
            var logger = MorphPlugin.getInstance().getSLF4JLogger();
            logger.warn("Failed to parse SocketDisguiseMeta from the server command! Leaving empty...");

            return new MIS2CDisguiseMetaCommand(Operation.INVALID, List.of(), UUID.randomUUID());
        }
    }
}

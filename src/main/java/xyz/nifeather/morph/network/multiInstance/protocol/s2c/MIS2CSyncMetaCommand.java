package xyz.nifeather.morph.network.multiInstance.protocol.s2c;

import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.network.multiInstance.protocol.IMasterHandler;
import xyz.nifeather.morph.network.multiInstance.protocol.Operation;
import xyz.nifeather.morph.network.multiInstance.protocol.SocketDisguiseMeta;

import java.util.List;
import java.util.UUID;

public class MIS2CSyncMetaCommand extends MIS2CCommand<SocketDisguiseMeta>
{

    public MIS2CSyncMetaCommand(SocketDisguiseMeta meta)
    {
        super("dmeta", meta);
    }

    public MIS2CSyncMetaCommand(Operation operation, List<String> identifiers, UUID bindingUUID)
    {
        this(new SocketDisguiseMeta(operation, identifiers, bindingUUID));
    }

    @Override
    public void onCommand(IMasterHandler handler)
    {
        handler.onSyncMetaCommand(this);
    }

    @Nullable
    public SocketDisguiseMeta getMeta()
    {
        return getArgumentAt(0);
    }

    public static MIS2CSyncMetaCommand from(String text)
    {
        try
        {
            return new MIS2CSyncMetaCommand(gson().fromJson(text, SocketDisguiseMeta.class));
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();
            logger.warn("Failed to parse SocketDisguiseMeta from the server command! Leaving empty...");

            return new MIS2CSyncMetaCommand(Operation.INVALID, List.of(), UUID.randomUUID());
        }
    }
}

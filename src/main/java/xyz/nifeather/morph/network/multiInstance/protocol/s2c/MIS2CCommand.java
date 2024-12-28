package xyz.nifeather.morph.network.multiInstance.protocol.s2c;

import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xiamomc.morph.network.BasicServerHandler;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.network.multiInstance.protocol.IMasterHandler;

public abstract class MIS2CCommand<T> extends AbstractS2CCommand<T>
{
    protected final Logger logger = FeatherMorphMain.getInstance().getSLF4JLogger();

    protected final String baseName;

    @Override
    public final void onCommand(BasicServerHandler<?> handler)
    {
    }

    @Override
    public String getBaseName()
    {
        return baseName;
    }

    public abstract void onCommand(IMasterHandler handler);

    public MIS2CCommand(String cmdBaseName, T... arguments)
    {
        super(arguments);
        this.baseName = cmdBaseName;
    }

    @Nullable
    private WebSocket sourceSocket;

    public void setSourceSocket(@NotNull WebSocket socket)
    {
        this.sourceSocket = socket;
    }

    @Nullable
    public WebSocket getSocket()
    {
        return sourceSocket;
    }
}

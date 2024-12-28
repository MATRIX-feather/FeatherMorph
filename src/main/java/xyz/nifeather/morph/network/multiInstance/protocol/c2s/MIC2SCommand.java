package xyz.nifeather.morph.network.multiInstance.protocol.c2s;

import org.java_websocket.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xiamomc.morph.network.BasicClientHandler;
import xiamomc.morph.network.commands.C2S.AbstractC2SCommand;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.network.multiInstance.protocol.IClientHandler;

public abstract class MIC2SCommand<T> extends AbstractC2SCommand<T>
{
    protected final Logger logger = FeatherMorphMain.getInstance().getSLF4JLogger();

    protected final String baseName;

    @Override
    public final void onCommand(BasicClientHandler<?> handler)
    {
    }

    @Override
    public String getBaseName()
    {
        return baseName;
    }

    public abstract void onCommand(IClientHandler handler);

    public MIC2SCommand(String cmdBaseName, T... arguments)
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

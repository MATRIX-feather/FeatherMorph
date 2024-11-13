package xyz.nifeather.morph.network.multiInstance.slave;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import xiamomc.pluginbase.XiaMoJavaPlugin;
import xyz.nifeather.morph.network.multiInstance.protocol.IMasterHandler;

import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InstanceClient extends WebSocketClient
{
    private final Logger logger;

    private final XiaMoJavaPlugin plugin;

    private final IMasterHandler masterHandler;

    public InstanceClient(URI serverUri, XiaMoJavaPlugin plugin, IMasterHandler masterHandler)
    {
        super(serverUri);

        this.logger = plugin.getSLF4JLogger();

        plugin.schedule(this::load);
        this.plugin = plugin;
        this.masterHandler = masterHandler;
    }

    private void load()
    {
    }

    private void logClientInfo(String message)
    {
        logger.info("[C@%s] %s".formatted(Integer.toHexString(this.hashCode()), message));
    }

    private void logClientWarn(String message)
    {
        logger.warn("[C@%s] %s".formatted(Integer.toHexString(this.hashCode()), message));
    }

    private final AtomicBoolean disposed = new AtomicBoolean(false);
    public void dispose()
    {
        this.disposed.set(true);
    }

    //region WebSocket stuffs

    private final AtomicBoolean connectionAlive = new AtomicBoolean(false);

    @Override
    public void onOpen(ServerHandshake serverHandshake)
    {
        logClientInfo("Opened connection to the instance server.");
        masterHandler.onConnectionOpen();
        connectionAlive.set(true);
    }

    @Override
    public void onMessage(String msg)
    {
        //logger.info("Received server message: " + msg);
        masterHandler.onText(msg);
    }

    @Override
    public void onClose(int code, String reason, boolean isFromRemote)
    {
        logClientInfo("Connection closed with code '%s' and reason '%s'".formatted(code, reason));
        this.connectionAlive.set(false);

        boolean shouldRetry = !reason.equalsIgnoreCase("NORETRY");

        var waitingSecond = 20;
        if (shouldRetry)
        {
            logClientInfo("Retrying connect after %s seconds...".formatted(waitingSecond));

            var connectionId = this.connectionId.incrementAndGet();
            plugin.schedule(() -> tryReconnect(connectionId), waitingSecond * 20);
        }
        else
        {
            logClientInfo("Not reconnecting because either the server or other sources declared NORETRY");
        }

        masterHandler.onConnectionClose(code);
    }

    private void tryReconnect(int connectId)
    {
        if (connectionId.get() != connectId)
        {
            logClientInfo("Not retrying because another connection is ongoing...");
            return;
        }

        this.reconnect();
    }

    private final AtomicInteger connectionId = new AtomicInteger(0);

    @Override
    public void connect()
    {
        if (disposed.get()) return;

        logClientInfo("Connecting to the instance server...");

        if (this.connectionAlive.get())
        {
            logClientWarn("Already connected to the server!");
            return;
        }

        connectionId.incrementAndGet();

        super.connect();
    }

    @Override
    public void onError(Exception e)
    {
        try
        {
            masterHandler.onClientError(e, this);
        }
        catch (Throwable t)
        {
            logClientWarn("Error occurred invoking onClientError(): " + t.getMessage());
            t.printStackTrace();
        }

        if (e instanceof ConnectException)
        {
            logClientInfo("Can't reach the server: " + e.getMessage());

            return;
        }

        logger.error("Unknown error occurred with the client %s: %s".formatted(Integer.toHexString(this.hashCode()), e.getMessage()));
        e.printStackTrace();
    }

    //endregion
}

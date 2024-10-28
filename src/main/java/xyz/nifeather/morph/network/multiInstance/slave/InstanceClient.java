package xyz.nifeather.morph.network.multiInstance.slave;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import xiamomc.pluginbase.XiaMoJavaPlugin;
import xyz.nifeather.morph.network.multiInstance.protocol.IMasterHandler;

import java.net.ConnectException;
import java.net.URI;

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

    //region WebSocket stuffs

    @Override
    public void onOpen(ServerHandshake serverHandshake)
    {
        logger.info("[C] Opened connection to the instance server.");
        masterHandler.onConnectionOpen();
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
        logger.info("[C] Connection closed with code '%s' and reason '%s'".formatted(code, reason));

        boolean shouldRetry = true;

        var waitingSecond = 20;
        if (shouldRetry)
        {
            logger.info("[C] Retrying connect after %s seconds...".formatted(waitingSecond));
            plugin.schedule(this::reconnect, waitingSecond * 20);
        }

        masterHandler.onConnectionClose(code);
    }

    @Override
    public void connect()
    {
        logger.info("[C] Connecting to the instance server...");
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
            logger.warn("[C] Error occurred invoking onClientError(): " + t.getMessage());
            t.printStackTrace();
        }

        if (e instanceof ConnectException)
        {
            logger.info("[C] Can't reach the server, retrying after 30 seconds: " + e.getMessage());
            plugin.schedule(this::reconnect, 30 * 20);

            return;
        }

        logger.error("Unknown error occurred with the client: " + e.getMessage());
        e.printStackTrace();
    }

    //endregion
}

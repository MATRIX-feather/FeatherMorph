package xyz.nifeather.morph.network.multiInstance.master;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xiamomc.pluginbase.XiaMoJavaPlugin;
import xyz.nifeather.morph.network.multiInstance.protocol.IClientHandler;

import java.net.InetSocketAddress;
import java.util.List;

public final class InstanceServer extends WebSocketServer
{
    private final Logger logger;

    private final IClientHandler clientHandler;

    public InstanceServer(XiaMoJavaPlugin plugin, InetSocketAddress address, IClientHandler iClientHandler)
    {
        super(address);

        this.logger = plugin.getSLF4JLogger();
        this.clientHandler = iClientHandler;

        plugin.schedule(this::load);
    }

    private void load()
    {
    }

    private final List<WebSocket> connectedSockets = new ObjectArrayList<>();

    public List<WebSocket> getConnectedSockets()
    {
        return new ObjectArrayList<>(connectedSockets);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
    {
        logger.info("[S] New connection opened: " + webSocket.getRemoteSocketAddress());

        connectedSockets.add(webSocket);
    }

    public boolean running;

    @Override
    public void stop(int timeout, String closeMessage) throws InterruptedException
    {
        logger.info("[S] Stopping instance server...");
        super.stop(timeout, closeMessage);

        running = false;
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b)
    {
        logger.info("[S] Connection closed: " + webSocket.getRemoteSocketAddress());

        connectedSockets.remove(webSocket);
        clientHandler.onConnectionClose(webSocket);
    }

    public record WsRecord(WebSocket socket, String rawMessage)
    {
    }

    @Override
    public void onMessage(WebSocket webSocket, String msg)
    {
        //logger.info("%s :: <- :: '%s'".formatted(webSocket.getRemoteSocketAddress(), msg));

        clientHandler.onMessage(new WsRecord(webSocket, msg), this);
    }

    @Override
    public void onError(@Nullable WebSocket webSocket, Exception e)
    {
        String socketAddress = "<unknown socket @ %s>".formatted(webSocket);
        if (webSocket != null) socketAddress = webSocket.getRemoteSocketAddress().toString();

        logger.warn("[S] An error occurred with socket '%s': %s".formatted(socketAddress, e.getMessage()));
        e.printStackTrace();
    }

    @Override
    public void onStart()
    {
        logger.info("[S] Master websocket server started on " + this.getAddress().toString());

        clientHandler.onServerStart(this);
        running = true;
    }

    public void dispose()
    {
    }
}

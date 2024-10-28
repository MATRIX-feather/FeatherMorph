package xyz.nifeather.morph.network.multiInstance.master;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.Bukkit;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.CommandRegistries;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.network.multiInstance.IInstanceService;
import xyz.nifeather.morph.network.multiInstance.protocol.IClientHandler;
import xyz.nifeather.morph.network.multiInstance.protocol.Operation;
import xyz.nifeather.morph.network.multiInstance.protocol.ProtocolLevel;
import xyz.nifeather.morph.network.multiInstance.protocol.c2s.MIC2SCommand;
import xyz.nifeather.morph.network.multiInstance.protocol.c2s.MIC2SDisguiseMetaCommand;
import xyz.nifeather.morph.network.multiInstance.protocol.c2s.MIC2SLoginCommand;
import xyz.nifeather.morph.network.multiInstance.protocol.s2c.*;
import xyz.nifeather.morph.network.multiInstance.slave.SlaveInstance;
import xyz.nifeather.morph.storage.playerdata.PlayerMeta;

import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MasterInstance extends MorphPluginObject implements IInstanceService, IClientHandler
{
    @Nullable
    private InstanceServer bindingServer;

    @Resolved
    private MorphConfigManager config;

    /**
     * @return Success?
     */
    private boolean stopServer()
    {
        try
        {
            if (bindingServer != null)
            {
                bindingServer.dispose();
                bindingServer.stop(0, "Master instance shutting down");
            }

            bindingServer = null;

            if (onStop != null)
                onStop.run();

            return true;
        }
        catch (Throwable t)
        {
            logger.error("[S] Error occurred shutting down socket server: " + t.getMessage());
            t.printStackTrace();

            return false;
        }
    }

    public boolean isOnline()
    {
        return bindingServer != null && bindingServer.running;
    }

    /**
     * @return Whether this operation operates successfully
     */
    private boolean prepareServer()
    {
        if (!stopServer())
            return false;

        try
        {
            String[] configuredAddress = config.getOrDefault(String.class, ConfigOption.MASTER_ADDRESS).split(":");

            String host = configuredAddress[0];
            int port = Integer.parseInt( configuredAddress.length >= 2 ? configuredAddress[1] : "39210" );
            var addr = new InetSocketAddress(InetAddress.getByName(host), port);

            bindingServer = new InstanceServer(plugin, addr, this);
            bindingServer.start();

            return true;
        }
        catch (Throwable t)
        {
            logger.warn("[S] Error occurred while setting up server:" + t.getMessage());
            t.printStackTrace();

            return false;
        }
    }

    private final Bindable<String> secret = new Bindable<>(null);
    private final Bindable<Boolean> debug_output = new Bindable<>(false);

    @Initializer
    private void load()
    {
        logger.info("Preparing multi-instance server...");

        config.bind(secret, ConfigOption.MASTER_SECRET);

        registries.registerC2S("login", MIC2SLoginCommand::from)
                .registerC2S("dmeta", MIC2SDisguiseMetaCommand::from);

        if (!prepareServer())
        {
            logger.error("Can't setup server, not enabling multi-instance service!");
            return;
        }
    }

    @Nullable
    public Runnable onStop;

    //region IInstanceService

    @Override
    public boolean stop()
    {
        return stopServer();
    }

    //endregion

    private void onText(InstanceServer.WsRecord record)
    {
        var ws = record.socket();
        var text = record.rawMessage().split(" ", 2);

        if (debug_output.get())
            logger.info("%s :: <- :: %s".formatted(ws.getRemoteSocketAddress(), record.rawMessage()));

        var cmd = registries.createC2SCommand(text[0], text.length == 2 ? text[1] : "");
        if (cmd == null)
        {
            logger.warn("[S] Unknown command: " + text[0]);
            return;
        }

        if (!(cmd instanceof MIC2SCommand<?> mic2s))
        {
            logger.warn("[S] Command is not a MIC2S instance!");
            return;
        }

        mic2s.setSourceSocket(ws);
        mic2s.onCommand(this);
    }

    //region IClientHandler

    private final CommandRegistries registries = new CommandRegistries();

    private final ProtocolLevel level = ProtocolLevel.V1;

    private final Map<WebSocket, ProtocolState> allowedSockets = new Object2ObjectArrayMap<>();

    @ApiStatus.Internal
    public void broadcastCommand(MIS2CCommand<?> command)
    {
        for (var allowedSocket : this.allowedSockets.keySet())
            this.sendCommand(allowedSocket, command);
    }

    private void sendCommand(WebSocket socket, MIS2CCommand<?> command)
    {
        if (!socket.isOpen())
        {
            logger.warn("[S] Not sending commands to a closed socket! %s".formatted(socket.getRemoteSocketAddress()));
            return;
        }

        //logger.info("%s :: -> :: %s".formatted(socket.getRemoteSocketAddress(), command.buildCommand()));

        socket.send(command.buildCommand());
    }

    private void disconnect(WebSocket socket, String reason)
    {
        this.sendCommand(socket, new MIS2CDisconnectCommand(CloseFrame.NORMAL, reason));

        this.allowedSockets.remove(socket);
        socket.close();
    }

    private boolean socketAllowed(WebSocket socket)
    {
        return allowedSockets.getOrDefault(socket, null) != null;
    }

    private void switchState(WebSocket socket, ProtocolState state)
    {
        allowedSockets.put(socket, state);
        sendCommand(socket, new MIS2CStateCommand(state));
    }

    public ProtocolState getConnectionState(WebSocket socket)
    {
        return allowedSockets.getOrDefault(socket, ProtocolState.INVALID);
    }

    //todo: 状态切换应当分为多个函数执行
    //      现在这么做很💩️
    @Override
    public void onLoginCommand(MIC2SLoginCommand cProtocolCommand)
    {
        var socket = cProtocolCommand.getSocket();
        if (socket == null)
        {
            logger.info("Received a login request from an unknown source, not processing.");
            return;
        }

        logger.info("[S] '%s' is requesting a login".formatted(socket.getRemoteSocketAddress()));

        this.switchState(socket, ProtocolState.LOGIN);

        if (debug_output.get())
            logger.info("Level is '%s', and their secret is '%s'".formatted(cProtocolCommand.getVersion(), cProtocolCommand.getSecret()));

        if (!this.level.equals(cProtocolCommand.getVersion()))
        {
            logger.info("[S] Protocol mismatch! Disconnecting...");

            this.disconnect(socket, "Protocol mismatch!");
            return;
        }

        if (cProtocolCommand.getSecret() == null || !cProtocolCommand.getSecret().equals(this.secret.get()))
        {
            logger.info("[S] Invalid secret! Disconnecting...");

            disconnect(socket, "Invalid secret '%s'".formatted(cProtocolCommand.getSecret()));
            return;
        }

        logger.info("[S] '%s' logged in".formatted(socket.getRemoteSocketAddress()));

        sendCommand(socket, new MIS2CLoginResultCommand(true));
        switchState(socket, ProtocolState.SYNC);

        var cmds = new ObjectArrayList<MIS2CSyncMetaCommand>();
        var disguises = disguiseManager.listAllMeta();
        for (var meta : disguises)
        {
            var identifiers = meta.getUnlockedDisguiseIdentifiers();

            if (!identifiers.isEmpty())
                cmds.add(new MIS2CSyncMetaCommand(Operation.ADD_IF_ABSENT, identifiers, meta.uniqueId));
        }

        cmds.forEach(cmd -> this.sendCommand(socket, cmd));
    }

    private final NetworkDisguiseManager disguiseManager = new NetworkDisguiseManager();

    /*
        缺陷：当子服断开链接后，若玩家在其中被剥夺了伪装，那么在重新连接后此变化不会在整个网络的其他部分生效
             如果设置会移除主服务器中不存在的条目，那么其他条目少的子服接入时会清空主服务器当前已有的条目
     */
    @Override
    public void onDisguiseMetaCommand(MIC2SDisguiseMetaCommand cDisguiseMetaCommand)
    {
        var socket = cDisguiseMetaCommand.getSocket();
        if (!socketAllowed(socket))
            return;

        assert socket != null;

        var meta = cDisguiseMetaCommand.getMeta();
        if (meta == null || !meta.isValid())
        {
            logger.warn("[S] Bad client implementation? Got invalid meta from '%s'".formatted(socket.getRemoteSocketAddress()));
            return;
        }

        var state = getConnectionState(socket);

        if (!state.loggedIn())
        {
            logger.warn("[S] Bad client implementation? They sent meta sync before they login! (%s)".formatted(socket.getRemoteSocketAddress()));
            return;
        }

        if (state == ProtocolState.SYNC)
            switchState(socket, ProtocolState.WAIT_LISTEN);

        var operation = meta.getOperation();
        var identifiers = meta.getIdentifiers();

        var playerMeta = disguiseManager.getPlayerMeta(Bukkit.getOfflinePlayer(Objects.requireNonNull(meta.getBindingUuid(), "???")));
        var unlocked = playerMeta.getUnlockedDisguises();

        if (operation == Operation.ADD_IF_ABSENT)
        {
            identifiers.forEach(id ->
            {
                var disguiseMeta = disguiseManager.getDisguiseMeta(id);

                if (!unlocked.contains(disguiseMeta))
                    playerMeta.addDisguise(disguiseMeta);
            });

            // Broadcast to all allowed sockets
            for (var allowedSocket : this.allowedSockets.keySet())
            {
                if (allowedSocket == cDisguiseMetaCommand.getSocket())
                    continue;

                this.sendCommand(allowedSocket, new MIS2CSyncMetaCommand(meta));
            }
        }
        else if (operation == Operation.REMOVE)
        {
            identifiers.forEach(id ->
            {
                var disguiseMeta = disguiseManager.getDisguiseMeta(id);

                playerMeta.removeDisguise(disguiseMeta);
            });

            // Broadcast to all allowed sockets
            for (var allowedSocket : this.allowedSockets.keySet())
            {
                if (allowedSocket == cDisguiseMetaCommand.getSocket())
                    continue;

                this.sendCommand(allowedSocket, new MIS2CSyncMetaCommand(meta));
            }
        }
    }

    @Override
    public void onMessage(InstanceServer.WsRecord wsRecord, InstanceServer server)
    {
        if (!allowedSockets.containsKey(wsRecord.socket())) return;

        this.addSchedule(() -> this.onText(wsRecord));
    }

    @Override
    public void onServerStart(InstanceServer server)
    {
        var slave = slaveWeakRef.get();
        if (slave == null) return;

        try
        {
            slave.onInternalMasterStart(this);
        }
        catch (Throwable t)
        {
            logger.error("Error occurred while setting up internal client. Stopping master server!");
            logger.warn(t.getMessage());
            t.printStackTrace();

            this.stop();
        }
    }

    @Override
    public void onConnectionClose(WebSocket socket)
    {
        allowedSockets.remove(socket);
    }

    //endregion

    //region Utilities

    @NotNull
    private WeakReference<SlaveInstance> slaveWeakRef = new WeakReference<>(null);

    public void setInternalSlave(SlaveInstance slave)
    {
        this.slaveWeakRef = new WeakReference<>(slave);
    }

    public void onInternalSlaveError(SlaveInstance slave, Exception e)
    {
        if (e instanceof ConnectException) return;

        logger.error("Error occurred with the internal client! Stopping master server...");
        this.stop();
    }

    public void loadInitialDisguises(List<PlayerMeta> metaList)
    {
        this.disguiseManager.merge(metaList);
    }

    //endregion
}

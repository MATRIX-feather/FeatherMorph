package xyz.nifeather.morph.network.multiInstance.slave;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.Bukkit;
import org.java_websocket.framing.CloseFrame;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.C2S.AbstractC2SCommand;
import xiamomc.morph.network.commands.CommandRegistries;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.network.multiInstance.IInstanceService;
import xyz.nifeather.morph.network.multiInstance.master.MasterInstance;
import xyz.nifeather.morph.network.multiInstance.protocol.IMasterHandler;
import xyz.nifeather.morph.network.multiInstance.protocol.Operation;
import xyz.nifeather.morph.network.multiInstance.protocol.ProtocolLevel;
import xyz.nifeather.morph.network.multiInstance.protocol.SocketDisguiseMeta;
import xyz.nifeather.morph.network.multiInstance.protocol.c2s.MIC2SDisguiseMetaCommand;
import xyz.nifeather.morph.network.multiInstance.protocol.c2s.MIC2SLoginCommand;
import xyz.nifeather.morph.network.multiInstance.protocol.s2c.*;
import xyz.nifeather.morph.network.server.MorphClientHandler;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SlaveInstance extends MorphPluginObject implements IInstanceService, IMasterHandler
{
    @Nullable
    private InstanceClient client;

    private boolean stopClient()
    {
        if (client == null) return true;

        try
        {
            client.close(CloseFrame.NORMAL);
            client = null;

            return true;
        }
        catch (Throwable t)
        {
            logger.warn("Can't close client! " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    @Resolved
    private MorphConfigManager config;

    @Nullable
    private MasterInstance internalMasterInstance;

    public void onInternalMasterStart(MasterInstance masterInstance)
    {
        if (prepareClient())
        {
            this.internalMasterInstance = masterInstance;
            return;
        }

        throw new IllegalStateException("Can't setup client!");
    }

    /**
     * @return Whether this operation operates successfully
     */
    private boolean prepareClient()
    {
        if (!stopClient())
            return false;

        try
        {
            var rawAddr = config.getOrDefault(String.class, ConfigOption.MASTER_ADDRESS);
            var uri = URI.create("ws://" + rawAddr);

            var client = new InstanceClient(uri, plugin, this);

            this.client = client;
            CompletableFuture.runAsync(client);

            return true;
        }
        catch (Throwable t)
        {
            logger.warn("Error occurred setting up client: " + t.getMessage());
            t.printStackTrace();

            return false;
        }
    }

    public SlaveInstance(boolean startOnLoad)
    {
        this.startOnLoad = startOnLoad;
    }

    private final boolean startOnLoad;

    @Initializer
    private void load()
    {
        logger.info("Preparing multi-instance client...");

        config.bind(secret, ConfigOption.MASTER_SECRET);

        registries.registerS2C("deny", MIS2CDisconnectCommand::from)
                .registerS2C("dmeta", MIS2CSyncMetaCommand::from)
                .registerS2C("r_login", MIS2CLoginResultCommand::from)
                .registerS2C("state", MIS2CStateCommand::from);

        if (client != null) return;

        if (!startOnLoad) return;

        if (!prepareClient())
        {
            logger.warn("Can't setup client, this instance will stay offline from the instance network!");
            return;
        }
    }

    private final Bindable<String> secret = new Bindable<>(null);

    @Override
    public boolean stop()
    {
        return stopClient();
    }

    @Resolved
    private MorphManager morphManager;

    @Resolved
    private MorphClientHandler clientHandler;

    @ApiStatus.Internal
    public void sendCommand(AbstractC2SCommand<?> command)
    {
        if (silent)
            return;

        if (client == null)
            throw new NullDependencyException("Null client!");

        client.send(command.buildCommand());
    }

    public boolean isOnline()
    {
        return client != null && client.isOpen();
    }

    @Override
    public void onSyncMetaCommand(MIS2CSyncMetaCommand metaCommand)
    {
        if (!currentState.get().loggedIn())
        {
            logger.warn("Bad server implementation? They are trying to sync meta before we login!");
            return;
        }

        var meta = metaCommand.getMeta();
        if (meta == null)
        {
            logger.warn("Bad server implementation? Get DisguiseMeta command but meta is null!");
            return;
        }

        if (!meta.isValid())
        {
            logger.warn("Bad server implementation? The meta is invalid!");
            return;
        }

        var operation = meta.getOperation();
        var offlinePlayer = Bukkit.getOfflinePlayer(Objects.requireNonNull(meta.getBindingUuid(), "???"));

        var playerMeta = morphManager.getPlayerMeta(offlinePlayer);
        var unlocked = playerMeta.getUnlockedDisguises();

        var player = offlinePlayer.getPlayer();

        silent = true;

        if (operation == Operation.ADD_IF_ABSENT)
        {
            var countPrev = playerMeta.getUnlockedDisguises().size();
            meta.getIdentifiers().forEach(id ->
            {
                var disguiseMeta = morphManager.getDisguiseMeta(id);

                if (!unlocked.contains(disguiseMeta))
                    playerMeta.addDisguise(disguiseMeta);
            });

            //morphManager.saveConfiguration();

            if (player != null && playerMeta.getUnlockedDisguiseIdentifiers().size() != countPrev)
                clientHandler.refreshPlayerClientMorphs(playerMeta.getUnlockedDisguiseIdentifiers(), player);
        }
        else if (operation == Operation.REMOVE)
        {
            var countPrev = playerMeta.getUnlockedDisguises().size();

            meta.getIdentifiers().forEach(id ->
            {
                var disguiseMeta = morphManager.getDisguiseMeta(id);

                playerMeta.removeDisguise(disguiseMeta);
            });

            //morphManager.saveConfiguration();

            if (player != null && playerMeta.getUnlockedDisguiseIdentifiers().size() != countPrev)
                clientHandler.refreshPlayerClientMorphs(playerMeta.getUnlockedDisguiseIdentifiers(), player);
        }

        silent = false;
    }

    @Override
    public void onDisconnectCommand(MIS2CDisconnectCommand cDenyCommand)
    {
        this.stopClient();
    }

    @Override
    public void onLoginResultCommand(MIS2CLoginResultCommand cLoginResultCommand)
    {
        if (currentState.get() != ProtocolState.LOGIN)
        {
            logger.warn("[C] Bad server implementation? They sent a login result at when we are not in a login process!");
            return;
        }

        if (!cLoginResultCommand.isAllowed()) return;

        var cmds = new ObjectArrayList<MIC2SDisguiseMetaCommand>();
        var disguises = morphManager.listAllPlayerMeta();
        for (var meta : disguises)
        {
            var identifiers = meta.getUnlockedDisguiseIdentifiers();

            if (!identifiers.isEmpty())
                cmds.add(new MIC2SDisguiseMetaCommand(Operation.ADD_IF_ABSENT, identifiers, meta.uniqueId));
        }

        for (var socketMeta : revokeStatesAfterDisconnect)
            cmds.add(new MIC2SDisguiseMetaCommand(socketMeta));

        revokeStatesAfterDisconnect.clear();

        cmds.forEach(this::sendCommand);
    }

    private final Bindable<ProtocolState> currentState = new Bindable<>(ProtocolState.NOT_CONNECTED);

    private final List<SocketDisguiseMeta> revokeStatesAfterDisconnect = new ObjectArrayList<>();

    public void cacheRevokeStates(SocketDisguiseMeta socketDisguiseMeta)
    {
        revokeStatesAfterDisconnect.add(socketDisguiseMeta);
    }

    @Override
    public void onStateCommand(MIS2CStateCommand cStateCommand)
    {
        if (cStateCommand.getState() == ProtocolState.INVALID)
            logger.warn("[C] Bad server implementation? The new session state is invalid!");

        switchState(cStateCommand.getState());
    }

    private void switchState(ProtocolState newState)
    {
        logger.info("[C] Client networking state switched to " + newState);
        currentState.set(newState);
    }

    private final ProtocolLevel implementingLevel = ProtocolLevel.V1;

    @Override
    public void onConnectionOpen()
    {
        this.addSchedule(() -> this.sendCommand(new MIC2SLoginCommand(implementingLevel, secret.get())));
    }

    @Nullable
    public Consumer<Integer> onClose;

    @Override
    public void onConnectionClose(int code)
    {
        if (onClose != null)
            onClose.accept(code);
    }

    @Override
    public void onClientError(Exception e, InstanceClient client)
    {
        if (internalMasterInstance != null)
            internalMasterInstance.onInternalSlaveError(this, e);
    }

    private final CommandRegistries registries = new CommandRegistries();

    @Override
    public void onText(String text)
    {
        this.addSchedule(() -> onCommandRaw(text));
    }

    private void onCommandRaw(String raw)
    {
        var text = raw.split(" ", 2);
        var cmd = registries.createS2CCommand(text[0], text.length == 2 ? text[1] : "");
        if (cmd == null)
        {
            logger.warn("[C] Unknown command: " + text[0]);
            return;
        }

        if (!(cmd instanceof MIS2CCommand<?> mis2c))
        {
            logger.warn("[C] Command '%s' is not a MIS2C instance!".formatted(cmd));
            return;
        }

        mis2c.onCommand(this);
    }

    private boolean silent = false;
}

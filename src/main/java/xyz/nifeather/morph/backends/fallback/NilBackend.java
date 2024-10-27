package xyz.nifeather.morph.backends.fallback;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapAddCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapRemoveCommand;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.backends.DisguiseBackend;
import xyz.nifeather.morph.backends.DisguiseWrapper;
import xyz.nifeather.morph.backends.WrapperProperties;
import xyz.nifeather.morph.messages.BackendStrings;
import xyz.nifeather.morph.misc.NetworkingHelper;
import xyz.nifeather.morph.network.server.MorphClientHandler;

import java.util.Collection;
import java.util.Map;

public class NilBackend extends DisguiseBackend<NilDisguise, NilWrapper>
{
    /**
     * Gets the identifier of this backend.
     *
     * @return An identifier of this backend.
     */
    @Override
    public String getIdentifier()
    {
        return "nil";
    }

    @Override
    public FormattableMessage getDisplayName()
    {
        return BackendStrings.nilBackendName();
    }

    @Override
    public boolean dependsClientRenderer()
    {
        return true;
    }

    @Override
    public DisguiseWrapper<NilDisguise> createInstance(@NotNull Entity targetEntity)
    {
        var wrapper = new NilWrapper(new NilDisguise(targetEntity.getType()), this);
        wrapper.setDisguiseName(targetEntity.getName());

        return wrapper;
    }

    @Override
    public DisguiseWrapper<NilDisguise> createInstance(EntityType entityType)
    {
        return new NilWrapper(new NilDisguise(entityType), this);
    }

    @Override
    public DisguiseWrapper<NilDisguise> createPlayerInstance(String targetPlayerName)
    {
        var wrapper = new NilWrapper(new NilDisguise(EntityType.PLAYER), this);
        wrapper.setDisguiseName(targetPlayerName);

        return wrapper;
    }

    @Override
    public NilDisguise createRawInstance(Entity entity)
    {
        return new NilDisguise(entity.getType());
    }

    @Override
    public boolean isDisguised(@Nullable Entity target)
    {
        return playerFallbackWrapperMap.containsKey(target);
    }

    @Override
    public NilWrapper getWrapper(Entity target)
    {
        if (!(target instanceof Player player)) return null;

        return playerFallbackWrapperMap.getOrDefault(player, null);
    }

    /**
     * 从给定的Wrapper克隆一个属于此后端的新Wrapper
     *
     * @param otherWrapper 可能属于其他后端的Wrapper
     * @return 一个新的属于此后端的Wrapper
     */
    @Override
    public @NotNull NilWrapper cloneWrapperFrom(DisguiseWrapper<?> otherWrapper)
    {
        return otherWrapper instanceof NilWrapper nilWrapper
                ? cloneWrapper(nilWrapper)
                : cloneOther(otherWrapper);
    }

    private NilWrapper cloneWrapper(NilWrapper other)
    {
        return (NilWrapper) other.clone();
    }

    private NilWrapper cloneOther(DisguiseWrapper<?> other)
    {
        return NilWrapper.fromExternal(other, this);
    }

    private final Map<Player, NilWrapper> playerFallbackWrapperMap = new Object2ObjectOpenHashMap<>();

    @Resolved(shouldSolveImmediately = true)
    private NetworkingHelper networkingHelper;

    NetworkingHelper getNetworkingHelper()
    {
        return networkingHelper;
    }

    @Override
    public boolean disguise(Player player, DisguiseWrapper<?> rawWrapper)
    {
        if (!(rawWrapper instanceof NilWrapper wrapper))
            return false;

        if (playerFallbackWrapperMap.containsKey(player))
            unDisguise(player);

        //发送元数据

        var players = new ObjectArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player);
        var cmd = new S2CRenderMapAddCommand(player.getEntityId(), wrapper.readPropertyOrThrow(WrapperProperties.DISGUISE_ID));
        players.forEach(p -> clientHandler.sendCommand(p, cmd));

        networkingHelper.prepareMeta(player)
                .forWrapper(rawWrapper)
                .send();

        wrapper.setBindingPlayer(player);

        playerFallbackWrapperMap.put(player, wrapper);
        return true;
    }

    @Resolved
    private MorphClientHandler clientHandler;

    @Override
    public boolean unDisguise(Player player)
    {
        var wrapper = playerFallbackWrapperMap.getOrDefault(player, null);

        if (wrapper != null)
            wrapper.dispose();

        var cmd = new S2CRenderMapRemoveCommand(player.getEntityId());
        var players = new ObjectArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player);
        players.forEach(p -> clientHandler.sendCommand(p, cmd));

        playerFallbackWrapperMap.remove(player);

        return true;
    }

    /**
     * Deserialize a wrapper instance from the giving parameter
     *
     * @param offlineParameter The parameter to deserialize
     * @return A wrapper that presents the giving parameter.
     * null if invalid or illegal
     */
    @Override
    public @Nullable NilWrapper fromOfflineSave(String offlineParameter)
    {
        return null;
    }

    /**
     * Serialize a wrapper instance to a string that can be saved in the Offline Storage
     *
     * @param wrapper The target wrapper to save
     * @return A serialized string that can be deserialized to a wrapper in the future.
     * Null if the giving wrapper is not supported by this backend.
     */
    @Override
    public @Nullable String toOfflineSave(DisguiseWrapper<?> wrapper)
    {
        return null;
    }

    @Override
    public Collection<NilWrapper> listInstances()
    {
        return playerFallbackWrapperMap.values();
    }
}

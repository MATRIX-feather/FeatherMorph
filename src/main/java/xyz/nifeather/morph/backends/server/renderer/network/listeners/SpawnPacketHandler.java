package xyz.nifeather.morph.backends.server.renderer.network.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.backends.server.renderer.network.DisplayParameters;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.SingleWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.PlayerWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.RenderRegistry;
import xyz.nifeather.morph.backends.server.renderer.utilties.WatcherUtils;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.misc.skins.PlayerSkinProvider;

import java.util.List;
import java.util.UUID;

public class SpawnPacketHandler extends ProtocolListener
{
    @Resolved(shouldSolveImmediately = true)
    private RenderRegistry registry;

    @Override
    public String getIdentifier()
    {
        return "spawn_listener";
    }

    public SpawnPacketHandler()
    {
        registry.onRegister(this, ep ->
                refreshStateForPlayer(ep.player(), getAffectedPlayers(ep.player())));

        registry.onUnRegister(this, ep ->
                unDisguiseForPlayer(ep.player(), ep.watcher()));
    }

    private List<Player> getAffectedPlayers(Player sourcePlayer)
    {
        return WatcherUtils.getAffectedPlayers(sourcePlayer);
    }

    private void unDisguiseForPlayer(@Nullable Player player, SingleWatcher disguiseWatcher)
    {
        if (player == null) return;

        var protocolManager = ProtocolLibrary.getProtocolManager();
        var affectedPlayers = getAffectedPlayers(player);
        var watcher = new PlayerWatcher(player);
        watcher.markSilent(this);

        watcher.writeEntry(CustomEntries.PROFILE, ((CraftPlayer) player).getProfile());
        watcher.writeEntry(CustomEntries.SPAWN_UUID, player.getUniqueId());
        watcher.writeEntry(CustomEntries.SPAWN_ID, player.getEntityId());
        watcher.writeEntry(CustomEntries.PROFILE_LISTED, true);

        var packets = getFactory().buildSpawnPackets(new DisplayParameters(watcher));

        var removePacket = new ClientboundRemoveEntitiesPacket(player.getEntityId());
        var rmPacketContainer = PacketContainer.fromPacket(removePacket);

        if (disguiseWatcher.getEntityType() == org.bukkit.entity.EntityType.PLAYER
                && !disguiseWatcher.readEntryOrDefault(CustomEntries.PROFILE_LISTED, false))
        {
            var disguiseUUID = disguiseWatcher.readEntryOrThrow(CustomEntries.SPAWN_UUID);

            var packetRemoveInfo = PacketContainer.fromPacket(
                    new ClientboundPlayerInfoRemovePacket(List.of(disguiseUUID)));

            Bukkit.getOnlinePlayers().forEach(p -> protocolManager.sendServerPacket(p, packetRemoveInfo));
        }

        watcher.dispose();

        affectedPlayers.forEach(p ->
        {
            protocolManager.sendServerPacket(p, rmPacketContainer);

            for (PacketContainer packet : packets)
                protocolManager.sendServerPacket(p, packet);
        });
    }

    private void refreshStateForPlayer(@Nullable Player player, List<Player> affectedPlayers)
    {
        if (player == null) return;

        var watcher = registry.getWatcher(player.getUniqueId());
        if (watcher == null)
            throw new NullDependencyException("Null Watcher for a existing player?!");

        refreshStateForPlayer(player,
                new DisplayParameters(watcher),
                affectedPlayers);
    }

    /**
     * 刷新玩家的伪装
     * @param player 目标玩家
     * @param displayParameters 和伪装对应的 {@link DisplayParameters}
     */
    private void refreshStateForPlayer(@Nullable Player player, @NotNull DisplayParameters displayParameters, List<Player> affectedPlayers)
    {
        if (affectedPlayers.isEmpty()) return;

        if (player == null) return;
        var watcher = displayParameters.getWatcher();

        var protocolManager = ProtocolLibrary.getProtocolManager();

        //先发包移除当前实体
        var packetRemove = new ClientboundRemoveEntitiesPacket(player.getEntityId());
        var packetRemoveContainer = PacketContainer.fromPacket(packetRemove);

        //然后发包创建实体
        //确保gameProfile非空
        //如果没有profile，那么随机一个并计划刷新
        if (watcher.getEntityType() == org.bukkit.entity.EntityType.PLAYER && watcher.readEntry(CustomEntries.PROFILE) == null)
        {
            var disguiseName = watcher.readEntry(CustomEntries.DISGUISE_NAME);

            if (disguiseName == null || disguiseName.isBlank())
            {
                logger.error("Parameter 'disguiseName' cannot be null or blank!");
                Thread.dumpStack();
                return;
            }

            var targetPlayer = Bukkit.getPlayerExact(disguiseName);

            GameProfile targetProfile = watcher.readEntryOrDefault(CustomEntries.PROFILE, null);

            if (targetProfile == null)
            {
                //皮肤在其他地方（例如PlayerDisguiseProvider#makeWrapper）中有做获取处理
                //因此这里只根据情况从缓存或者找到的玩家获取皮肤
                targetProfile = targetPlayer == null
                        ? PlayerSkinProvider.getInstance().getCachedProfile(disguiseName)
                        : NmsRecord.ofPlayer(targetPlayer).gameProfile;
            }

            watcher.writeEntry(CustomEntries.PROFILE, targetProfile == null ? new GameProfile(UUID.randomUUID(), disguiseName) : targetProfile);
        }

        var parametersFinal = new DisplayParameters(watcher); //.setDontIncludeMeta();
        var spawnPackets = getFactory().buildSpawnPackets(parametersFinal);

        affectedPlayers.forEach(p ->
        {
            protocolManager.sendServerPacket(p, packetRemoveContainer);

            spawnPackets.forEach(packet -> protocolManager.sendServerPacket(p, packet));
        });
    }

    private void onEntityAddPacket(ClientboundAddEntityPacket packet, PacketEvent packetEvent)
    {
        var packetContainer = packetEvent.getPacket();

        //忽略不在注册表中的玩家
        var bindingWatcher = registry.getWatcher(packet.getUUID());
        if (bindingWatcher == null)
            return;

        //不要二次处理来自我们自己的包
        if (!getFactory().isPacketOurs(packetContainer));
        {
            packetEvent.setCancelled(true);
            refreshStateForPlayer(Bukkit.getPlayer(packet.getUUID()), List.of(packetEvent.getPlayer()));
        }
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent)
    {
        if (!packetEvent.isServerPacket()) return;

        var packetContainer = packetEvent.getPacket();
        if (packetContainer.getHandle() instanceof ClientboundAddEntityPacket originalPacket
                && originalPacket.getType() == EntityType.PLAYER)
        {
            onEntityAddPacket(originalPacket, packetEvent);
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent)
    {
    }

    @Override
    public ListeningWhitelist getSendingWhitelist()
    {
        return ListeningWhitelist
                .newBuilder()
                .types(PacketType.Play.Server.SPAWN_ENTITY)
                .gamePhase(GamePhase.PLAYING)
                .build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist()
    {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }
}

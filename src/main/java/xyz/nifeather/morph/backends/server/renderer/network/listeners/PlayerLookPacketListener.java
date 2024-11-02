package xyz.nifeather.morph.backends.server.renderer.network.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.util.Mth;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.backends.server.renderer.network.registries.RenderRegistry;

public class PlayerLookPacketListener extends ProtocolListener
{
    @Override
    public String getIdentifier()
    {
        return "look_move_listener";
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        var packetType = event.getPacketType();

        var packet = event.getPacket();
        //event.setCancelled(true);

        //不要处理来自我们自己的包
        if (getFactory().isPacketOurs(packet))
        {
            return;
        }

        if (packetType == PacketType.Play.Server.ENTITY_LOOK
                || packetType == PacketType.Play.Server.REL_ENTITY_MOVE
                || packetType == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK)
        {
            //PacketPlayOutEntity$PacketPlayOutEntityLook
            var cast = (ClientboundMoveEntityPacket)packet.getHandle();
            onLookPacket(cast, event);
        }
        else if (packetType == PacketType.Play.Server.ENTITY_HEAD_ROTATION)
        {
            var cast = (ClientboundRotateHeadPacket)packet.getHandle();
            onHeadRotation(cast, event);
        }
        else if (packetType == PacketType.Play.Server.ENTITY_TELEPORT)
        {
            var cast = (ClientboundTeleportEntityPacket)packet.getHandle();
            onTeleport(cast, event);
        }
        else
        {
            //logger.error("Invalid packet type: " + packetType);
        }
    }
    @Resolved(shouldSolveImmediately = true)
    private RenderRegistry registry;

    private void onTeleport(ClientboundTeleportEntityPacket packet, PacketEvent event)
    {
        //获取此包的来源实体
        var sourceNmsEntity = getNmsPlayerFrom(packet.id());
        if (sourceNmsEntity == null)
            return;

        if (!(sourceNmsEntity.getBukkitEntity() instanceof Player sourcePlayer)) return;

        var watcher = registry.getWatcher(sourcePlayer.getUniqueId());

        if (watcher == null)
            return;

        var isDragon = watcher.getEntityType() == EntityType.ENDER_DRAGON;
        var isPhantom = watcher.getEntityType() == EntityType.PHANTOM;

        if (!isDragon && !isPhantom)
            return;

        var yaw = packet.change().yRot();
        var pitch = packet.change().xRot();

        var playerYaw = isDragon ? (sourcePlayer.getYaw() + 180f) : sourcePlayer.getYaw();
        yaw = (playerYaw / 360f) * 256f;

        var playerPitch = isPhantom ? -sourcePlayer.getPitch() : sourcePlayer.getPitch();

        pitch = (playerPitch / 360f) * 256f;

        var container = event.getPacket();
        container.getBytes().write(0, Mth.packDegrees(yaw));
        container.getBytes().write(1, Mth.packDegrees(pitch));
    }

    private void onHeadRotation(ClientboundRotateHeadPacket packet, PacketEvent event)
    {
        //获取此包的来源实体
        var sourceNmsEntity = this.getNmsPlayerEntityFromUnreadablePacket(packet);
        if (sourceNmsEntity == null) return;

        if (!(sourceNmsEntity.getBukkitEntity() instanceof Player sourcePlayer)) return;

        var watcher = registry.getWatcher(sourcePlayer.getUniqueId());

        if (watcher == null || watcher.getEntityType() != EntityType.ENDER_DRAGON)
            return;

        var newHeadYaw = (byte)(((sourcePlayer.getYaw() + 180f) / 360f) * 256f);

        var newPacket = new ClientboundRotateHeadPacket(sourceNmsEntity, newHeadYaw);
        var finalPacket = PacketContainer.fromPacket(newPacket);
        getFactory().markPacketOurs(finalPacket);

        event.setPacket(finalPacket);
    }

    private void onLookPacket(ClientboundMoveEntityPacket packet, PacketEvent event)
    {
        //获取此包的来源实体
        var sourceNmsEntity = this.getNmsPlayerEntityFromUnreadablePacket(packet);

        if (sourceNmsEntity == null) return;

        if (!(sourceNmsEntity.getBukkitEntity() instanceof Player sourcePlayer)) return;

        var watcher = registry.getWatcher(sourcePlayer.getUniqueId());

        if (watcher == null)
            return;

        var isDragon = watcher.getEntityType() == EntityType.ENDER_DRAGON;
        var isPhantom = watcher.getEntityType() == EntityType.PHANTOM;

        if (!isDragon && !isPhantom)
            return;

        var yaw = packet.getyRot();
        var pitch = packet.getxRot();

        var playerYaw = isDragon ? (sourcePlayer.getYaw() + 180f) : sourcePlayer.getYaw();
        var finalYaw = (playerYaw / 360f) * 256f;
        yaw = (byte)finalYaw;

        var playerPitch = isPhantom ? -sourcePlayer.getPitch() : sourcePlayer.getPitch();

        var finalPitch = (playerPitch / 360f) * 256f;
        pitch = (byte)finalPitch;

        ClientboundMoveEntityPacket newPacket;

        var packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.ENTITY_LOOK)
        {
            newPacket = new ClientboundMoveEntityPacket.Rot(
                    sourcePlayer.getEntityId(),
                    Mth.packDegrees(yaw), Mth.packDegrees(pitch),
                    packet.isOnGround()
            );
        }
        else if (packetType == PacketType.Play.Server.REL_ENTITY_MOVE)
        {
            newPacket = new ClientboundMoveEntityPacket.Pos(
                    sourcePlayer.getEntityId(),
                    packet.getXa(), packet.getYa(), packet.getZa(),
                    packet.isOnGround()
            );
        }
        else if (packetType == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK)
        {
            newPacket = new ClientboundMoveEntityPacket.PosRot(
                    sourcePlayer.getEntityId(),
                    packet.getXa(), packet.getYa(), packet.getZa(),
                    Mth.packDegrees(yaw), Mth.packDegrees(pitch),
                    packet.isOnGround()
            );
        }
        else
        {
            logger.error("Unknown ClientboundMoveEntityPacket: " + packetType);
            return;
        }

        var finalPacket = PacketContainer.fromPacket(newPacket);
        getFactory().markPacketOurs(finalPacket);
        event.setPacket(finalPacket);
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent)
    {
    }

    @Override
    public ListeningWhitelist getSendingWhitelist()
    {
        return ListeningWhitelist.newBuilder()
                .gamePhase(GamePhase.PLAYING)
                .types(PacketType.Play.Server.ENTITY_LOOK,
                        PacketType.Play.Server.ENTITY_HEAD_ROTATION,
                        PacketType.Play.Server.REL_ENTITY_MOVE,
                        PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
                        PacketType.Play.Server.ENTITY_TELEPORT)
                .build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist()
    {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }
}

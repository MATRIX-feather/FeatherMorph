package xyz.nifeather.morph.backends.server.renderer.network.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.GamePhase;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.backends.server.renderer.network.registries.RenderRegistry;
import xyz.nifeather.morph.utilities.EntityTypeUtils;

import java.util.concurrent.atomic.AtomicReference;

public class SoundListener extends ProtocolListener
{
    @Override
    public String getIdentifier()
    {
        return "sound";
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        var packetType = event.getPacketType();
        var packet = event.getPacket();

        //不要处理来自我们自己的包
        if (getFactory().isPacketOurs(packet))
        {
            return;
        }

        if (packetType != PacketType.Play.Server.NAMED_SOUND_EFFECT)
            return;

        //event.getPlayer() 是要发送过去的玩家
        var newHandler = getClientboundSoundPacket(event, packet);
        if (newHandler == null) return;

        event.setPacket(PacketContainer.fromPacket(newHandler));
    }

    @Resolved(shouldSolveImmediately = true)
    private RenderRegistry registry;

    @Nullable
    private ClientboundSoundPacket getClientboundSoundPacket(PacketEvent event, PacketContainer packet)
    {
        var handle = (ClientboundSoundPacket) packet.getHandle();
        var player = event.getPlayer();

        // 根据位置寻找Watcher和玩家
        var playerLocation = player.getLocation();

        int sourceX = (int) (handle.getX() * 8);
        int sourceY = (int) (handle.getY() * 8);
        int sourceZ = (int) (handle.getZ() * 8);

        var watcher = registry.getWatchers().stream().filter(w ->
        {
            if (!w.isActive()) return false;

            var loc = w.getBindingPlayer().getLocation();
            if (!loc.getWorld().equals(playerLocation.getWorld())) return false;

            int locX = (int) (loc.x() * 8);
            int locY = (int) (loc.y() * 8);
            int locZ = (int) (loc.z() * 8);

            return sourceX == locX && sourceY == locY && sourceZ == locZ;
        }).findFirst().orElse(null);

        // 不要处理玩家伪装
        if (watcher == null || watcher.getEntityType() == EntityType.PLAYER) return null;

        // 设置Sound
        var holder = handle.getSound();
        var unwrapOptional = holder.unwrap();

        var right = unwrapOptional.right();
        var left = unwrapOptional.left();

        // 获取正要播放的音效
        AtomicReference<ResourceLocation> resourceLocationRef = new AtomicReference<>(null);
        left.ifPresent(rK -> resourceLocationRef.set(rK.location()));
        right.ifPresent(se -> resourceLocationRef.set(se.location()));

        if (resourceLocationRef.get() == null) return null;
        SoundEvent sound = null;
        var location = resourceLocationRef.get();

        /*if (location.getPath().endsWith(".step"))
        {
            var soundId = EntityTypeUtils.getStepSound(watcher.getEntityType());
            if (soundId == null) return null;

            ResourceLocation rL = ResourceLocation.tryParse(soundId);
            if (rL == null) return null;

            sound = SoundEvent.createFixedRangeEvent(rL, 16);
        }*/

        var path = location.getPath();

        // 如果以".hurt"结尾，那么尝试覆盖此音效
        if (path.endsWith(".hurt")
                || path.endsWith(".hurt_on_fire")
                || path.endsWith(".hurt_drown")
                || path.endsWith(".hurt_freeze")
                || path.endsWith(".hurt_sweet_berry_bush"))
        {
            var soundId = EntityTypeUtils.getDamageSound(watcher.getEntityType());
            if (soundId == null) return null;

            ResourceLocation rL = ResourceLocation.tryParse(soundId);
            if (rL == null) return null;

            var originalEvent = handle.getSound().unwrap().right().orElse(null);

            if (originalEvent != null)
                sound = SoundEvent.createFixedRangeEvent(rL, originalEvent.getRange(handle.getVolume()));
            else
                sound = SoundEvent.createVariableRangeEvent(rL);
        }

        if (sound == null) return null;

        return new ClientboundSoundPacket(
                Holder.direct(sound), handle.getSource(),
                handle.getX(), handle.getY(), handle.getZ(),
                handle.getVolume(), handle.getPitch(), handle.getSeed());
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
                .types(PacketType.Play.Server.NAMED_SOUND_EFFECT)
                .build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist()
    {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }
}

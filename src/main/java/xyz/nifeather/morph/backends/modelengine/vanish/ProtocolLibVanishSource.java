package xyz.nifeather.morph.backends.modelengine.vanish;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.backends.server.renderer.network.PacketFactory;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.PlayerWatcher;
import xyz.nifeather.morph.backends.server.renderer.utilties.WatcherUtils;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.utilities.EntityTypeUtils;

import java.util.List;

public class ProtocolLibVanishSource extends MorphPluginObject implements IVanishSource, PacketListener
{
    public ProtocolLibVanishSource()
    {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    private final List<Player> vanish = new ObjectArrayList<>();

    @Override
    public void vanishPlayer(Player player)
    {
        vanish.add(player);

        var affected = WatcherUtils.getAffectedPlayers(player);
        var packet = new ClientboundRemoveEntitiesPacket(player.getEntityId());
        var container = PacketContainer.fromPacket(packet);

        for (var affectedPlayer : affected)
            ProtocolLibrary.getProtocolManager().sendServerPacket(affectedPlayer, container);
    }

    private final PacketFactory packetFactory = new PacketFactory();

    @Override
    public void cancelVanish(Player player)
    {
        vanish.remove(player);

        var watcher = new PlayerWatcher(player);
        watcher.sync();

        var nmsPlayer = NmsRecord.ofPlayer(player);
        var packet = new ClientboundAddEntityPacket(
                player.getEntityId(), player.getUniqueId(),
                player.getX(), player.getY(), player.getZ(),
                player.getPitch(), player.getYaw(),
                EntityTypeUtils.getNmsType(EntityType.PLAYER), 0,
                nmsPlayer.getDeltaMovement(),
                nmsPlayer.getYHeadRot()
        );

        var container = PacketContainer.fromPacket(packet);

        var manager = ProtocolLibrary.getProtocolManager();
        for (Player affected : WatcherUtils.getAffectedPlayers(player))
        {
            manager.sendServerPacket(affected, container);
        }
    }

    @Override
    public void onPacketSending(PacketEvent packetEvent)
    {
        if (packetEvent.getPacketType() != PacketType.Play.Server.SPAWN_ENTITY) return;

        var packet = (ClientboundAddEntityPacket) packetEvent.getPacket().getHandle();
        if (vanish.stream().anyMatch(p -> packet.getId() == p.getEntityId()))
            packetEvent.setCancelled(true);
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent)
    {
    }

    private final ListeningWhitelist whitelist = ListeningWhitelist.newBuilder()
            .gamePhase(GamePhase.PLAYING)
            .types(PacketType.Play.Server.SPAWN_ENTITY)
            .build();

    @Override
    public ListeningWhitelist getSendingWhitelist()
    {
        return whitelist;
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist()
    {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public org.bukkit.plugin.Plugin getPlugin()
    {
        return FeatherMorphMain.getInstance();
    }
}

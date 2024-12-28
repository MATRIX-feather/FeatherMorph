package xyz.nifeather.morph.backends.server.renderer.network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.plugin.Plugin;
import xiamomc.pluginbase.Annotations.Initializer;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.MorphPluginObject;

public class TestPacketListener extends MorphPluginObject implements PacketListener
{
    @Initializer
    private void load()
    {

    }

    /**
     * Invoked right before a packet is transmitted from the server to the client.
     * <p>
     * Note that the packet may be replaced, if needed.
     * <p>
     * This method is executed on the main server thread by default. However, some spigot forks (like paper) schedule
     * specific packets off the main thread. If the {@link ListenerOptions#ASYNC} option is not specified any invocation
     * of this method will be on the main server thread.
     *
     * @param event - the packet that should be sent.
     */
    @Override
    public void onPacketSending(PacketEvent event)
    {
        var type = event.getPacketType();

        if (type == PacketType.Play.Server.NAMED_SOUND_EFFECT
            || type == PacketType.Play.Server.ENTITY_EFFECT
            || type == PacketType.Play.Server.BLOCK_CHANGE
            || type == PacketType.Play.Server.UPDATE_TIME)
        {
            return;
        }

        if (event.isCancelled())
            return;

        logger.info("SEND! type is '%s' handle is '%s'".formatted(event.getPacketType().name(), event.getPacket().getHandle()));
    }

    /**
     * Invoked right before a received packet from a client is being processed.
     * <p>
     * This method will be called asynchronously (or on the netty event loop) by default. If the
     * {@link ListenerOptions#SYNC} option is specified, the invocation of this method will be synced to the main server
     * thread which might cause issues due to delayed packets.
     *
     * @param event - the packet that has been received.
     */
    @Override
    public void onPacketReceiving(PacketEvent event)
    {
    }

    /**
     * Retrieve which packets sent by the server this listener will observe.
     *
     * @return List of server packets to observe, along with the priority.
     */
    @Override
    public ListeningWhitelist getSendingWhitelist()
    {
        var builder = ListeningWhitelist.newBuilder()
                .gamePhase(GamePhase.PLAYING);

        var types = new ObjectArrayList<PacketType>();

        PacketType.Play.Server.getInstance().forEach(type ->
        {
            logger.info("Registing type " + type);
            types.add(type);
        });

        builder.types(types);

        return builder.build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist()
    {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin()
    {
        return FeatherMorphMain.getInstance();
    }

}

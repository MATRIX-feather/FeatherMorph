package xyz.nifeather.morph.backends.server.renderer.network.listeners;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.backends.server.renderer.network.PacketFactory;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.utilities.NmsUtils;
import xyz.nifeather.morph.utilities.ReflectionUtils;

import java.lang.reflect.Field;

public abstract class ProtocolListener extends MorphPluginObject implements PacketListener
{
    @Resolved(shouldSolveImmediately = true)
    private PacketFactory packetFactory;

    public abstract String getIdentifier();

    protected PacketFactory getFactory() { return packetFactory; }

    protected ProtocolManager protocolManager()
    {
        return ProtocolLibrary.getProtocolManager();
    }

    @Override
    public org.bukkit.plugin.Plugin getPlugin()
    {
        return FeatherMorphMain.getInstance();
    }

    private final Bindable<Boolean> debugOutput = new Bindable<>(false);

    protected boolean isDebugEnabled()
    {
        return debugOutput.get();
    }

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(debugOutput, ConfigOption.DEBUG_OUTPUT);
    }

    protected Player getNmsPlayerEntityFromUnreadablePacket(Packet<?> packet)
    {
        int entityId;

        try
        {
            entityId = ReflectionUtils.getValue(packet, "entityId", int.class);
        }
        catch (Throwable t)
        {
            if (isDebugEnabled())
            {
                logger.error("No field 'entityId' in packet " + packet + "! Skipping: " + t.getMessage());

                logger.info("Valid fields: ");
                for (Field declaredField : packet.getClass().getDeclaredFields())
                {
                    logger.info("  \\--" + declaredField.getName());
                }
            }

            return null;
        }

        return this.getNmsPlayerFrom(entityId);
    }

    @Nullable
    protected Player getNmsPlayerFrom(int id)
    {
        //if (!TickThread.isTickThread())
        //    logger.warn("Not on a tick thread! Caution for exceptions!");

        // Bukkit.getOnlinePlayers() 会将正前往不同维度的玩家从列表里移除
        // 因此我们需要在每个世界都手动查询一遍
        for (var world : Bukkit.getWorlds())
        {
            // For performance, we use NMS instead of CraftWorld
            var nmsWorld = NmsUtils.getNmsLevel(world);
            var worldPlayers = nmsWorld.players();

            var match = worldPlayers.stream()
                    .filter(p -> p.getId() == id)
                    .findFirst()
                    .orElse(null);

            if (match != null)
                return match;
        }

        return null;
    }

    @Nullable
    protected Entity getNmsEntityFrom(PacketEvent event, int id)
    {
        var packetTarget = event.getPlayer();
        var sourceNmsEntity = NmsUtils.getNmsLevel(packetTarget.getWorld()).getEntity(id);
        if (sourceNmsEntity == null)
        {
            if (debugOutput.get())
            {
                logger.warn("A packet from a player that doesn't exist in its world?!");
                logger.warn("Packet: " + event.getPacketType());
            }

            return null;
        }

        return sourceNmsEntity;
    }
}

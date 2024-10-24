package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import com.comphenix.protocol.events.PacketContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.SingleWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.misc.NmsRecord;

import java.util.Optional;

public class EntityWatcher extends SingleWatcher
{
    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.BASE_ENTITY);
    }

    public EntityWatcher(Player bindingPlayer, EntityType entityType)
    {
        super(bindingPlayer, entityType);
    }

    protected byte getPlayerBitMask(Player player)
    {
        byte bitMask = 0x00;
        if (player.getFireTicks() > 0 || player.isVisualFire())
            bitMask |= (byte) 0x01;

        if (player.isSneaking())
            bitMask |= (byte) 0x02;

        if (player.isSprinting())
            bitMask |= (byte) 0x08;

        if (player.isSwimming())
            bitMask |= (byte) 0x10;

        if (player.isInvisible())
            bitMask |= (byte) 0x20;

        if (player.isGlowing())
            bitMask |= (byte) 0x40;

        if (NmsRecord.ofPlayer(player).isFallFlying())
            bitMask |= (byte) 0x80;

        return bitMask;
    }

    @Override
    protected void doSync()
    {
        super.doSync();

        var player = getBindingPlayer();
        var nmsPlayer = NmsRecord.ofPlayer(player);
        var values = ValueIndex.BASE_ENTITY;

        writeTemp(values.GENERAL, getPlayerBitMask(player));
        //write(values.SILENT, true);
        writeTemp(values.NO_GRAVITY, !player.hasGravity());
        writeTemp(values.POSE, nmsPlayer.getPose());
        writeTemp(values.FROZEN_TICKS, nmsPlayer.getTicksFrozen());
    }

    @Override
    protected <X> void onEntryWrite(CustomEntry<X> entry, X oldVal, X newVal)
    {
        super.onEntryWrite(entry, oldVal, newVal);

        if (entry.equals(CustomEntries.DISGUISE_NAME))
        {
            var str = newVal.toString();
            var component = str.isEmpty() ? null : Component.literal(str);
            writePersistent(ValueIndex.BASE_ENTITY.CUSTOM_NAME, component == null ? Optional.empty() : Optional.of(component));
        }

        if (entry.equals(CustomEntries.VANISHED))
        {
            var packet = new ClientboundRemoveEntitiesPacket(this.readEntryOrThrow(CustomEntries.SPAWN_ID));
            this.sendPacketToAffectedPlayers(PacketContainer.fromPacket(packet));
        }
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("CustomName"))
        {
            var name = nbt.getString("CustomName");
            var component = Component.Serializer.fromJsonLenient(name, MinecraftServer.getServer().registryAccess());

            if (component != null)
                writePersistent(ValueIndex.BASE_ENTITY.CUSTOM_NAME, Optional.of(component));
        }

        if (nbt.contains("CustomNameVisible"))
        {
            var visible = nbt.getBoolean("CustomNameVisible");
            writePersistent(ValueIndex.BASE_ENTITY.CUSTOM_NAME_VISIBLE, visible);
        }
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        var customName = read(ValueIndex.BASE_ENTITY.CUSTOM_NAME);
        customName.ifPresent(c -> nbt.putString("CustomName", Component.Serializer.toJson(c, MinecraftServer.getServer().registryAccess())));

        nbt.putBoolean("CustomNameVisible", read(ValueIndex.BASE_ENTITY.CUSTOM_NAME_VISIBLE));
    }
}

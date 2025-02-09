package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import org.bukkit.Color;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.joml.Vector3i;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.misc.NmsRecord;

import java.util.List;
import java.util.Optional;

public class LivingEntityWatcher extends EntityWatcher
{
    public LivingEntityWatcher(Player bindingPlayer, EntityType entityType)
    {
        super(bindingPlayer, entityType);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.BASE_LIVING);
    }

    private final Pair<Player, EquipmentSlot> handPair = new ObjectObjectMutablePair<>(null, null);

    public void onPlayerStartUsingItem(PlayerInteractEvent e)
    {
        if (!this.isPlayerOnline()) return;
        if (!this.getBindingPlayer().equals(e.getPlayer())) return;

        handPair.left(e.getPlayer());
        handPair.right(e.getHand());
    }

    @Override
    protected void doSync()
    {
        super.doSync();

        var player = getBindingPlayer();
        var nmsPlayer = NmsRecord.ofPlayer(player);
        var values = ValueIndex.BASE_LIVING;

        writeTemp(values.HEALTH, (float)player.getHealth());

        var flagBit = 0x00;

        if (nmsPlayer.isUsingItem())
        {
            flagBit |= 0x01;

            var handInUse = handPair.right();

            if (handInUse == null)
            {
                var nmsHand = nmsPlayer.getUsedItemHand();
                handInUse = nmsHand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
            }

            boolean isOffhand = handInUse == EquipmentSlot.OFF_HAND;
            if (isOffhand) flagBit |= 0x02;
        }

        if (player.isRiptiding())
            flagBit |= 0x04;

        writeTemp(values.LIVING_FLAGS, (byte)flagBit);

        List<Color> colors = new ObjectArrayList<>();
        boolean hasAmbient = false;
        for (PotionEffect effect : player.getActivePotionEffects())
        {
            if (effect.hasParticles())
                colors.add(effect.getType().getColor());

            hasAmbient = hasAmbient || effect.isAmbient();
        }

        var colorList = new ObjectArrayList<ParticleOptions>();
        for (var color : colors)
            colorList.add(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color.asRGB()));

        writeTemp(values.POTION_COLOR, colorList);
        writeTemp(values.POTION_ISAMBIENT, hasAmbient);

        writeTemp(values.STUCKED_ARROWS, player.getArrowsInBody());
        writeTemp(values.BEE_STINGERS, player.getBeeStingersInBody());

        Optional<Vector3i> bedPos = Optional.empty();
        if (player.isSleeping())
        {
            var bukkitPos = player.getBedLocation();
            bedPos = Optional.of(
                    new Vector3i(bukkitPos.blockX(), bukkitPos.blockY(), bukkitPos.blockZ())
            );
        }

        writeTemp(values.BED_POS, bedPos);
    }
}

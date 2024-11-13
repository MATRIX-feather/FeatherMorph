package xyz.nifeather.morph.misc;

import net.kyori.adventure.sound.Sound;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.utilities.EntityTypeUtils;
import xyz.nifeather.morph.utilities.MathUtils;
import xyz.nifeather.morph.utilities.SoundUtils;

import java.util.Random;

public class SoundHandler
{
    public int ambientInterval = 0;
    public Sound ambientSoundPrimary;
    public Sound ambientSoundSecondary;
    private int soundTime;
    private double soundFrequency = 0D;
    private float soundVolume = 1f;

    public void resetSoundTime()
    {
        soundTime = 0;
    }

    private final Player bindingPlayer;

    @Nullable
    private EntityType entityType;

    private SoundCategory soundCategory = SoundCategory.PLAYERS;

    @NotNull
    private EntityType getEntityType()
    {
        return entityType == null ? EntityType.PLAYER : entityType;
    }

    public SoundHandler(Player bindingPlayer)
    {
        this.bindingPlayer = bindingPlayer;
    }

    public void update()
    {
        soundTime++;

        // Java中浮点数除以0是正或负无穷
        // 因为soundFrequency永远大于等于0，而分子是1，因此frequencyScale的最大值是正无穷
        // 除非soundTime最后也加到了大于等于正无穷，否则不需要额外的判断，但这真的会发生吗（
        double frequencyScale = 1.0D / soundFrequency;

        //logger.info("Sound: %s <-- %s(%s) --> %s".formatted(soundTime, frequency, soundFrequency, ambientInterval * frequency));
        if (ambientInterval != 0 && soundTime >= ambientInterval * frequencyScale && !bindingPlayer.isSneaking())
        {
            boolean playSecondary = false;

            if (getEntityType() == EntityType.ALLAY)
            {
                var eq = bindingPlayer.getEquipment();
                if (!eq.getItemInMainHand().getType().isAir()) playSecondary = true;
            }

            Sound sound = playSecondary ? ambientSoundSecondary : ambientSoundPrimary;

            var nmsPlayer = NmsRecord.ofPlayer(bindingPlayer);
            var isSpectator = nmsPlayer.isSpectator();

            // 和原版行为保持一致, 并且不要为旁观者播放音效:
            // net.minecraft.world.entity.Mob#baseTick()
            if (isSpectator)
            {
                soundTime = -(int)(ambientInterval * frequencyScale);
            }
            else if (sound != null && random.nextInt((int)(1000 * frequencyScale)) < soundTime)
            {
                soundTime = -(int)(ambientInterval * frequencyScale);
                bindingPlayer.getWorld().playSound(
                        bindingPlayer.getLocation(),
                        sound.name().asString(),
                        soundCategory,
                        soundVolume,
                        1f);
            }
        }
    }

    private final Random random = new Random();

    private final MorphConfigManager config = MorphConfigManager.getInstance();

    public void resetSound()
    {
        ambientSoundPrimary = null;
        ambientSoundSecondary = null;
        ambientInterval = 0;
        resetSoundTime();
    }

    public void refreshSounds(EntityType entityType, boolean isBaby)
    {
        resetSound();

        this.entityType = entityType;

        soundFrequency = MathUtils.clamp(0, 2, config.getBindable(Double.class, ConfigOption.AMBIENT_FREQUENCY).get());

        var soundEvent = EntityTypeUtils.getAmbientSound(entityType, this.bindingPlayer.getWorld(), this.bindingPlayer.getLocation());

        var sound = soundEvent.sound();
        if (sound == null) return;

        this.soundVolume = soundEvent.volume();
        this.ambientInterval = soundEvent.interval();
        var pitch = isBaby ? 1.5F : 1F;

        this.ambientSoundPrimary = SoundUtils.toBukkitSound(soundEvent, pitch);

        var isEnemy = EntityTypeUtils.isEnemy(entityType);
        this.soundCategory = isEnemy ? SoundCategory.HOSTILE : SoundCategory.NEUTRAL;

        if (entityType == EntityType.ALLAY)
        {
            var allaySecondary = SoundEvents.ALLAY_AMBIENT_WITH_ITEM;
            var secSi = new EntityTypeUtils.SoundInfo(allaySecondary, SoundSource.NEUTRAL, ambientInterval, soundEvent.volume());
            this.ambientSoundSecondary = SoundUtils.toBukkitSound(secSi, pitch);
        }
    }
}


package xyz.nifeather.morph.utilities;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minecraft.sounds.SoundSource;

public class SoundUtils
{
    @SuppressWarnings("PatternValidation")
    public static Sound toBukkitSound(EntityTypeUtils.SoundInfo si, float pitch)
    {
        if (si.sound() == null)
            return null;

        var resLoc = si.sound().getLocation();

        return Sound.sound().source(toAdventureSource(si.source())).volume(si.volume()).pitch(pitch)
                .type(Key.key(resLoc.getNamespace(), resLoc.getPath())).build();
    }

    public static Sound.Source toAdventureSource(SoundSource nmsSource)
    {
        return switch (nmsSource)
        {
            case MASTER -> Sound.Source.MASTER;
            case MUSIC -> Sound.Source.MUSIC;
            case RECORDS -> Sound.Source.RECORD;
            case WEATHER -> Sound.Source.WEATHER;
            case BLOCKS -> Sound.Source.BLOCK;
            case HOSTILE -> Sound.Source.HOSTILE;
            case NEUTRAL -> Sound.Source.NEUTRAL;
            case PLAYERS -> Sound.Source.PLAYER;
            case AMBIENT -> Sound.Source.AMBIENT;
            case VOICE -> Sound.Source.VOICE;
        };
    }
}

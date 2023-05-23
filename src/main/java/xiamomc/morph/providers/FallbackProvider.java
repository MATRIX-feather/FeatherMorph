package xiamomc.morph.providers;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.backends.DisguiseWrapper;
import xiamomc.morph.misc.DisguiseInfo;
import xiamomc.morph.misc.DisguiseState;

import java.util.List;

public class FallbackProvider extends DefaultDisguiseProvider
{
    @Override
    public @NotNull String getNameSpace()
    {
        return "fallback";
    }

    @Override
    public boolean isValid(String rawIdentifier)
    {
        return false;
    }

    @Override
    public List<String> getAllAvailableDisguises()
    {
        return List.of();
    }

    @Override
    public @NotNull DisguiseResult makeWrapper(Player player, DisguiseInfo disguiseInfo, @Nullable Entity targetEntity)
    {
        return DisguiseResult.fail();
    }

    @Override
    public boolean canConstruct(DisguiseInfo info, Entity targetEntity, @Nullable DisguiseState theirState)
    {
        return false;
    }

    @Override
    protected boolean canCloneDisguise(DisguiseInfo info, Entity targetEntity, @NotNull DisguiseState theirState, @NotNull DisguiseWrapper<?> theirDisguise)
    {
        return false;
    }

    @Override
    public Component getDisplayName(String disguiseIdentifier, String locale)
    {
        return Component.text("???");
    }
}

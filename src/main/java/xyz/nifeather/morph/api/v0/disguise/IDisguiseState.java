package xyz.nifeather.morph.api.v0.disguise;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Exceptions.NullDependencyException;

public interface IDisguiseState
{
    @Nullable
    Player tryGetPlayer();

    /**
     *
     * @apiNote For a nullable method, use {@link IDisguiseState#tryGetPlayer()}
     * @throws NullDependencyException If the player doesn't exist.
     */
    @NotNull
    Player getPlayer() throws NullDependencyException;

    boolean isSelfViewing();

    /**
     * @return The display component used for the player
     */
    Component getPlayerDisplay();
    void setPlayerDisplay(@NotNull Component newName);

    /**
     * @return The display component used for others, like Bossbar, ChatOverride, and PAPI integration
     */
    Component getServerDisplay();
    void setServerDisplay(@NotNull Component newName);

    /**
     * Sets both the player and the server display to the given component
     */
    void setCustomDisplayName(Component newName);
}

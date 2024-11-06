package xyz.nifeather.morph.api.v0.disguise;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.providers.disguise.DisguiseProvider;

import java.util.List;
import java.util.UUID;

public interface IMorphManager
{
    boolean canMorph(UUID playerUUID);

    void updateLastPlayerMorphOperationTime(UUID uuid);

    List<String> getBannedDisguisesCopy();

    boolean disguiseDisabled(String identifier);

    @NotNull
    DisguiseProvider getDisguiseProvider(String namespaceIdentifier);
    boolean registerDisguiseProvider(DisguiseProvider provider);

    boolean tryQuickDisguise(Player player);

    boolean morph(MorphParameters parameters);

    void unMorph(Player player);
    void unMorph(Player player, boolean ignorePermissions);
    void unMorph(@Nullable CommandSender source, Player player, boolean bypassPermission, boolean forceUnmorph);

    /**
     * @return See {@link DisguiseValidateResult}
     */
    int validateDisguise(String identifier);

    boolean clientViewAvailable(Player player);

    void setSelfDisguiseVisible(Player player, boolean value, boolean saveToConfig, boolean dontSetServerSide, boolean noClientCommand);
    void setSelfDisguiseVisible(Player player, boolean value, boolean saveToConfig);

    IDisguiseState getDisguiseStateFor(Player player);

    boolean grantMorphToPlayer(Player player, String disguiseIdentifier);

    boolean revokeMorphFromPlayer(Player player, String disguiseIdentifier);
}

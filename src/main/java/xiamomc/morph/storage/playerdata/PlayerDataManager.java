package xiamomc.morph.storage.playerdata;

import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.MorphManager;
import xiamomc.morph.interfaces.IManagePlayerData;
import xiamomc.morph.misc.DisguiseInfo;
import xiamomc.morph.misc.DisguiseState;
import xiamomc.morph.messages.MessageUtils;
import xiamomc.morph.storage.JsonBasedStorage;
import xiamomc.pluginbase.Annotations.Resolved;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerDataManager extends JsonBasedStorage<MorphConfiguration> implements IManagePlayerData
{
    private final List<DisguiseInfo> cachedInfos = new ArrayList<>();

    @Resolved
    private MorphManager morphs;

    @Override
    protected @NotNull String getFileName()
    {
        return "data.json";
    }

    @Override
    protected @NotNull MorphConfiguration createDefault()
    {
        return new MorphConfiguration();
    }

    @Override
    protected @NotNull String getDisplayName()
    {
        return "数据存储";
    }

    //region Implementation of IManagePlayerData

    @Override
    public boolean reloadConfiguration()
    {
        var success = super.reloadConfiguration();

        if (success)
        {
            if (storingObject.Version < targetConfigurationVersion)
                migrate(storingObject);

            saveConfiguration();
        }

        return success;
    }

    private final int targetConfigurationVersion = 2;

    private void migrate(MorphConfiguration configuration)
    {
        configuration.Version = targetConfigurationVersion;

        configuration.playerMorphConfigurations.forEach(c ->
        {
            if (Objects.equals(c.playerName, "Unknown")) c.playerName = null;
        });
    }

    @Override
    public PlayerMorphConfiguration getPlayerConfiguration(Player player)
    {
        var valueOptional = storingObject.playerMorphConfigurations
                .stream().filter(c -> c.uniqueId.equals(player.getUniqueId())).findFirst();

        if (valueOptional.isPresent())
        {
            var value = valueOptional.get();
            if (value.playerName == null) value.playerName = player.getName();

            return value;
        }
        else
        {
            var newInstance = new PlayerMorphConfiguration();
            newInstance.uniqueId = player.getUniqueId();
            newInstance.playerName = player.getName();
            newInstance.unlockedDisguises = new ArrayList<>();

            var msg = Component.text("不知道如何使用伪装? 发送 /mmorph help 即可查看！");
            player.sendMessage(MessageUtils.prefixes(player, msg));

            storingObject.playerMorphConfigurations.add(newInstance);
            return newInstance;
        }
    }

    @Override
    public boolean grantMorphToPlayer(Player player, EntityType type)
    {
        var playerConfiguration = getPlayerConfiguration(player);
        var info = getDisguiseInfo(type);

        if (playerConfiguration.unlockedDisguises.stream().noneMatch(c -> c.equals(info)))
        {
            playerConfiguration.unlockedDisguises.add(info);
            saveConfiguration();
        }
        else return false;

        sendMorphAcquiredNotification(player, morphs.getDisguiseStateFor(player),
                Component.text("✔ 已解锁")
                        .append(Component.translatable(type.translationKey()))
                        .append(Component.text("的伪装")).color(NamedTextColor.GREEN));

        return true;
    }

    @Override
    public boolean grantPlayerMorphToPlayer(Player sourcePlayer, String targetPlayerName)
    {
        var playerConfiguration = getPlayerConfiguration(sourcePlayer);

        if (playerConfiguration.unlockedDisguises.stream().noneMatch(c -> c.equals(targetPlayerName)))
            playerConfiguration.unlockedDisguises.add(this.getDisguiseInfo(targetPlayerName));
        else
            return false;

        saveConfiguration();

        sendMorphAcquiredNotification(sourcePlayer, morphs.getDisguiseStateFor(sourcePlayer),
                Component.text("✔ 已解锁" + targetPlayerName + "的伪装").color(NamedTextColor.GREEN));
        return true;
    }

    @Override
    public boolean revokeMorphFromPlayer(Player player, EntityType entityType)
    {
        var avaliableDisguises = getAvaliableDisguisesFor(player);

        var optional = avaliableDisguises.stream().filter(d -> d.type == entityType).findFirst();
        if (optional.isEmpty()) return false;

        getPlayerConfiguration(player).unlockedDisguises.remove(optional.get());
        saveConfiguration();

        var state = morphs.getDisguiseStateFor(player);
        if (state != null && state.getDisguise().getType().getEntityType().equals(entityType))
            morphs.unMorph(player);

        sendMorphAcquiredNotification(player, state,
                Component.text("❌ 已失去")
                        .append(Component.translatable(entityType.translationKey()))
                        .append(Component.text("的伪装")).color(NamedTextColor.RED));
        return true;
    }

    @Override
    public boolean revokePlayerMorphFromPlayer(Player player, String playerName)
    {
        var avaliableDisguises = getAvaliableDisguisesFor(player);

        var optional = avaliableDisguises.stream()
                .filter(d -> (d.isPlayerDisguise() && Objects.equals(d.playerDisguiseTargetName, playerName))).findFirst();

        if (optional.isEmpty()) return false;

        getPlayerConfiguration(player).unlockedDisguises.remove(optional.get());
        saveConfiguration();

        var state = morphs.getDisguiseStateFor(player);

        if (state != null
                && state.getDisguise().isPlayerDisguise()
                && ((PlayerDisguise)state.getDisguise()).getName().equals(playerName))
        {
            morphs.unMorph(player);
        }

        sendMorphAcquiredNotification(player, state,
                Component.text("❌ 已失去" + playerName + "的伪装").color(NamedTextColor.RED));

        return true;
    }

    @Override
    public DisguiseInfo getDisguiseInfo(EntityType type)
    {
        if (type.equals(EntityType.PLAYER)) throw new IllegalArgumentException("玩家不能作为类型传入");

        if (this.cachedInfos.stream().noneMatch(o -> o.equals(type)))
            cachedInfos.add(new DisguiseInfo(type));

        return cachedInfos.stream().filter(o -> o.equals(type)).findFirst().get();
    }

    @Override
    public DisguiseInfo getDisguiseInfo(String playerName)
    {
        if (this.cachedInfos.stream().noneMatch(o -> o.equals(playerName)))
            cachedInfos.add(new DisguiseInfo(playerName));

        return cachedInfos.stream().filter(o -> o.equals(playerName)).findFirst().get();
    }

    @Override
    public ArrayList<DisguiseInfo> getAvaliableDisguisesFor(Player player)
    {
        return new ArrayList<>(getPlayerConfiguration(player).unlockedDisguises);
    }

    //endregion Implementation of IManagePlayerData

    private void sendMorphAcquiredNotification(Player player, @Nullable DisguiseState state, Component text)
    {
        if (state == null)
            player.sendActionBar(text);
        else
            player.sendMessage(MessageUtils.prefixes(player, text));
    }
}

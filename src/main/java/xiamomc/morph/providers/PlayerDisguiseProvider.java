package xiamomc.morph.providers;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.backends.DisguiseWrapper;
import xiamomc.morph.messages.MessageUtils;
import xiamomc.morph.messages.MorphStrings;
import xiamomc.morph.misc.DisguiseInfo;
import xiamomc.morph.misc.DisguiseState;
import xiamomc.morph.misc.DisguiseTypes;
import xiamomc.morph.misc.MorphGameProfile;

import java.util.List;

public class PlayerDisguiseProvider extends DefaultDisguiseProvider
{
    @Override
    public @NotNull String getNameSpace()
    {
        return DisguiseTypes.PLAYER.getNameSpace();
    }

    @Override
    public boolean isValid(String rawIdentifier)
    {
        return DisguiseTypes.fromId(rawIdentifier) == DisguiseTypes.PLAYER;
    }

    @Override
    public @NotNull DisguiseResult makeWrapper(Player player, DisguiseInfo disguiseInfo, @Nullable Entity targetEntity)
    {
        if (getMorphManager().getBannedDisguises().contains("minecraft:player"))
        {
            player.sendMessage(MessageUtils.prefixes(player, MorphStrings.disguiseBannedOrNotSupportedString()));
            return DisguiseResult.fail();
        }

        var id = disguiseInfo.getIdentifier();
        var backend = getBackend();

        if (DisguiseTypes.fromId(id) != DisguiseTypes.PLAYER)
            return DisguiseResult.fail();

        var result = constructFromEntity(disguiseInfo, targetEntity);
        var disguise = result.success()
                ? result.wrapperInstance()
                : backend.createPlayerInstance(disguiseInfo.playerDisguiseTargetName);

        var mainHandItem = player.getEquipment().getItemInMainHand();

        //存在玩家头颅，尝试通过头颅获取目标皮肤
        if (mainHandItem.getType() == Material.PLAYER_HEAD)
        {
            var gameProfile = getGameProfile(mainHandItem);

            if (gameProfile == null)
            {
                player.sendMessage(MessageUtils.prefixes(player, MorphStrings.invalidSkinString()));
                return DisguiseResult.fail();
            }

            //如果玩家头和目标伪装ID一致，那么设置伪装皮肤
            if (gameProfile.getName().equals(DisguiseTypes.PLAYER.toStrippedId(id)))
                disguise.applySkin(gameProfile);
        }

        return DisguiseResult.success(disguise, result.isCopy());
    }

    @Override
    public void postConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
        super.postConstructDisguise(state, targetEntity);

        var profile = state.getDisguiseWrapper().getSkin();

        if (profile != null)
        {
            var gameProfile = new MorphGameProfile(profile);
            var compound = xiamomc.morph.utilities.NbtUtils.toCompoundTag(state.getCachedNbtString(), false);

            gameProfile.setName(DisguiseTypes.PLAYER.toStrippedId(state.getDisguiseIdentifier()));

            NbtUtils.writeGameProfile(compound, gameProfile);
            state.setCachedProfileNbtString(compound.toString());
        }
    }

    private MorphGameProfile getGameProfile(ItemStack item)
    {
        if (item.getType() != Material.PLAYER_HEAD) return null;

        var profile = ((SkullMeta) item.getItemMeta()).getPlayerProfile();
        if (profile == null) return null;

        return new MorphGameProfile(profile);
    }

    @Override
    public List<String> getAllAvailableDisguises()
    {
        var onlinePlayers = Bukkit.getOnlinePlayers();

        var list = new ObjectArrayList<String>();
        onlinePlayers.forEach(p -> list.add(DisguiseTypes.PLAYER.toId(p.getName())));

        return list;
    }

    @Override
    public boolean canConstruct(DisguiseInfo info, Entity targetEntity, @Nullable DisguiseState theirState)
    {
        if (theirState != null)
        {
            var type = DisguiseTypes.fromId(theirState.getDisguiseIdentifier());

            return type == DisguiseTypes.PLAYER
                    && type.toStrippedId(theirState.getDisguiseIdentifier()).equals(info.playerDisguiseTargetName);
        }

        if (!(targetEntity instanceof Player targetPlayer))
            return false;

        return targetPlayer.getName().equals(info.playerDisguiseTargetName);
    }

    @Override
    protected boolean canCloneDisguise(DisguiseInfo info, Entity targetEntity,
                                       @NotNull DisguiseState theirState, @NotNull DisguiseWrapper<?> theirDisguise)
    {
        return theirDisguise.getDisguiseName().equals(info.playerDisguiseTargetName) && theirDisguise.isPlayerDisguise();
    }

    @Override
    public boolean validForClient(DisguiseState state)
    {
        return true;
    }

    @Override
    public @Nullable CompoundTag getNbtCompound(DisguiseState state, Entity targetEntity)
    {
        if (!(targetEntity instanceof Player targetPlayer)) return null;

        if (!targetPlayer.getName().equals(DisguiseTypes.PLAYER.toStrippedId(state.getDisguiseIdentifier()))) return null;

        return super.getNbtCompound(state, targetEntity);
    }

    @Override
    public boolean unMorph(Player player, DisguiseState state)
    {
        return super.unMorph(player, state);
    }

    @Override
    public Component getDisplayName(String disguiseIdentifier, String locale)
    {
        //尝试获取玩家的显示名称
        Component finalName;
        var playerName = DisguiseTypes.PLAYER.toStrippedId(disguiseIdentifier);
        var player = Bukkit.getPlayerExact(playerName);

        if (player != null)
            finalName = player.displayName();
        else
            finalName = Component.text(playerName);

        return finalName;
    }
}

package xyz.nifeather.morph.abilities.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.abilities.AbilityType;
import xyz.nifeather.morph.abilities.MorphAbility;
import xyz.nifeather.morph.abilities.options.BossbarOption;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.utilities.DisguiseUtils;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.List;

public class BossbarAbility extends MorphAbility<BossbarOption>
{
    private final Bindable<Boolean> allowBossbar = new Bindable<>(false);

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(allowBossbar, ConfigOption.DISPLAY_BOSSBAR);
    }

    @Override
    public @NotNull NamespacedKey getIdentifier()
    {
        return AbilityType.BOSSBAR;
    }

    @Override
    protected @NotNull BossbarOption createOption()
    {
        return new BossbarOption();
    }

    @Override
    public boolean revokeFromPlayer(Player player, DisguiseState state)
    {
        if (super.revokeFromPlayer(player, state))
        {
            state.setBossbar(null);
            return true;
        }

        return false;
    }

    @Override
    public boolean applyToPlayer(Player player, DisguiseState state)
    {
        if (super.applyToPlayer(player, state))
        {
            if (!allowBossbar.get()) return true;

            var option = this.getOptionFor(state);

            if (option == null) return false;

            var createOption = option.getCreateOption();

            state.setBossbar(BossBar.bossBar(
                    this.getBossbarName(state, option),
                    1f,
                    createOption.color(),
                    createOption.overlay(),
                    createOption.flags()
            ));
        }

        return false;
    }

    @Override
    public boolean handle(Player player, DisguiseState state)
    {
        if (!super.handle(player, state)) return false;

        if (!this.isPlayerApplied(player) || plugin.getCurrentTick() % 4 != 0)
            return true;

        var option = this.getOptionFor(state);

        if (option == null) return false;

        var bossbar = state.getBossbar();
        if (bossbar != null)
        {
            var distance = option.getApplyDistance();

            if (distance < 0)
                distance = (Bukkit.getViewDistance() - 1) * 16;

            var playerGameMode = player.getGameMode();
            List<Player> playersToShow = DisguiseUtils.findNearbyPlayers(player, distance, true);
            List<Player> playersToHide = new ObjectArrayList<>(Bukkit.getOnlinePlayers());

            if (playerGameMode == GameMode.SPECTATOR)
                playersToShow.removeIf(p -> p.getGameMode() != playerGameMode);

            bossbar.progress((float) (player.getHealth() / player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
            //bossbar.name(this.getBossbarName(state, option));

            if (state.canDisplayBossbar())
            {
                playersToHide.removeAll(playersToShow);
                playersToHide.remove(player);
            }
            else
            {
                playersToShow.clear();
            }

            playersToShow.forEach(p -> p.showBossBar(bossbar));
            playersToHide.forEach(p -> p.hideBossBar(bossbar));
        }

        return true;
    }

    private Component getBossbarName(DisguiseState state, BossbarOption option)
    {
        return MiniMessage.miniMessage().deserialize(option.getCreateOption().name(),
                Placeholder.component("name", state.getServerDisplay()),
                Placeholder.component("who", state.getPlayer().displayName()));
    }
}

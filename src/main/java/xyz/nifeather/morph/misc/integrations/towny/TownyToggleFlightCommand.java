package xyz.nifeather.morph.misc.integrations.towny;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.messages.CommandNameStrings;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.CommonStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

public class TownyToggleFlightCommand extends BrigadierCommand
{
    private final TownyAdapter adapter;

    public TownyToggleFlightCommand(TownyAdapter adapter)
    {
        this.adapter = adapter;
    }

    @Override
    public String getPermissionRequirement()
    {
        return CommonPermissions.TOGGLE_TOWN_FLIGHT;
    }

    @Override
    public @NotNull String name()
    {
        return "toggle-town-morph-flight";
    }

    @Override
    public boolean register(Commands dispatcher)
    {
        dispatcher.register(
                Commands.literal(name())
                        .executes(this::execNoArg)
                        .then(Commands.argument("switch", BoolArgumentType.bool())
                                .executes(this::execWithArg))
                        .build()
        );

        return true;
    }

    private int execWithArg(CommandContext<CommandSourceStack> context)
    {
        if (!(context.getSource().getExecutor() instanceof Player player))
            return 0;

        var bool = BoolArgumentType.getBool(context, "switch");

        this.toggle(
                context.getSource().getSender(),
                player,
                bool
        );

        return 1;
    }

    private int execNoArg(CommandContext<CommandSourceStack> context)
    {
        if (!(context.getSource().getExecutor() instanceof Player player))
            return 0;

        this.toggle(
                context.getSource().getSender(),
                player,
                null
        );

        return 1;
    }

    private void toggle(CommandSender sender, Player player, @Nullable Boolean input)
    {
        var towny = TownyAPI.getInstance();
        var town = towny.getTown(player);
        var resident = towny.getResident(player);

        if (town == null)
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.townyDoesntHaveTown()));
            return;
        }

        if (resident == null)
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.unknownError()));
            return;
        }

        if (!town.isMayor(resident))
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.townyPlayerNotMayor()));
            return;
        }

        var playerLocale = MessageUtils.getLocale(sender);
        var message = CommandStrings.optionValueString()
                .resolve("what", CommandNameStrings.morphFlightForTownX()
                        .withLocale(playerLocale)
                        .resolve("which", town.getName()));

        boolean allow;

        if (input == null)
        {
            boolean currentAllow = true;

            var bdf = TownyAdapter.allowMorphFlight;
            if (MetaDataUtil.hasMeta(town, bdf))
                currentAllow = !MetaDataUtil.getBoolean(town, bdf);

            allow = currentAllow;
        }
        else
        {
            allow = input;
        }

        setTownFlightStatus(town, allow);

        message.resolve("value", (allow ? CommonStrings.on() : CommonStrings.off()).withLocale(playerLocale));

        sender.sendMessage(MessageUtils.prefixes(sender, message));

    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return new FormattableMessage(plugin, "Toggle town flight");
    }

    private void setTownFlightStatus(Town town, boolean newStatus)
    {
        MetaDataUtil.setBoolean(town, TownyAdapter.allowMorphFlight, newStatus, true);

        refreshPlayersIn(town);
    }

    private void refreshPlayersIn(Town town)
    {
        // Towny没有API来告诉我们一个Town里进了多少玩家
        // 因此我们只能遍历所有玩家实例
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            // 获取玩家爱所在的Town
            var currentTown = TownyAPI.getInstance().getTown(player.getLocation());

            // 在野外或者不是目标town
            if (currentTown == null || currentTown != town) return;

            adapter.updatePlayer(player, currentTown);
        });
    }
}

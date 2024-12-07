package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.util.List;

public class LookupSubCommand extends BrigadierCommand
{
    @Override
    public @NotNull String name()
    {
        return "lookup";
    }

    @Override
    public @Nullable String getPermissionRequirement()
    {
        return CommonPermissions.LOOKUP;
    }

    /**
     * 获取此指令的帮助信息
     *
     * @return 帮助信息
     */
    @Override
    public FormattableMessage getHelpMessage()
    {
        return new FormattableMessage(plugin, "lookup");
    }

    @Resolved(shouldSolveImmediately = true)
    private MorphManager manager;

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal("lookup")
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who", StringArgumentType.string())
                                        .executes(this::execWithName)
                                        .then(
                                                Commands.argument("filter", StringArgumentType.string())
                                                        .executes(this::execWithNameFilter)
                                        )
                        )
        );

        super.registerAsChild(parentBuilder);
    }

    private int execWithName(CommandContext<CommandSourceStack> context)
    {
        var targetLookupName = StringArgumentType.getString(context, "who");
        LookupSubCommand.this.doLookup(context.getSource().getSender(), targetLookupName, null);

        return 1;
    }

    private int execWithNameFilter(CommandContext<CommandSourceStack> context)
    {
        var targetLookupName = StringArgumentType.getString(context, "who");
        var filterName = StringArgumentType.getString(context, "filter");
        LookupSubCommand.this.doLookup(context.getSource().getSender(), targetLookupName, filterName);

        return 1;
    }

    private void doLookup(CommandSender sender, String who, @Nullable String filterName)
    {
        var offlinePlayer = Bukkit.getOfflinePlayer(who);
        var configuration = manager.getPlayerMeta(offlinePlayer);

        List<String> matches;

        //filter keys
        if (filterName != null)
        {
            matches = configuration.getUnlockedDisguiseIdentifiers()
                    .stream().filter(k -> k.toUpperCase().contains(filterName.toUpperCase()))
                    .toList();
        }
        else
        {
            matches = configuration.getUnlockedDisguiseIdentifiers().clone();
        }

        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.lookupFilterCommand()));
        matches.forEach(m -> sender.sendMessage(MessageUtils.prefixes(sender, m)));
    }
}

package xyz.nifeather.morph.commands.subcommands.plugin.management;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

public class ForceUnmorphSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "unmorph";
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.MANAGE_UNMORPH_DISGUISE;
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who",  ArgumentTypes.players())
                                        .executes(this::execute)
                        )
        );
    }

    private int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        var players = context.getArgument("who", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource());

        var sender = context.getSource().getSender();

        players.forEach(player ->
        {
            if (!player.isOnline())
                return;

            manager.unMorph(sender, player, true, true);

            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.unMorphedSomeoneString()
                    .resolve("who", player.getName())));

        });

        return 1;
    }

    @Resolved
    private MorphManager manager;

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.manageUnmorphDescription();
    }
}

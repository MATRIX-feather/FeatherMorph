package xyz.nifeather.morph.commands.subcommands.plugin.management;

import com.mojang.brigadier.arguments.StringArgumentType;
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
import xyz.nifeather.morph.commands.brigadier.arguments.DisguiseIdentifierArgumentType;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.misc.MorphParameters;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

public class ForceMorphSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "morph";
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.MANAGE_MORPH_DISGUISE;
    }

    @Resolved
    private MorphManager manager;

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who", ArgumentTypes.players())
                                        .then(
                                                Commands.argument("as_what", DisguiseIdentifierArgumentType.ALL_AVAILABLE)
                                                        .executes(this::execute)
                                        )
                        )
        );
    }

    private int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        var players = context.getArgument("who", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource());

        players.forEach(who ->
        {
            var targetName = StringArgumentType.getString(context, "as_what");

            var commandSender = context.getSource().getSender();
            if (!who.isOnline())
                return;

            var parameters = MorphParameters
                    .create(who, targetName)
                    .setSource(commandSender)
                    //.setTargetedEntity(who.getTargetEntity(3))
                    .setForceExecute(true)
                    .setBypassAvailableCheck(true)
                    .setBypassPermission(true);

            manager.morph(parameters);
        });

        return 1;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.manageUnmorphDescription();
    }
}

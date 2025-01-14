package xyz.nifeather.morph.commands.subcommands.plugin.management;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.brigadier.arguments.DisguiseIdentifierArgumentType;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.CommonStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

public class RevokeDisguiseSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "revoke";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.manageRevokeDescription();
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.MANAGE_REVOKE_DISGUISE;
    }

    @Resolved
    private MorphManager morphs;

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who", ArgumentTypes.players())
                                        .then(
                                                Commands.argument("id", DisguiseIdentifierArgumentType.ALL_AVAILABLE)
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
            var targetName = StringArgumentType.getString(context, "id");

            var commandSender = context.getSource().getSender();
            if (!who.isOnline())
            {
                commandSender.sendMessage(MessageUtils.prefixes(commandSender, CommonStrings.playerNotFoundString()));
                return;
            }

            if (!targetName.contains(":"))
                targetName = "minecraft:" + targetName;

            String finalTargetName = targetName;
            var info = morphs.getAvaliableDisguisesFor(who)
                    .stream().filter(i -> i.getKey().equals(finalTargetName)).findFirst().orElse(null);

            var revokeSuccess = info != null && morphs.revokeMorphFromPlayer(who, info.getKey());

            var msg = revokeSuccess
                    ? CommandStrings.revokeSuccessString()
                    : CommandStrings.revokeFailString();

            msg.resolve("what", Component.text(targetName)).resolve("who", who.getName());

            commandSender.sendMessage(MessageUtils.prefixes(commandSender, msg));
        });

        return 1;
    }
}

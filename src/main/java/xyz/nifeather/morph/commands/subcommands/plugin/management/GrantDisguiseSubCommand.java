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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.brigadier.arguments.DisguiseIdentifierArgumentType;
import xyz.nifeather.morph.messages.*;
import xyz.nifeather.morph.misc.DisguiseTypes;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

public class GrantDisguiseSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "grant";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.manageGrantDescription();
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.MANAGE_GRANT_DISGUISE;
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who", ArgumentTypes.players())
                                        .then(
                                                Commands.argument("what", DisguiseIdentifierArgumentType.ALL_AVAILABLE)
                                                        .executes(this::execute)
                                        )
                        )
        );
    }

    @Resolved
    private MorphManager morphs;

    private int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        var players = context.getArgument("who", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource());

        var commandSender = context.getSource().getSender();

        if (players.isEmpty())
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender, CommonStrings.playerNotFoundString()));
            return 1;
        }

        players.forEach(who ->
        {
            var targetName = StringArgumentType.getString(context, "what");

            if (!who.isOnline())
                return;

            if (!targetName.contains(":"))
                targetName = "minecraft:" + targetName;

            //检查是否已知
            var provider = MorphManager.getProvider(targetName);

            var nameType = DisguiseTypes.fromId(targetName);
            if (nameType.toStrippedId(targetName).equals("@all"))
            {
                var allDisg = provider.getAllAvailableDisguises();
                allDisg.forEach(id -> grantDisguise(who, nameType.toId(id), commandSender));

                return;
            }
            else if (!provider.isValid(targetName))
            {
                commandSender.sendMessage(MessageUtils.prefixes(commandSender, MorphStrings.invalidIdentityString()));
                return;
            }

            grantDisguise(who, targetName, commandSender);
        });

        return 1;
    }

    private void grantDisguise(Player who, String targetName, CommandSender commandSender)
    {
        var msg = morphs.grantMorphToPlayer(who, targetName)
                ? CommandStrings.grantSuccessString()
                : CommandStrings.grantFailString();

        msg.resolve("what", Component.text(targetName)).resolve("who", who.getName());

        commandSender.sendMessage(MessageUtils.prefixes(commandSender, msg));

    }
}

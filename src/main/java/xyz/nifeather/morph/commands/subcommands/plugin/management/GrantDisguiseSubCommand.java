package xyz.nifeather.morph.commands.subcommands.plugin.management;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.messages.*;
import xyz.nifeather.morph.misc.DisguiseTypes;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.util.concurrent.CompletableFuture;

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
                                Commands.argument("who", StringArgumentType.string())
                                        .suggests(this::suggestPlayer)
                                        .then(
                                                Commands.argument("what", StringArgumentType.greedyString())
                                                        .executes(this::execute)
                                                        .suggests(this::suggestDisguise)
                                        )
                        )
        );
    }

    private CompletableFuture<Suggestions> suggestPlayer(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var online = Bukkit.getOnlinePlayers();

        return CompletableFuture.supplyAsync(() ->
        {
            var input = suggestionsBuilder.getRemainingLowerCase();

            online.stream().forEach(player ->
            {
                var name = player.getName();
                if (name.toLowerCase().contains(input))
                    suggestionsBuilder.suggest(name);
            });

            return suggestionsBuilder.build();
        });
    }

    private CompletableFuture<Suggestions> suggestDisguise(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            var input = suggestionsBuilder.getRemainingLowerCase();

            for (var p : MorphManager.getProviders())
            {
                if (p == MorphManager.fallbackProvider) continue;

                var ns = p.getNameSpace();
                p.getAllAvailableDisguises().forEach(s ->
                {
                    var str = ns + ":" + s;
                    if (str.toLowerCase().contains(input))
                        suggestionsBuilder.suggest(str);
                });

                suggestionsBuilder.suggest(ns + ":" + "@all");
            }

            return suggestionsBuilder.build();
        });
    }

    @Resolved
    private MorphManager morphs;

    private int execute(CommandContext<CommandSourceStack> context)
    {
        var who = Bukkit.getPlayerExact(StringArgumentType.getString(context, "who"));
        var targetName = StringArgumentType.getString(context, "what");
        var commandSender = context.getSource().getSender();

        if (who == null || !who.isOnline())
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender, CommonStrings.playerNotFoundString()));
            return 1;
        }

        if (!targetName.contains(":"))
            targetName = "minecraft:" + targetName;

        //检查是否已知
        var provider = MorphManager.getProvider(targetName);

        var nameType = DisguiseTypes.fromId(targetName);
        if (nameType.toStrippedId(targetName).equals("@all"))
        {
            var allDisg = provider.getAllAvailableDisguises();
            allDisg.forEach(id -> grantDisguise(who, nameType.toId(id), commandSender));

            return 1;
        }
        else if (!provider.isValid(targetName))
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender, MorphStrings.invalidIdentityString()));
            return 1;
        }

        grantDisguise(who, targetName, commandSender);

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

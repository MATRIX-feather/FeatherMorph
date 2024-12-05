package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xiamomc.pluginbase.Messages.MessageStore;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.events.api.lifecycle.ConfigurationReloadEvent;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphMessageStore;
import xyz.nifeather.morph.messages.vanilla.VanillaMessageStore;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.misc.recipe.RecipeManager;
import xyz.nifeather.morph.misc.skins.PlayerSkinProvider;
import xyz.nifeather.morph.network.multiInstance.MultiInstanceService;
import xyz.nifeather.morph.storage.skill.SkillsConfigurationStoreNew;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReloadSubCommand extends BrigadierCommand
{
    @Override
    @NotNull
    public String name()
    {
        return "reload";
    }

    @Override
    public @Nullable String getPermissionRequirement()
    {
        return CommonPermissions.DO_RELOAD;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.reloadDescription();
    }

    @Resolved
    private MorphManager morphManager;

    @Resolved
    private MorphConfigManager config;

    @Resolved
    private MessageStore<?> messageStore;

    @Resolved
    private VanillaMessageStore vanillaMessageStore;

    @Resolved
    private SkillsConfigurationStoreNew skills;

    @Resolved
    private MultiInstanceService multiInstanceService;

    @Resolved
    private RecipeManager recipeManager;

    private final List<String> subcommands = ObjectImmutableList.of("data", "message", "update_message");

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal("reload")
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("operation", StringArgumentType.greedyString())
                                        .suggests(this::suggests)
                                        .executes(this::executes)
                        )
        );

        super.registerAsChild(parentBuilder);
    }

    public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            suggestionsBuilder.suggest("*");

            var input = suggestionsBuilder.getRemainingLowerCase();

            var target = subcommands.stream()
                    .filter(s -> s.startsWith(input))
                    .toList();

            target.forEach(suggestionsBuilder::suggest);

            return suggestionsBuilder.build();
        });
    }

    public int executes(CommandContext<CommandSourceStack> context)
    {
        var reloadsData = false;
        var reloadsMessage = false;
        var reloadOverwriteNonDefMsg = false;
        String option = StringArgumentType.getString(context, "operation");

        switch (option)
        {
            case "data" -> reloadsData = true;
            case "message" -> reloadsMessage = true;
            case "update_message" -> reloadsMessage = reloadOverwriteNonDefMsg = true;
            default -> reloadsMessage = reloadsData = true;
        }

        if (reloadsData)
        {
            config.reload();
            skills.clearCache();
            morphManager.reloadConfiguration();

            PlayerSkinProvider.getInstance().reload();

            multiInstanceService.onReload();

            recipeManager.reload();
        }

        if (reloadsMessage)
        {
            if (reloadOverwriteNonDefMsg && messageStore instanceof MorphMessageStore morphMessageStore)
                morphMessageStore.reloadOverwriteNonDefault();
            else
                messageStore.reloadConfiguration();

            vanillaMessageStore.reloadConfiguration();
        }

        var event = new ConfigurationReloadEvent(reloadsData, reloadsMessage);
        event.callEvent();

        var sender = context.getSource().getSender();
        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.reloadCompleteMessage()));

        return 0;
    }
}

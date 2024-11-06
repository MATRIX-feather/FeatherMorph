package xyz.nifeather.morph.commands.subcommands.plugin;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Command.ISubCommand;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xiamomc.pluginbase.Messages.MessageStore;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.api.v0.events.lifecycle.ConfigurationReloadEvent;
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

public class ReloadSubCommand extends MorphPluginObject implements ISubCommand
{
    @Override
    @NotNull
    public String getCommandName()
    {
        return "reload";
    }

    @Override
    @NotNull
    public String getPermissionRequirement()
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
    public List<String> onTabComplete(List<String> args, CommandSender source)
    {
        if (source.hasPermission(getPermissionRequirement()) && args.size() >= 1)
            return subcommands.stream().filter(s -> s.startsWith(args.get(0))).toList();
        else
            return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args)
    {
        if (sender.hasPermission(getPermissionRequirement()))
        {
            var reloadsData = false;
            var reloadsMessage = false;
            var reloadOverwriteNonDefMsg = false;
            String option = args.length >= 1 ? args[0] : "*";

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

            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.reloadCompleteMessage()));
        }
        else
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.noPermissionMessage()));

        return true;
    }
}

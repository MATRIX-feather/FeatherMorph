package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.subcommands.OptionSubCommands;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.events.InteractionMirrorProcessor;
import xyz.nifeather.morph.messages.CommandNameStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OptionSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "option";
    }

    @Resolved(shouldSolveImmediately = true)
    private MorphConfigManager config;

    public OptionSubCommand()
    {
        subCommands.add(getToggle("bossbar", ConfigOption.DISPLAY_BOSSBAR, CommandNameStrings.bossbarString()));

        subCommands.add(getToggle("unmorph_on_death", ConfigOption.UNMORPH_ON_DEATH));

        //subCommands.with(getToggle("allow_local_disguise", ConfigOption.ALLOW_LD_DISGUISES, "ld", CommandNameStrings.allowLDDisguiseString()));

        subCommands.add(getToggle("allow_client_mods", ConfigOption.ALLOW_CLIENT));

        subCommands.add(getToggle("piglin_brute_ignore_disguises", ConfigOption.PIGLIN_BRUTE_IGNORE_DISGUISES));

        subCommands.add(getToggle("headmorph", ConfigOption.ALLOW_HEAD_MORPH, CommandNameStrings.headMorphString()));

        subCommands.add(getToggle("chatoverride", ConfigOption.ALLOW_CHAT_OVERRIDE, CommandNameStrings.chatOverrideString()));

        subCommands.add(getToggle("modify_bounding_boxes", ConfigOption.MODIFY_BOUNDING_BOX));

        subCommands.add(getToggle("force_protocol_version", ConfigOption.FORCE_TARGET_VERSION));

        subCommands.add(getToggle("armorstand_show_arms", ConfigOption.ARMORSTAND_SHOW_ARMS));

        subCommands.add(getMirrorMode("mirror_mode", ConfigOption.MIRROR_SELECTION_MODE, null));
        subCommands.add(getToggle("mirror_interaction", ConfigOption.MIRROR_BEHAVIOR_DO_SIMULATION, CommandNameStrings.mirrorInteractionString()));
        subCommands.add(getToggle("mirror_sneak", ConfigOption.MIRROR_BEHAVIOR_SNEAK, CommandNameStrings.mirrorSneakString()));
        subCommands.add(getToggle("mirror_swaphand", ConfigOption.MIRROR_BEHAVIOR_SWAP_HAND, CommandNameStrings.mirrorSwapHandString()));
        subCommands.add(getToggle("mirror_drop", ConfigOption.MIRROR_BEHAVIOR_DROP, CommandNameStrings.mirrorDropString()));
        subCommands.add(getToggle("mirror_hotbar", ConfigOption.MIRROR_BEHAVIOR_HOTBAR, CommandNameStrings.mirrorHotbar()));
        subCommands.add(getToggle("mirror_ignore_disguised", ConfigOption.MIRROR_IGNORE_DISGUISED, CommandNameStrings.mirrorIgnoreDisguised()));
        subCommands.add(getToggle("mirror_log_operations", ConfigOption.MIRROR_LOG_OPERATION));
        subCommands.add(getInteger("mirror_log_cleanup", ConfigOption.MIRROR_LOG_CLEANUP_DATE));

        subCommands.add(getToggle("debug_output", ConfigOption.DEBUG_OUTPUT));
        subCommands.add(getToggle("revealing", ConfigOption.REVEALING));

        subCommands.add(getToggle("check_update", ConfigOption.CHECK_UPDATE));
        subCommands.add(getToggle("allow_acquire_morphs", ConfigOption.ALLOW_ACQUIRE_MORPHS));

        subCommands.add(getToggle("log_outgoing_packets", ConfigOption.LOG_OUTGOING_PACKETS));
        subCommands.add(getToggle("log_incoming_packets", ConfigOption.LOG_INCOMING_PACKETS));

        subCommands.add(getToggle("allow_acquire_morphs", ConfigOption.ALLOW_ACQUIRE_MORPHS));

        subCommands.add(getToggle("allow_flight", ConfigOption.ALLOW_FLIGHT));

        subCommands.add(getToggle("client_renderer", ConfigOption.USE_CLIENT_RENDERER));

        subCommands.add(getList("banned_disguises", ConfigOption.BANNED_DISGUISES, null));
        subCommands.add(getList("nofly_worlds", ConfigOption.NOFLY_WORLDS, null));
        subCommands.add(getList("blacklist_tags", ConfigOption.BLACKLIST_TAGS, null));
        subCommands.add(getList("blacklist_nbt_pattern", ConfigOption.BLACKLIST_PATTERNS, null));

        subCommands.add(getToggle("ability_check_permissions", ConfigOption.DO_CHECK_ABILITY_PERMISSIONS, null));

        subCommands.add(getToggle("towny_allow_flight_in_wilderness", ConfigOption.TOWNY_ALLOW_FLY_IN_WILDERNESS));
    }

    private CompletableFuture<Suggestions> suggestListOperation(CommandContext<CommandSourceStack> context,
                                                                SuggestionsBuilder suggestionsBuilder)
    {
        suggestionsBuilder.suggest("list").suggest("add").suggest("remove");

        return suggestionsBuilder.buildFuture();
    }

    private IConvertibleBrigadier getList(String optionName, ConfigOption option,
                                                                                 @Nullable FormattableMessage displayName)
    {
        return new OptionSubCommands.StringListOptionBaseCommand(optionName, config, option);
    }

    private IConvertibleBrigadier getMirrorMode(String name, ConfigOption option, @Nullable FormattableMessage displayName)
    {
        return new OptionSubCommands.LimiterStringListOptionCommand(
                name, config, option, List.of(
                        InteractionMirrorProcessor.InteractionMirrorSelectionMode.BY_NAME.toLowerCase(),
                        InteractionMirrorProcessor.InteractionMirrorSelectionMode.BY_SIGHT.toLowerCase())
        );
    }

    private IConvertibleBrigadier getInteger(String name, ConfigOption option)
    {
        return getInteger(name, option, null);
    }

    private IConvertibleBrigadier getInteger(String name, ConfigOption option, @Nullable FormattableMessage displayName)
    {
        return new OptionSubCommands.BooleanOptionCommand(name, config, option);
    }

    private IConvertibleBrigadier getToggle(String name, ConfigOption option)
    {
        return getToggle(name, option, null);
    }

    private IConvertibleBrigadier getToggle(String name, ConfigOption option, @Nullable FormattableMessage displayName)
    {
        return new OptionSubCommands.BooleanOptionCommand(name, config, option);
    }

    private boolean parseBoolean(String input)
    {
        return "true".equalsIgnoreCase(input)
                || "t".equalsIgnoreCase(input)
                || "on".equalsIgnoreCase(input)
                || "1".equalsIgnoreCase(input)
                || "enable".equalsIgnoreCase(input)
                || "enabled".equalsIgnoreCase(input);
    }

    private final List<IConvertibleBrigadier> subCommands = new ObjectArrayList<>();

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.SET_OPTIONS;
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        var thisBuilder = Commands.literal(name()).requires(this::checkPermission);

        for (IConvertibleBrigadier subCommand : this.subCommands)
            subCommand.registerAsChild(thisBuilder);

        parentBuilder.then(thisBuilder);
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.pluginOptionDescription();
    }
}

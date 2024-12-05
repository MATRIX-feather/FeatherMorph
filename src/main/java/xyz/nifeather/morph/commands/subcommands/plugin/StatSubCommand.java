package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.abilities.AbilityManager;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.StatStrings;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.skills.MorphSkillHandler;

public class StatSubCommand extends BrigadierCommand
{
    @Override
    public @NotNull String name()
    {
        return "stat";
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .executes(this::executes)
        );

        super.registerAsChild(parentBuilder);
    }

    /**
     * 获取此指令的帮助信息
     *
     * @return 帮助信息
     */
    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.statDescription();
    }

    @Resolved
    private MorphClientHandler clientHandler;

    @Resolved
    private MorphSkillHandler skillHandler;

    @Resolved
    private AbilityManager abilityHandler;

    @Resolved
    private MorphManager morphManager;

    public int executes(CommandContext<CommandSourceStack> context)
    {
        var disguisesActive = morphManager.getActiveDisguises().stream()
                .filter(s -> s.getPlayer().isOnline()).toArray().length;

        var authors = "MATRIX-feather"; //plugin.getPluginMeta().getAuthors();

        var sender = context.getSource().getSender();

        var listString = new StringBuilder();
        var backends = morphManager.listManagedBackends();
        var locale = MessageUtils.getLocale(sender);

        for (var backend : backends)
        {
            var instances = backend.listInstances();
            if (instances.isEmpty()) continue;

            var formattable = StatStrings.backendDescription();

            formattable.resolve("name", backend.getIdentifier())
                    .resolve("count", "" + instances.size());

            var str = formattable.toString(locale);
            listString.append(str).append(" ");
        }

        if (listString.isEmpty())
            listString.append(StatStrings.backendsNone().toString(locale));

        var defaultBackend = morphManager.getDefaultBackend();
        var defaultBackendString = "%s (%s)".formatted(defaultBackend.getIdentifier(), defaultBackend.getClass().getName());

        var msg = new FormattableMessage[]
                {
                        StatStrings.versionString()
                                .resolve("version", plugin.getPluginMeta().getVersion())
                                .resolve("author", authors)
                                .resolve("proto", String.valueOf(clientHandler.targetApiVersion)),

                        StatStrings.defaultBackendString()
                                .resolve("backend", defaultBackendString),

                        StatStrings.activeBackends()
                                .resolve("list", listString.toString()),

                        StatStrings.providersString()
                                .resolve("count", String.valueOf(MorphManager.getProviders().size())),

                        StatStrings.bannedDisguisesString()
                                .resolve("count", String.valueOf(morphManager.getBannedDisguises().size())),

                        StatStrings.abilitiesString()
                                .resolve("count", String.valueOf(abilityHandler.getRegistedAbilities().size())),

                        StatStrings.skillsString()
                                .resolve("count", String.valueOf(skillHandler.getRegistedSkills().size())),

                        StatStrings.activeClientsString()
                                .resolve("count", String.valueOf(clientHandler.getConnectedPlayers().size())),

                        StatStrings.activeDisguisesString()
                                .resolve("count", String.valueOf(disguisesActive))
                                .resolve("max", String.valueOf(Bukkit.getOnlinePlayers().size()))
                };

        for (FormattableMessage formattableMessage : msg)
            sender.sendMessage(MessageUtils.prefixes(sender, formattableMessage));

        return 1;
    }

    private FormattableMessage getFormattable(String str)
    {
        return new FormattableMessage(plugin, str);
    }
    
    @Override
    public String getPermissionRequirement()
    {
        return CommonPermissions.CHECK_STAT;
    }
}

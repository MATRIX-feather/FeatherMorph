package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.subcommands.plugin.management.ForceMorphSubCommand;
import xyz.nifeather.morph.commands.subcommands.plugin.management.ForceUnmorphSubCommand;
import xyz.nifeather.morph.commands.subcommands.plugin.management.GrantDisguiseSubCommand;
import xyz.nifeather.morph.commands.subcommands.plugin.management.RevokeDisguiseSubCommand;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.util.List;

public class DisguiseManageSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    private final List<IConvertibleBrigadier> subCommands = ObjectList.of(
            new GrantDisguiseSubCommand(),
            new RevokeDisguiseSubCommand(),
            new ForceUnmorphSubCommand(),
            new ForceMorphSubCommand()
    );

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        var thisBuilder = Commands.literal(name())
                .requires(this::checkPermission);

        this.subCommands.forEach(s -> s.registerAsChild(thisBuilder));

        parentBuilder.then(thisBuilder);
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.MANAGE_DISGUISES;
    }

    @Override
    public @NotNull String name()
    {
        return "manage";
    }

    @Override
    public @Nullable FormattableMessage getHelpMessage()
    {
        return HelpStrings.manageDescription();
    }
}

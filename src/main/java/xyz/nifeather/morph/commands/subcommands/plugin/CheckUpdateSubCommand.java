package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.UpdateStrings;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.updates.UpdateHandler;

public class CheckUpdateSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "check_update";
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.CHECK_UPDATE;
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .executes(this::execute)
        );
    }

    private int execute(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        sender.sendMessage(MessageUtils.prefixes(sender, UpdateStrings.checkingUpdate()));
        handler.checkUpdate(true, result ->
                this.onRequestFinish(result, sender), sender);

        return 1;
    }

    /**
     * 获取此指令的帮助信息
     *
     * @return 帮助信息
     */
    @Override
    public FormattableMessage getHelpMessage()
    {
        return new FormattableMessage(plugin, "update");
    }

    @Resolved
    private UpdateHandler handler;

    private void onRequestFinish(UpdateHandler.CheckResult result, CommandSender sender)
    {
        if (result == UpdateHandler.CheckResult.ALREADY_LATEST)
        {
            var msg = UpdateStrings.noNewVersionAvailable().resolve("mc_version", Bukkit.getMinecraftVersion());
            sender.sendMessage(MessageUtils.prefixes(sender, msg));
        }
    }
}

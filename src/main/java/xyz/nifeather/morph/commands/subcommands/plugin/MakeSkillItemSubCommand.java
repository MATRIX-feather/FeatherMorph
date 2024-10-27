package xyz.nifeather.morph.commands.subcommands.plugin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Command.ISubCommand;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.utilities.ItemUtils;

import java.util.List;

public class MakeSkillItemSubCommand extends MorphPluginObject implements ISubCommand
{
    @Override
    public @NotNull String getCommandName()
    {
        return "make_disguise_tool";
    }

    @Override
    public @Nullable String getPermissionRequirement()
    {
        return CommonPermissions.MAKE_DISGUISE_TOOL;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return new FormattableMessage(plugin, "make selected a disguise tool");
    }

    private final List<String> emptyList = List.of();

    @Override
    public @Nullable List<String> onTabComplete(List<String> args, CommandSender source)
    {
        return emptyList;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.noPermissionMessage()));
            return true;
        }

        var item = player.getEquipment().getItemInMainHand();
        if (item.isEmpty() || item.getType().isAir())
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.illegalArgumentString().resolve("detail", "air... :(")));
            return true;
        }

        item = ItemUtils.buildDisguiseToolFrom(item);
        player.getEquipment().setItemInMainHand(item);

        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.success()));

        return true;
    }
}

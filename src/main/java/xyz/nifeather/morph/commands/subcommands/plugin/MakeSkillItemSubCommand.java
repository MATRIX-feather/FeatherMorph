package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.utilities.ItemUtils;

public class MakeSkillItemSubCommand extends BrigadierCommand
{
    @Override
    public @NotNull String name()
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

    @Override
    public boolean register(Commands dispatcher)
    {
        dispatcher.register(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .executes(this::executes)
                        .build()
        );

        return super.register(dispatcher);
    }

    public int executes(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        if (!(sender instanceof Player player))
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.noPermissionMessage()));
            return 1;
        }

        var item = player.getEquipment().getItemInMainHand();
        if (item.isEmpty() || item.getType().isAir())
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.illegalArgumentString().resolve("detail", "air... :(")));
            return 1;
        }

        item = ItemUtils.buildDisguiseToolFrom(item);
        player.getEquipment().setItemInMainHand(item);

        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.success()));

        return 1;
    }
}

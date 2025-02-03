package xyz.nifeather.morph.commands;

import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;

import java.util.concurrent.TimeUnit;

public class DebugTestCommand extends BrigadierCommand
{
    @Override
    public String getPermissionRequirement()
    {
        return null;
    }

    @Resolved
    private MorphManager morphManager;

    @Override
    public boolean register(Commands dispatcher)
    {

        dispatcher.register(
                Commands.literal("testDataStore").executes(ctx ->
                {
                    try
                    {
                        var store = morphManager.getDataStore();
                        var sender = ctx.getSource().getSender();

                        Bukkit.getAsyncScheduler().runDelayed(plugin, task ->
                        {
                            sender.sendMessage("Test1");

                            var UUID = java.util.UUID.fromString("0-0-0-0-0");
                            store.getPlayerMeta(Bukkit.getOfflinePlayer(UUID));
                        }, 50, TimeUnit.MILLISECONDS);

                        sender.sendMessage("Test2");
                        var UUID = java.util.UUID.fromString("0-0-0-0-0");
                        store.getPlayerMeta(Bukkit.getOfflinePlayer(UUID));

                    }
                    catch (Throwable t)
                    {
                        logger.warn("Exception! " + t.getMessage());
                        t.printStackTrace();
                    }
                    return 1;
                })
                        .build()
        );

        return true;
    }

    @Override
    public @NotNull String name()
    {
        return "fmdebug";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return null;
    }
}

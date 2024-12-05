package xyz.nifeather.morph.commands.brigadier;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import xyz.nifeather.morph.commands.IHaveFormattableHelp;

import java.util.List;

//                                             todo: Remove this
public interface IConvertibleBrigadier extends IHaveFormattableHelp
{
    /**
     * @return Whether this command is self-registered
     */
    public default boolean register(Commands dispatcher)
    {
        return false;
    }

    public default void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
    }

    public default boolean checkPermission(CommandSourceStack context)
    {
        var permission = this.permission();
        return permission == null || context.getSender().hasPermission(permission);
    }

    @Nullable
    public default String permission()
    {
        return null;
    }

    /**
     * @return List of valid children that can be used for building help messages
     */
    @Unmodifiable
    public default List<? extends IHaveFormattableHelp> children()
    {
        return List.of();
    }
}

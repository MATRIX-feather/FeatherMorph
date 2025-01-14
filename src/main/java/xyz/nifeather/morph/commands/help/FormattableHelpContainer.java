package xyz.nifeather.morph.commands.help;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.commands.IHaveFormattableHelp;

import java.util.List;

public record FormattableHelpContainer(@Nullable String perm,
                                       String name,
                                       FormattableMessage message,
                                       @Nullable List<FormattableMessage> notes) implements IHaveFormattableHelp
{
    public FormattableHelpContainer(String name, FormattableMessage message)
    {
        this(null, name, message, null);
    }

    @Override
    public List<FormattableMessage> getNotes()
    {
        return notes != null ? notes : IHaveFormattableHelp.super.getNotes();
    }

    @Override
    public @Nullable String permission()
    {
        return perm;
    }

    @Override
    public @NotNull String name()
    {
        return name;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return message;
    }
}

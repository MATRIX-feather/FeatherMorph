package xyz.nifeather.morph.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPlugin;

import java.util.List;

public interface IHaveFormattableHelp
{
    @Nullable
    public String permission();

    @NotNull
    public String name();

    public FormattableMessage getHelpMessage();

    default public List<FormattableMessage> getNotes()
    {
        return List.of(new FormattableMessage(MorphPlugin.getMorphNameSpace(), "_", "_"));
    }
}

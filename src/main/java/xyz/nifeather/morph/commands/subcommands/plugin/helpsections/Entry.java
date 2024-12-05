package xyz.nifeather.morph.commands.subcommands.plugin.helpsections;

import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Messages.FormattableMessage;

public record Entry(@Nullable String permission, String baseName, FormattableMessage description, String suggestingCommand)
{
    @Override
    public String toString()
    {
        return baseName + "的Entry";
    }
}

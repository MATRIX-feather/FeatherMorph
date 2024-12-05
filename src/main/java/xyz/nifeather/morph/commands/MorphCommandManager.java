package xyz.nifeather.morph.commands;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;

import java.util.List;

public class MorphCommandManager extends MorphPluginObject
{
    private final List<IConvertibleBrigadier> commands = List.of(
            new MorphCommand(),
            new MorphPlayerCommand(),
            new UnMorphCommand(),
            new RequestCommand(),
            new MorphPluginCommand(),
            new AnimationCommand());

    public List<IConvertibleBrigadier> commands()
    {
        return this.commands;
    }

    public void register(ReloadableRegistrarEvent<@NotNull Commands> event)
    {
        var registrar = event.registrar();

        for (var brigadierConvertable : commands)
            brigadierConvertable.register(registrar);
    }
}

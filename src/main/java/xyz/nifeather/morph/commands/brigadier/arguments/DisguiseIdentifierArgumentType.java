package xyz.nifeather.morph.commands.brigadier.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.misc.DisguiseMeta;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class DisguiseIdentifierArgumentType extends MorphPluginObject implements CustomArgumentType<String, String>
{
    private static final List<String> EXAMPLES = List.of("allay", "minecraft:allay", "player:Icalingua");
    public static final DisguiseIdentifierArgumentType ALL_AVAILABLE = new DisguiseIdentifierArgumentType(false);
    public static final DisguiseIdentifierArgumentType FOR_PLAYER = new DisguiseIdentifierArgumentType(true);

    public static String getArgument(final CommandContext<?> context, final String name)
    {
        return context.getArgument(name, String.class);
    }

    private final boolean usePlayerAvailableDisguises;

    protected DisguiseIdentifierArgumentType(boolean usePlayerAvailableDisguises)
    {
        this.usePlayerAvailableDisguises = usePlayerAvailableDisguises;
    }

    @Override
    @NotNull
    public String parse(StringReader reader) throws CommandSyntaxException
    {
        int begin = reader.getCursor();

        if (!reader.canRead())
            reader.skip();

        while (reader.canRead() && !Character.isWhitespace(reader.peek()))
            reader.skip();

        return reader.getString().substring(begin, reader.getCursor());
    }

    private final List<String> cachedAvailableIDs = ObjectLists.synchronize(new ObjectArrayList<>());

    @Override
    @NotNull
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
    {
        return usePlayerAvailableDisguises
                ? suggestPlayerDisguises(context, builder)
                : suggestAllDisguises(context, builder);
    }

    @Resolved(shouldSolveImmediately = true)
    private MorphManager morphs;

    private <S> CompletableFuture<Suggestions> suggestPlayerDisguises(CommandContext<S> context, SuggestionsBuilder builder)
    {
        if (!(context.getSource() instanceof CommandSourceStack commandSourceStack))
            return builder.buildFuture();

        var source = commandSourceStack.getExecutor();

        if (!(source instanceof Player player))
            return CompletableFuture.completedFuture(builder.build());

        String input = builder.getRemainingLowerCase();

        var availableDisguises = morphs.getAvaliableDisguisesFor(player);

        return CompletableFuture.supplyAsync(() ->
        {
            for (DisguiseMeta disguiseMeta : availableDisguises)
            {
                var name = disguiseMeta.getKey();

                if (!name.toLowerCase().contains(input))
                    continue;

                builder.suggest(name);
            }

            return builder.build();
        });
    }

    private <S> CompletableFuture<Suggestions> suggestAllDisguises(CommandContext<S> context, SuggestionsBuilder builder)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            if (!cachedAvailableIDs.isEmpty())
            {
                cachedAvailableIDs.forEach(builder::suggest);
                return builder.build();
            }

            var input = builder.getRemainingLowerCase();

            for (var p : MorphManager.getProviders())
            {
                if (p == MorphManager.fallbackProvider) continue;

                var providerNamespace = p.getNameSpace();
                p.getAllAvailableDisguises().forEach(path ->
                {
                    var id = providerNamespace + ":" + path;
                    if (id.toLowerCase().contains(input))
                        builder.suggest(id);

                    cachedAvailableIDs.add(id);
                });

                builder.suggest(providerNamespace + ":" + "@all");
                cachedAvailableIDs.add(providerNamespace + ":" + "@all");
            }

            return builder.build();
        });
    }

    @Override
    @NotNull
    public ArgumentType<String> getNativeType()
    {
        return StringArgumentType.word();
    }

    @Override
    public Collection<String> getExamples()
    {
        return EXAMPLES;
    }
}

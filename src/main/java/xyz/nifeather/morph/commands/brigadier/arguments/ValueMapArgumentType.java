package xyz.nifeather.morph.commands.brigadier.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xyz.nifeather.morph.FeatherMorphMain;

import java.util.Collection;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class ValueMapArgumentType implements CustomArgumentType<Map<String, String>, String>
{
    public static final Collection<String> EXAMPLES = ObjectArrayList.of("[foo=bar]", "[foo=bar, aabb=\"ccdd\"]");
    private static final Logger log = FeatherMorphMain.getInstance().getSLF4JLogger();
    private static final Map<String, String> defaultMap = new Object2ObjectOpenHashMap<>();

    public static Map<String, String> get(String name, CommandContext<CommandSourceStack> context)
    {
        return context.getArgument(name, defaultMap.getClass());
    }

    @Override
    public Map<String, String> parse(StringReader reader) throws CommandSyntaxException
    {
        if (reader.peek() != '[')
            throw new SimpleCommandExceptionType(Component.translatable("Expected square bracket to start a string")).createWithContext(reader);

        if (!reader.getString().endsWith("]"))
            throw new SimpleCommandExceptionType(Component.literal("Unclosed bracket string")).createWithContext(reader);

        reader.skip();

        Map<String, String> map = new Object2ObjectOpenHashMap<>();

        while (reader.canRead())
        {
            var subString = readStringQuotableUntil(reader, ',');

            if (subString == null) continue;

            String[] split = subString.split("=", 2);
            String key = split[0];
            String value = split.length > 1 ? split[1] : null;

            if (value != null)
                map.put(key, value);
        }

        return map;
    }

    /**
     * @return A string, NULL if the input equals 'terminator'
     * @throws CommandSyntaxException
     */
    @Nullable
    public String readStringQuotableUntil(StringReader reader, char terminator) throws CommandSyntaxException
    {
        //log.info("Starting read... Peek is '%s'".formatted(reader.peek()));
        StringBuilder builder = new StringBuilder();

        while (true)
        {
            if (!reader.canRead())
                break;

            char current = reader.read();
            //log.info("Current: '%s'".formatted(current));

            // 遇到了结束符
            if (current == terminator)
                break;

            // 遇到引号了
            if (StringReader.isQuotedStringStart(current))
            {
                var str = reader.readStringUntil(current);

                //log.info("APPENDING QUOTE STRING [%s]".formatted(str));
                builder.append(str);
                continue;
            }

            // 如果遇到了闭合括号，break;
            if (current == ']')
                break;

            // 是空格
            if (Character.isWhitespace(current))
                continue;

            // 其他情况
            //log.info("APPENDING [%s]".formatted(current));
            builder.append(current);
        }

        //log.info("DONE! result is [%s]".formatted(builder.toString()));
        return builder.isEmpty() ? null : builder.toString();
    }

    @Override
    @NotNull
    public ArgumentType<String> getNativeType()
    {
        return StringArgumentType.greedyString();
    }

    @Override
    @NotNull
    public Collection<String> getExamples()
    {
        return EXAMPLES;
    }
}

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

    private static final Component ERR_NO_BRACKET = Component.translatableWithFallback(
            "morphclient.parsing.bracket.start",
            "Expected square bracket (\"[\") to start a string"
    );

    private static final Component ERR_EMPTY_KEY = Component.translatableWithFallback(
            "morphclient.parsing.key.empty",
            "May not have a empty key"
    );

    private static SimpleCommandExceptionType forKeyNoValue(String key)
    {
        return new SimpleCommandExceptionType(Component.translatableWithFallback(
                "morphclient.parsing.value_map.no_value",
                "Missing value for key '%s'",
                key
        ));
    }

    private static SimpleCommandExceptionType forDuplicateKey(String key)
    {
        return new SimpleCommandExceptionType(Component.translatableWithFallback(
                "morphclient.parsing.value_map.duplicate_key",
                "Duplicate key '%s'",
                key
        ));
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    @Override
    public Map<String, String> parse(StringReader reader) throws CommandSyntaxException
    {
        if (reader.peek() != '[')
            throw new SimpleCommandExceptionType(ERR_NO_BRACKET).createWithContext(reader);

        reader.skip();

        Map<String, String> map = new Object2ObjectOpenHashMap<>();

        boolean closeBracketMet = false;
        while (reader.canRead())
        {
            if (reader.peek() == ']')
            {
                closeBracketMet = true;
                break;
            }

            int beginCursor = reader.getCursor();
            var parseResult = parseOnce(reader, ',', ']');

            if (parseResult.key == null)
            {
                reader.setCursor(beginCursor);
                throw new SimpleCommandExceptionType(ERR_EMPTY_KEY).createWithContext(reader);
            }

            if (parseResult.value != null)
            {
                if (map.containsKey(parseResult.key))
                {
                    var index = reader.getString().substring(beginCursor).indexOf(parseResult.key);
                    reader.setCursor(beginCursor + index);

                    throw forDuplicateKey(parseResult.key).createWithContext(reader);
                }

                map.put(parseResult.key, parseResult.value);
            }
            else
            {
                var index = reader.getString().substring(beginCursor).indexOf(parseResult.key);
                reader.setCursor(beginCursor + index);

                throw forKeyNoValue(parseResult.key).createWithContext(reader);
            }
        }

        if (closeBracketMet)
            reader.skip();
        else
            throw new SimpleCommandExceptionType(Component.translatable("parsing.expected", "]")).createWithContext(reader);

        return map;
    }

    public record KeyValuePair(@Nullable String key, @Nullable String value)
    {
    }

    /**
     * @return A string, NULL if the input equals 'terminator'
     * @throws CommandSyntaxException
     */
    @NotNull
    public KeyValuePair parseOnce(StringReader reader, char terminator, char endOfString) throws CommandSyntaxException
    {
        //log.info("Starting read... Peek is '%s'".formatted(reader.peek()));
        StringBuilder keyStringBuilder = new StringBuilder();
        @Nullable StringBuilder valueStringBuilder = null;

        boolean isKey = true;

        String key = null;
        String value = null;

        while (true)
        {
            if (!reader.canRead())
                break;

            char next = reader.peek();

            // 如果遇到了闭合括号，break;
            if (next == endOfString)
                break;

            // Next变Current
            reader.skip();

            //log.info("Current: '%s'".formatted(next));

            // 遇到了结束符
            if (next == terminator)
                break;

            //region 识别Key

            var builder = isKey ? keyStringBuilder : valueStringBuilder;

            // 遇到等于号，切换至Value
            if (next == '=' && isKey)
            {
                isKey = false;
                continue;
            }

            if (!isKey && builder == null)
                builder = valueStringBuilder = new StringBuilder();

            //endregion 识别Key

            // 遇到引号了
            if (StringReader.isQuotedStringStart(next))
            {
                var str = reader.readStringUntil(next);

                //log.info("APPENDING QUOTE STRING [%s]".formatted(str));
                builder.append(str);
                continue;
            }

            // 是空格
            if (Character.isWhitespace(next))
                continue;

            // 其他情况
            //log.info("APPENDING [%s]".formatted(current));
            builder.append(next);
        }

        if (!keyStringBuilder.isEmpty())
            key = keyStringBuilder.toString();

        if (valueStringBuilder != null)
            value = valueStringBuilder.toString();

        //log.info("DONE! result is [%s]".formatted(builder.toString()));
        return new KeyValuePair(key, value);
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

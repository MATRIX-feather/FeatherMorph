package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.MorphCommandManager;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.subcommands.plugin.helpsections.Entry;
import xyz.nifeather.morph.commands.subcommands.plugin.helpsections.Section;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HelpSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public String name()
    {
        return "help";
    }

    @Initializer
    private void load()
    {
        setupCommandSections();
    }

    @Resolved
    private MorphCommandManager cmdHelper;

    private final List<Section> commandSections = new ObjectArrayList<>();

    /**
     * 设置用于构建帮助信息的Section
     */
    private void setupCommandSections()
    {
        //不属于任何section的指令丢到这里
        var miscCommandSection = new Section("/", HelpStrings.morphCommandDescription(), ObjectList.of(
                HelpStrings.morphCommandSpecialNote1(),
                HelpStrings.morphCommandSpecialNote2()
        ));

        commandSections.add(miscCommandSection);

        //遍历所有指令
        for (var c : cmdHelper.commands())
        {
            //如果指令拥有子指令，新建section
            if (!c.children().isEmpty())
            {
                //此section下所有指令的父级指令
                var parentCommandName = c.name();

                List<FormattableMessage> notes = new ObjectArrayList<>(c.getNotes());

                var section = new Section(parentCommandName,
                        c.getHelpMessage(),
                        notes);

                //添加指令到section中
                for (var sc : c.children())
                {
                    var cmdName = parentCommandName + " " + sc.name();
                    section.add(new Entry(sc.permission(),
                            cmdName,
                             sc.getHelpMessage(),
                            "/" + cmdName));
                }

                commandSections.add(section);
            }
            else
            {
                miscCommandSection.add(new Entry(c.permission(),
                        c.name(),
                        c.getHelpMessage(),
                        "/" + c.name()));
            }
        }
    }

    private List<Component> constructSectionMessage(CommandSender sender, Section section)
    {
        var entries = section.getEntries();
        var locale = MessageUtils.getLocale(sender);

        //添加section的标题
        var list = ObjectArrayList.of(
                Component.empty(),
                HelpStrings.commandSectionHeaderString()
                        .resolve("basename", section.getCommandBaseName()).toComponent(locale));

        //build entry
        for (var entry : entries)
        {
            var perm = entry.permission();

            //如果指令不要求权限或者sender拥有此权限，添加到列表里
            if (perm == null || sender.hasPermission(perm))
            {
                var msg = HelpStrings.commandEntryString()
                        .withLocale(locale)
                        .resolve("basename", entry.baseName())
                        .resolve("description", entry.description(), null)
                        .toComponent(null)
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(HelpStrings.clickToCompleteString().toComponent(locale)))
                        .clickEvent(ClickEvent.suggestCommand(entry.suggestingCommand()));

                list.add(msg);
            }
        }

        if (section.getNotes() != null && !section.getNotes().isEmpty())
        {
            list.addAll(ObjectList.of(
                    Component.empty(),
                    HelpStrings.specialNoteString().toComponent(locale)
            ));

            for (var f : section.getNotes())
            {
                list.add(f.toComponent(locale)
                        .decorate(TextDecoration.ITALIC));
            }
        }

        list.add(Component.empty());

        return list;
    }

    /**
     * 从设置的Section中构建sender的帮助信息
     *
     * @param sender 要显示给谁
     * @return 构建的帮助信息
     */
    private List<Component> constructHelpMessage(CommandSender sender)
    {
        var list = new ObjectArrayList<Component>();
        var locale = MessageUtils.getLocale(sender);

        list.add(HelpStrings.avaliableCommandHeaderString().toComponent(locale));
        for (var section : commandSections)
        {
            var msg = HelpStrings.commandNamePatternString()
                    .withLocale(locale)
                    .resolve("basename", section.getCommandBaseName())
                    .resolve("description", section.getDescription(), null)
                    .toComponent(locale)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/feathermorph " + name() + " " + section.getCommandBaseName()))
                    .hoverEvent(HoverEvent.showText(HelpStrings.clickToViewString().toComponent(locale)));

            list.add(msg);
        }

        return list;
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .executes(this::executeNoArgs)
                        .then(
                                Commands.argument("section", StringArgumentType.greedyString())
                                        .suggests(this::suggestSection)
                                        .executes(this::executeWithArgs)
                        )
        );
    }

    private int executeNoArgs(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        for (var s : constructHelpMessage(sender))
            sender.sendMessage(MessageUtils.prefixes(sender, s));

        return 1;
    }

    private int executeWithArgs(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        var sectionName = StringArgumentType.getString(context, "section");
        var section = commandSections.stream()
                .filter(s -> s.getCommandBaseName().equalsIgnoreCase(sectionName)).findFirst().orElse(null);

        if (section != null)
        {
            for (var s : constructSectionMessage(sender, section))
                sender.sendMessage(MessageUtils.prefixes(sender, s));
        }
        else
            sender.sendMessage(MessageUtils.prefixes(sender, HelpStrings.sectionNotFoundString().withLocale(MessageUtils.getLocale(sender))));

        return 1;
    }

    private CompletableFuture<Suggestions> suggestSection(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var baseName = suggestionsBuilder.getRemainingLowerCase();

        var matchedSections = commandSections.stream()
                .filter(s -> s.getCommandBaseName().toLowerCase().startsWith(baseName.toLowerCase())).toList();

        for (var s : matchedSections)
            suggestionsBuilder.suggest(s.getCommandBaseName());

        return suggestionsBuilder.buildFuture();
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.helpDescription();
    }
}

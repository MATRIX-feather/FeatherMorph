package xyz.nifeather.morph.commands.subcommands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.utilities.BindableUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class OptionSubCommands
{
    protected abstract static class BasicOptionCommand<T> extends BrigadierCommand
    {
        protected final String name;
        protected final MorphConfigManager config;
        protected final ConfigOption option;

        protected BasicOptionCommand(String name, MorphConfigManager configManager, ConfigOption option)
        {
            this.name = name;
            this.config = configManager;
            this.option = option;
        }

        protected void setConfig(CommandSender sender, T value)
        {
            config.set(option, value);

            sender.sendMessage(MessageUtils.prefixes(sender,
                    CommandStrings.optionSetString()
                            .withLocale(MessageUtils.getLocale(sender))
                            .resolve("what", name)
                            .resolve("value", value + "")));
        }

        protected void lookupConfig(CommandSender sender, Class<?> type)
        {
            sender.sendMessage(MessageUtils.prefixes(sender,
                    CommandStrings.optionValueString()
                            .withLocale(MessageUtils.getLocale(sender))
                            .resolve("what", name)
                            .resolve("value", config.get(type, option) + "")));
        }

        @Override
        public String name()
        {
            return name;
        }
    }

    public static class LimiterStringListOptionCommand extends BasicOptionCommand<String>
    {
        private final List<String> knownValues;

        public LimiterStringListOptionCommand(String name,
                                            MorphConfigManager configManager,
                                            ConfigOption option,
                                            List<String> knownValues)
        {
            super(name, configManager, option);

            this.knownValues = knownValues;
        }

        @Override
        public String getPermissionRequirement()
        {
            return null;
        }

        @Override
        public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
        {
            parentBuilder.then(
                    Commands.literal(name())
                            .executes(this::executesNoArg)
                            .then(
                                    Commands.argument("value", StringArgumentType.greedyString())
                                            .executes(this::execSetConfig)
                                            .suggests(this::suggests)
                            )
            );

            super.registerAsChild(parentBuilder);
        }

        public int executesNoArg(CommandContext<CommandSourceStack> context)
        {
            lookupConfig(context.getSource().getSender(), String.class);
            return 0;
        }

        private int execSetConfig(CommandContext<CommandSourceStack> context)
        {
            var input = StringArgumentType.getString(context, "value");
            var target = knownValues.stream()
                    .filter(str -> str.equalsIgnoreCase(input))
                    .findFirst()
                    .orElse(null);

            if (target == null)
            {
                return 1;
            }

            setConfig(context.getSource().getSender(), target);
            return 1;
        }

        public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
        {
            return CompletableFuture.supplyAsync(() ->
            {
                var input = suggestionsBuilder.getRemainingLowerCase();

                this.knownValues.forEach(str ->
                {
                    var lowerCase = str.toLowerCase();
                    if (lowerCase.contains(input))
                        suggestionsBuilder.suggest(str);
                });

                return suggestionsBuilder.build();
            });
        }

        @Override
        public FormattableMessage getHelpMessage()
        {
            return null;
        }
    }

    public static class StringListOptionBaseCommand extends BasicOptionCommand<List<String>>
    {
        public StringListOptionBaseCommand(String name, MorphConfigManager configManager, ConfigOption option)
        {
            super(name, configManager, option);
        }

        @Override
        public String getPermissionRequirement()
        {
            return null;
        }

        @Override
        public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
        {
            var operationList = new OperationListCommand(config, option, name);
            var operationAdd = new OperationAddCommand(config, option, name);
            var operationRemove = new OperationRemoveCommand(config, option, name);

            var thisBuilder = Commands.literal(name());

            operationList.registerAsChild(thisBuilder);
            operationAdd.registerAsChild(thisBuilder);
            operationRemove.registerAsChild(thisBuilder);

            parentBuilder.then(thisBuilder);
        }


        @Override
        public FormattableMessage getHelpMessage()
        {
            return null;
        }
    }

    private abstract static class OperationCommand extends BrigadierCommand
    {
        protected final MorphConfigManager configManager;
        protected final ConfigOption configOption;
        protected final String optionName;

        public OperationCommand(MorphConfigManager configManager, ConfigOption option, String optionName)
        {
            this.configManager = configManager;
            this.configOption = option;
            this.optionName = optionName;
        }

    }

    protected static class OperationRemoveCommand extends OperationCommand
    {
        public OperationRemoveCommand(MorphConfigManager configManager, ConfigOption option, String optionName)
        {
            super(configManager, option, optionName);
        }

        @Override
        public String getPermissionRequirement()
        {
            return null;
        }

        @Override
        public String name()
        {
            return "remove";
        }

        @Override
        public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
        {
            parentBuilder.then(
                    Commands.literal(name())
                            .then(
                                    Commands.argument("value", StringArgumentType.greedyString())
                                            .executes(this::executes)
                            )
            );

            super.registerAsChild(parentBuilder);
        }

        public int executes(CommandContext<CommandSourceStack> context)
        {
            var bindableList = configManager.getBindableList(String.class, configOption);
            var sender = context.getSource().getSender();
            var value = StringArgumentType.getString(context, "value");

            var listChanged = bindableList.remove(value);

            if (listChanged)
            {
                sender.sendMessage(MessageUtils.prefixes(sender,
                        CommandStrings.listRemoveSuccess()
                                .withLocale(MessageUtils.getLocale(sender))
                                .resolve("value", value)
                                .resolve("option", optionName)));
            }
            else
            {
                sender.sendMessage(MessageUtils.prefixes(sender,
                        CommandStrings.listRemoveFailUnknown()
                                .withLocale(MessageUtils.getLocale(sender))
                                .resolve("value", value)
                                .resolve("option", optionName)));
            }

            return 1;
        }

        @Override
        public FormattableMessage getHelpMessage()
        {
            return null;
        }
    }

    protected static class OperationAddCommand extends OperationCommand
    {
        public OperationAddCommand(MorphConfigManager configManager, ConfigOption option, String optionName)
        {
            super(configManager, option, optionName);
        }

        @Override
        public String getPermissionRequirement()
        {
            return null;
        }

        @Override
        public String name()
        {
            return "add";
        }

        @Override
        public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
        {
            parentBuilder.then(
                    Commands.literal(name())
                            .then(
                                    Commands.argument("value", StringArgumentType.greedyString())
                                            .executes(this::executes)
                            )
            );

            super.registerAsChild(parentBuilder);
        }

        public int executes(CommandContext<CommandSourceStack> context)
        {
            var bindableList = configManager.getBindableList(String.class, configOption);
            var sender = context.getSource().getSender();
            var value = StringArgumentType.getString(context, "value");

            try
            {
                bindableList.add(value);

                //workaround: List的add方法传入非null时永远返回true
                if (bindableList.contains(value))
                {
                    sender.sendMessage(MessageUtils.prefixes(sender,
                            CommandStrings.listAddSuccess()
                                    .withLocale(MessageUtils.getLocale(sender))
                                    .resolve("value", value)
                                    .resolve("option", optionName)));
                }
                else
                {
                    sender.sendMessage(MessageUtils.prefixes(sender,
                            CommandStrings.listAddFailUnknown()
                                    .withLocale(MessageUtils.getLocale(sender))
                                    .resolve("value", value)
                                    .resolve("option", optionName)));
                }
            }
            catch (Throwable t)
            {
                sender.sendMessage(MessageUtils.prefixes(sender,
                        CommandStrings.listAddFailUnknown()
                                .withLocale(MessageUtils.getLocale(sender))
                                .resolve("value", value)
                                .resolve("option", optionName)));

                logger.error("Error adding option to bindable list: " + t.getMessage());
            }

            return 0;
        }

        @Override
        public FormattableMessage getHelpMessage()
        {
            return null;
        }
    }

    protected static class OperationListCommand extends OperationCommand
    {
        public OperationListCommand(MorphConfigManager configManager, ConfigOption option, String optionName)
        {
            super(configManager, option, optionName);
        }

        @Override
        public String getPermissionRequirement()
        {
            return null;
        }

        @Override
        public String name()
        {
            return "list";
        }

        @Override
        public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
        {
            parentBuilder.then(
                    Commands.literal(name())
                            .executes(this::executes)
            );

            super.registerAsChild(parentBuilder);
        }

        public int executes(CommandContext<CommandSourceStack> context)
        {
            var bindableList = configManager.getBindableList(String.class, configOption);
            var displayValue = BindableUtils.bindableListToString(bindableList);

            var sender = context.getSource().getSender();
            sender.sendMessage(MessageUtils.prefixes(sender,
                    CommandStrings.optionValueString()
                            .withLocale(MessageUtils.getLocale(sender))
                            .resolve("what", optionName)
                            .resolve("value", displayValue)));

            return 1;
        }

        @Override
        public FormattableMessage getHelpMessage()
        {
            return null;
        }
    }

    public static class IntegerOptionCommand extends BasicOptionCommand<Integer>
    {
        protected IntegerOptionCommand(String name, MorphConfigManager configManager, ConfigOption option)
        {
            super(name, configManager, option);
        }

        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;

        public IntegerOptionCommand min(int min)
        {
            this.min = min;
            return this;
        }

        public IntegerOptionCommand max(int max)
        {
            this.max = max;
            return this;
        }

        public IntegerOptionCommand withRange(int min, int max)
        {
            this.min = min;
            this.max = max;

            return this;
        }

        @Override
        public String getPermissionRequirement()
        {
            return null;
        }

        @Override
        public String name()
        {
            return this.name;
        }

        @Override
        public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
        {
            parentBuilder.then(
                    Commands.literal(name)
                            .executes(this::executes)
                            .then(
                                    Commands.argument("value", IntegerArgumentType.integer())
                                            .executes(this::execSetConfig)
                            )
            );

            super.registerAsChild(parentBuilder);
        }

        public int executes(CommandContext<CommandSourceStack> context)
        {
            lookupConfig(context.getSource().getSender(), Integer.class);
            return 1;
        }

        private int execSetConfig(CommandContext<CommandSourceStack> context)
        {
            var sender = context.getSource().getSender();
            sender.sendMessage(MessageUtils.prefixes(sender,
                    CommandStrings.optionValueString()
                            .withLocale(MessageUtils.getLocale(sender))
                            .resolve("what", name)
                            .resolve("value", config.get(Integer.class, option) + "")));
            return 1;
        }

        @Override
        public FormattableMessage getHelpMessage()
        {
            return null;
        }
    }

    public static class BooleanOptionCommand extends BasicOptionCommand<Boolean>
    {
        public BooleanOptionCommand(String name,
                                    MorphConfigManager config,
                                    ConfigOption option)
        {
            super(name, config, option);
        }

        @Override
        public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
        {
            parentBuilder.then(
                    Commands.literal(name)
                            .executes(this::executes)
                            .then(
                                    Commands.argument("value", BoolArgumentType.bool())
                                            .suggests(this::suggests)
                                            .executes(this::execSetConfig)
                            )
            );

            super.registerAsChild(parentBuilder);
        }

        public int executes(CommandContext<CommandSourceStack> context)
        {
            lookupConfig(context.getSource().getSender(), Boolean.class);
            return 1;
        }

        private int execSetConfig(CommandContext<CommandSourceStack> context)
        {
            boolean value = BoolArgumentType.getBool(context, "value");

            this.setConfig(context.getSource().getSender(), value);
            return 1;
        }

        private final List<String> booleanValues = List.of("true", "false");

        public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
        {
            return CompletableFuture.supplyAsync(() ->
            {
                var input = suggestionsBuilder.getRemainingLowerCase();

                booleanValues.stream().filter(bv -> bv.contains(input))
                        .forEach(suggestionsBuilder::suggest);

                return suggestionsBuilder.build();
            });
        }

        @Override
        public String getPermissionRequirement()
        {
            return null;
        }

        @Override
        public FormattableMessage getHelpMessage()
        {
            return null;
        }
    }
}

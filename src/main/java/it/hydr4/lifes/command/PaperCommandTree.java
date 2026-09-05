package it.hydr4.lifes.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import it.hydr4.democracy.api.command.ArgumentDescriptor;
import it.hydr4.democracy.api.command.CommandDescriptor;
import it.hydr4.democracy.api.command.CommandMethodDescriptor;
import it.hydr4.democracy.api.command.CommandResult;
import it.hydr4.democracy.api.command.CommandSource;
import it.hydr4.democracy.api.command.GeneratedCommand;
import it.hydr4.democracy.core.command.CommandCatalog;
import it.hydr4.democracy.paper.command.PaperCommandSource;
import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.text.MessageKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Builds the Brigadier tree for a generated command and registers it through Paper directly.
 *
 * <p>The descriptor produced by the annotation processor stays the single source of truth: names,
 * arguments, permissions and suggestion providers all come from it. Only the wiring to Brigadier is
 * local, which keeps every argument in its own node so that completion replaces the word being typed
 * rather than the rest of the line.
 */
public final class PaperCommandTree {
    private final Logger logger;
    private final CommandCatalog catalog;
    private final GeneratedCommand generated;
    private final LifesRuntime runtime;

    PaperCommandTree(Logger logger, CommandCatalog catalog, GeneratedCommand generated, LifesRuntime runtime) {
        this.logger = logger;
        this.catalog = catalog;
        this.generated = generated;
        this.runtime = runtime;
    }

    /** Attaches the tree to the command lifecycle event of the owning plugin. */
    public static void register(Plugin plugin, CommandCatalog catalog, GeneratedCommand generated,
                                LifesRuntime runtime) {
        var tree = new PaperCommandTree(plugin.getLogger(), catalog, generated, runtime);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
            event -> event.registrar().register(tree.build(), tree.descriptor().description(), tree.descriptor().aliases()));
    }

    CommandDescriptor descriptor() {
        return generated.descriptor();
    }

    /** The command root as described by the generated descriptor. */
    public LiteralCommandNode<CommandSourceStack> build() {
        var descriptor = descriptor();
        var root = Commands.literal(descriptor.name());
        for (var method : descriptor.methods()) {
            if (method.path().isEmpty()) {
                // A requirement on the root would hide every subcommand from the client, so the bare
                // command is checked when it runs instead.
                root.executes(execution(method, List.of()));
                continue;
            }
            root.then(branch(method));
        }
        return root.build();
    }

    /**
     * Builds one method as a literal branch, deepest node first.
     *
     * <p>A builder that has already been handed to {@code then} keeps the children it had at that
     * moment, so every level is completed before it is attached to the level above it.
     */
    private LiteralArgumentBuilder<CommandSourceStack> branch(CommandMethodDescriptor method) {
        var arguments = method.arguments();
        RequiredArgumentBuilder<CommandSourceStack, ?> tail = null;
        for (var index = arguments.size() - 1; index >= 0; index--) {
            var node = argument(arguments.get(index), method);
            if (tail == null) {
                node.executes(execution(method, arguments));
            } else {
                node.then(tail);
            }
            tail = node;
        }
        LiteralArgumentBuilder<CommandSourceStack> branch = null;
        var path = method.path();
        for (var index = path.size() - 1; index >= 0; index--) {
            var node = Commands.literal(path.get(index)).requires(source -> allowed(method, source));
            if (branch == null) {
                if (tail == null) {
                    node.executes(execution(method, arguments));
                } else {
                    node.then(tail);
                }
            } else {
                node.then(branch);
            }
            branch = node;
        }
        return branch;
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> argument(ArgumentDescriptor argument,
                                                                   CommandMethodDescriptor method) {
        var node = Commands.argument(argument.name(), type(argument));
        node.requires(source -> allowed(method, source));
        suggestions(node, argument);
        return node;
    }

    @SuppressWarnings("unchecked")
    private static <T> ArgumentType<T> type(ArgumentDescriptor argument) {
        return (ArgumentType<T>) switch (argument.type()) {
            case STRING -> argument.remaining() ? StringArgumentType.greedyString() : StringArgumentType.word();
            case INTEGER -> IntegerArgumentType.integer();
            case LONG -> LongArgumentType.longArg();
            case DOUBLE -> DoubleArgumentType.doubleArg();
            case BOOLEAN -> BoolArgumentType.bool();
            case UUID, ENUM -> StringArgumentType.word();
        };
    }

    private <T> void suggestions(RequiredArgumentBuilder<CommandSourceStack, T> node, ArgumentDescriptor argument) {
        var key = argument.suggestionProviderType();
        if (key == null || key.isBlank()) {
            return;
        }
        node.suggests((context, builder) -> {
            var provider = catalog.findSuggestionProvider(key).orElse(null);
            var sender = context.getSource().getSender();
            if (provider == null || sender == null) {
                return CompletableFuture.completedFuture(builder.build());
            }
            try {
                for (var suggestion : provider.suggest(PaperCommandSource.wrap(sender), context.getInput(),
                    builder.getRemaining())) {
                    builder.suggest(suggestion);
                }
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "Suggestion provider " + key + " failed", exception);
            }
            return CompletableFuture.completedFuture(builder.build());
        });
    }

    private com.mojang.brigadier.Command<CommandSourceStack> execution(CommandMethodDescriptor method,
                                                                        List<ArgumentDescriptor> arguments) {
        return context -> {
            var sender = context.getSource().getSender();
            if (sender == null) {
                return 0;
            }
            if (!allowed(method, context.getSource())) {
                sender.sendMessage(runtime.messages().render(MessageKey.NO_PERMISSION));
                return 0;
            }
            var values = new ArrayList<Object>(arguments.size());
            for (var argument : arguments) {
                values.add(value(context, argument));
            }
            var source = PaperCommandSource.wrap(sender);
            generated.invoke(method.id(), source, values)
                .whenComplete((result, failure) -> deliver(source, result, failure));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        };
    }

    private static Object value(CommandContext<CommandSourceStack> context, ArgumentDescriptor argument) {
        return switch (argument.type()) {
            case STRING, UUID, ENUM -> context.getArgument(argument.name(), String.class);
            case INTEGER -> context.getArgument(argument.name(), Integer.class);
            case LONG -> context.getArgument(argument.name(), Long.class);
            case DOUBLE -> context.getArgument(argument.name(), Double.class);
            case BOOLEAN -> context.getArgument(argument.name(), Boolean.class);
        };
    }

    private static void deliver(CommandSource source, CommandResult result, Throwable failure) {
        if (failure != null || result == null) {
            source.send(Component.text("The command did not complete, check the server log."));
            return;
        }
        switch (result) {
            case CommandResult.Success success -> success.message().ifPresent(source::send);
            case CommandResult.Failure error -> error.message().ifPresent(source::send);
            case CommandResult.Silent ignored -> {
            }
        }
    }

    private static boolean allowed(CommandMethodDescriptor method, CommandSourceStack stack) {
        var sender = stack.getSender();
        if (sender == null) {
            return false;
        }
        for (var permission : method.permissions()) {
            if (!sender.hasPermission(permission)) {
                return false;
            }
        }
        return switch (method.sourceConstraint()) {
            case ANY -> true;
            case PLAYER_ONLY -> sender instanceof Player;
            case CONSOLE_ONLY -> sender instanceof ConsoleCommandSender;
        };
    }
}

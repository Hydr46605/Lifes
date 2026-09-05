package it.hydr4.lifes.command;

import it.hydr4.democracy.annotations.Argument;
import it.hydr4.democracy.annotations.Command;
import it.hydr4.democracy.annotations.DefaultExecution;
import it.hydr4.democracy.annotations.Permission;
import it.hydr4.democracy.annotations.PlayerOnly;
import it.hydr4.democracy.annotations.Subcommand;
import it.hydr4.democracy.api.command.CommandFailure;
import it.hydr4.democracy.api.command.CommandResult;
import it.hydr4.democracy.api.command.CommandSource;
import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.command.suggest.SuggestionKeys;
import it.hydr4.lifes.text.MessageKey;

/** The {@code /lives} command tree: user self-check and admin subcommands. */
@Command(name = "lives", aliases = {"vite", "lifes"}, description = "Manage permadeath lives.")
public final class LivesCommand {
    private final LifesRuntime runtime;
    private final LivesService service;

    public LivesCommand(LifesRuntime runtime, LivesService service) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.service = java.util.Objects.requireNonNull(service, "service");
    }

    @DefaultExecution
    @PlayerOnly
    @Permission("lifes.command.check.self")
    public CommandResult self(CommandSource source) {
        var id = source.playerId().orElseThrow();
        var account = service.find(id).orElseGet(() -> service.create(id, source.name()));
        return CommandResult.success(message(MessageKey.LIVES_SELF,
            "lives", account.lives(),
            "maximum", runtime.maximumLives()));
    }

    @Subcommand("check")
    @Permission("lifes.command.check.others")
    public CommandResult check(
        CommandSource source,
        @Argument(value = "player", suggestions = SuggestionKeys.ONLINE_PLAYERS) String player
    ) {
        var account = requireAccount(player);
        if (account == null) {
            return unknownPlayer(player);
        }
        return CommandResult.success(message(MessageKey.LIVES_OTHER,
            "player", account.name(),
            "lives", account.lives(),
            "maximum", runtime.maximumLives()));
    }

    @Subcommand("set")
    @Permission("lifes.command.set")
    public CommandResult set(
        CommandSource source,
        @Argument(value = "player", suggestions = SuggestionKeys.ONLINE_PLAYERS) String player,
        @Argument("amount") int amount
    ) {
        return adjust(player, LifeChangeReason.ADMIN_SET, amount);
    }

    @Subcommand("add")
    @Permission("lifes.command.add")
    public CommandResult add(
        CommandSource source,
        @Argument(value = "player", suggestions = SuggestionKeys.ONLINE_PLAYERS) String player,
        @Argument("amount") int amount
    ) {
        return adjust(player, LifeChangeReason.ADMIN_ADD, amount);
    }

    @Subcommand("remove")
    @Permission("lifes.command.remove")
    public CommandResult remove(
        CommandSource source,
        @Argument(value = "player", suggestions = SuggestionKeys.ONLINE_PLAYERS) String player,
        @Argument("amount") int amount
    ) {
        return adjust(player, LifeChangeReason.ADMIN_REMOVE, amount);
    }

    @Subcommand("reset")
    @Permission("lifes.command.reset")
    public CommandResult reset(
        CommandSource source,
        @Argument(value = "player", suggestions = SuggestionKeys.ONLINE_PLAYERS) String player
    ) {
        return adjust(player, LifeChangeReason.ADMIN_RESET, 0);
    }

    @Subcommand("reload")
    @Permission("lifes.command.reload")
    public CommandResult reload(CommandSource source) {
        try {
            runtime.reload();
        } catch (ConfigException exception) {
            return CommandResult.failure(
                CommandFailure.INTERNAL_ERROR,
                net.kyori.adventure.text.Component.text(exception.getMessage())
            );
        }
        return CommandResult.success(message(MessageKey.LIVES_RELOAD_DONE));
    }

    private CommandResult adjust(String playerName, LifeChangeReason reason, int amount) {
        var account = requireAccount(playerName);
        if (account == null) {
            return unknownPlayer(playerName);
        }
        var maximum = runtime.maximumLives();
        if (reason == LifeChangeReason.ADMIN_SET && (amount < 0 || amount > maximum)) {
            return CommandResult.failure(CommandFailure.INVALID_ARGUMENT, message(
                MessageKey.LIVES_INVALID_AMOUNT, "minimum", 0, "maximum", maximum));
        }
        if ((reason == LifeChangeReason.ADMIN_ADD || reason == LifeChangeReason.ADMIN_REMOVE) && amount <= 0) {
            return CommandResult.failure(CommandFailure.INVALID_ARGUMENT, message(
                MessageKey.LIVES_INVALID_AMOUNT, "minimum", 1, "maximum", maximum));
        }
        var change = service.adjust(account.uuid(), reason, amount);
        var key = switch (reason) {
            case ADMIN_SET -> MessageKey.LIVES_SET;
            case ADMIN_ADD -> MessageKey.LIVES_ADD;
            case ADMIN_REMOVE -> MessageKey.LIVES_REMOVE;
            case ADMIN_RESET -> MessageKey.LIVES_RESET;
            case DEATH -> throw new IllegalStateException("not an admin reason");
        };
        return CommandResult.success(message(key,
            "player", account.name(),
            "amount", amount,
            "lives", change.after().lives()));
    }

    private LivesAccount requireAccount(String playerName) {
        return service.findByName(playerName).orElse(null);
    }

    private CommandResult unknownPlayer(String playerName) {
        return CommandResult.failure(
            CommandFailure.PLAYER_NOT_FOUND,
            message(MessageKey.LIVES_UNKNOWN_PLAYER, "player", playerName)
        );
    }

    private net.kyori.adventure.text.Component message(MessageKey key, Object... pairs) {
        return runtime.messages().render(key, pairs);
    }
}

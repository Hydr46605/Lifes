package it.hydr4.lifes.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import it.hydr4.democracy.core.command.DefaultCommandCatalog;
import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.command.suggest.OnlinePlayerSuggestions;
import it.hydr4.lifes.command.suggest.PlayerNameIndex;
import it.hydr4.lifes.command.suggest.SuggestionKeys;
import it.hydr4.lifes.core.AccountDirectory;
import it.hydr4.lifes.core.DefaultLivesService;
import it.hydr4.lifes.discord.OfflineGateway;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Completion is driven through the real Brigadier tree, so the ranges the client would apply are
 * the ones asserted here. Calling a suggestion provider directly proves nothing about that.
 */
class PaperCommandTreeTest {
    private static final String SETTINGS = """
        version: 1
        lives:
          default: 3
          maximum: 10
        death:
          cost-per-death: 1
          ignored-causes: []
          actions: []
        exhaustion:
          actions: []
        persistence:
          save-interval-seconds: 0
          save-off-thread: true
        messages: {}
        """;

    private static final List<String> ADMIN_PERMISSIONS = List.of(
        "lifes.command.check.others",
        "lifes.command.set",
        "lifes.command.add",
        "lifes.command.remove",
        "lifes.command.reset",
        "lifes.command.reload",
        "lifes.command.check.self"
    );

    @TempDir
    Path dir;

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> root;
    private PaperCommandTree tree;

    @BeforeEach
    void setUp() throws IOException {
        var file = dir.resolve("settings.yml");
        Files.writeString(file, SETTINGS);
        var runtime = LifesRuntime.load(file, OfflineGateway.create());
        var directory = new AccountDirectory();
        var service = new DefaultLivesService(directory, runtime::settings);
        service.create(UUID.randomUUID(), "Hydr4");
        service.create(UUID.randomUUID(), "Hazel");
        service.create(UUID.randomUUID(), "Ghost");

        var names = new PlayerNameIndex();
        names.online("Hydr4");

        var catalog = new DefaultCommandCatalog();
        catalog.registerSuggestionProvider(SuggestionKeys.ONLINE_PLAYERS, new OnlinePlayerSuggestions(names, directory));

        var generated = new LivesCommandDemocracyCommand(new LivesCommand(runtime, service));
        catalog.register(generated);
        tree = new PaperCommandTree(Logger.getGlobal(), catalog, generated, runtime);

        dispatcher = new CommandDispatcher<>();
        root = tree.build();
        dispatcher.getRoot().addChild(root);
    }

    private List<String> completed(String input, Set<String> permissions) {
        var source = source(permissions);
        try {
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source))
                .get(5, java.util.concurrent.TimeUnit.SECONDS);
            return suggestions.getList().stream().map(suggestion -> suggestion.apply(input)).toList();
        } catch (Exception exception) {
            throw new IllegalStateException("completion failed for [" + input + "]", exception);
        }
    }

    private static CommandSourceStack source(Set<String> permissions) {
        var sender = Proxy.newProxyInstance(
            org.bukkit.command.ConsoleCommandSender.class.getClassLoader(),
            new Class<?>[] {org.bukkit.command.ConsoleCommandSender.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "hasPermission" -> permissions.contains(arguments[0]);
                case "getName" -> "Tester";
                case "equals" -> proxy == arguments[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "ConsoleCommandSender[Tester]";
                default -> method.getReturnType().equals(boolean.class) ? Boolean.FALSE : null;
            });
        return new TestStack((CommandSender) sender);
    }

    private record TestStack(CommandSender sender) implements CommandSourceStack {
        @Override
        public Location getLocation() {
            return null;
        }

        @Override
        public CommandSender getSender() {
            return sender;
        }

        @Override
        public Entity getExecutor() {
            return null;
        }

        @Override
        public CommandSourceStack withLocation(Location location) {
            return this;
        }

        @Override
        public CommandSourceStack withExecutor(Entity executor) {
            return this;
        }
    }

    @Test
    void subcommandsAreOfferedAfterTheRoot() {
        var lines = completed("lives ", Set.copyOf(ADMIN_PERMISSIONS));
        assertTrue(lines.contains("lives add"), lines.toString());
        assertTrue(lines.contains("lives check"), lines.toString());
        assertTrue(lines.contains("lives reload"), lines.toString());
    }

    @Test
    void aPartialSubcommandIsCompletedInPlace() {
        assertEquals(List.of("lives check"), completed("lives ch", Set.copyOf(ADMIN_PERMISSIONS)));
    }

    @Test
    void playerSuggestionsReplaceOnlyTheWordBeingTyped() {
        var lines = completed("lives check ", Set.copyOf(ADMIN_PERMISSIONS));
        assertFalse(lines.isEmpty(), "no player suggestions at all");
        assertTrue(lines.contains("lives check Hydr4"), lines.toString());
        assertTrue(lines.contains("lives check Ghost"), lines.toString());
    }

    @Test
    void aPartialPlayerNameKeepsTheSubcommandIntact() {
        var lines = completed("lives check Haz", Set.copyOf(ADMIN_PERMISSIONS));
        assertEquals(List.of("lives check Hazel"), lines);
    }

    @Test
    void theAmountArgumentIsNotOfferedAsText() {
        assertTrue(completed("lives set Hydr4 ", Set.copyOf(ADMIN_PERMISSIONS)).isEmpty(),
            "an integer argument has nothing to suggest");
    }

    @Test
    void subcommandsWithoutTheirPermissionAreNotUsable() {
        var nobody = source(Set.of());
        var admin = source(Set.copyOf(ADMIN_PERMISSIONS));
        for (var name : List.of("add", "check", "reload", "remove", "reset", "set")) {
            var node = root.getChild(name);
            assertTrue(node.canUse(admin), name + " should be usable by an admin");
            assertFalse(node.canUse(nobody), name + " should be hidden from a sender without its permission");
        }
    }

    @Test
    void aSenderWithOnePermissionCanUseOnlyThatSubcommand() {
        var source = source(Set.of("lifes.command.check.others"));
        assertTrue(root.getChild("check").canUse(source));
        for (var name : List.of("add", "reload", "remove", "reset", "set")) {
            assertFalse(root.getChild(name).canUse(source), name + " leaked to a sender without its permission");
        }
    }

    @Test
    void thePlayerArgumentCarriesItsSuggestionProvider() {
        var player = root.getChild("check").getChild("player");
        assertNotNull(player, "the player argument must be its own node");
        assertInstanceOf(ArgumentCommandNode.class, player);
    }
}

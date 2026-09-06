package it.hydr4.lifes.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.democracy.api.command.CommandFailure;
import it.hydr4.democracy.api.command.CommandResult;
import it.hydr4.democracy.testkit.CommandTestHarness;
import it.hydr4.democracy.testkit.CommandTestSource;
import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.core.AccountDirectory;
import it.hydr4.lifes.core.DefaultLivesService;
import it.hydr4.lifes.discord.OfflineGateway;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LivesCommandTest {
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

    @TempDir
    Path dir;

    private CommandTestHarness harness;
    private CommandTestSource player;
    private CommandTestSource admin;
    private DefaultLivesService service;
    private AccountDirectory directory;

    @BeforeEach
    void setUp() throws IOException {
        var file = dir.resolve("settings.yml");
        Files.writeString(file, SETTINGS);
        var runtime = LifesRuntime.load(file, OfflineGateway.create());
        directory = new AccountDirectory();
        service = new DefaultLivesService(directory, runtime::settings);
        harness = new CommandTestHarness();
        harness.register(new LivesCommandDemocracyCommand(new LivesCommand(runtime, service)));
        player = CommandTestSource.player(UUID.randomUUID(), "Guest").grant("lifes.command.check.self");
        admin = CommandTestSource.console();
        for (var permission : new String[] {
            "lifes.command.check.others",
            "lifes.command.set",
            "lifes.command.add",
            "lifes.command.remove",
            "lifes.command.reset",
            "lifes.command.reload",
        }) {
            admin.grant(permission);
        }
    }

    private static String plain(CommandResult result) {
        return switch (result) {
            case CommandResult.Success success -> success.message()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .orElse("");
            case CommandResult.Failure failure -> failure.message()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .orElse("");
            case CommandResult.Silent silent -> "";
        };
    }

    @Test
    void selfCheckCreatesTheAccountLazily() {
        var result = harness.execute(player, "lives");
        assertInstanceOf(CommandResult.Success.class, result);
        assertTrue(plain(result).contains("You have 3 lives"), plain(result));
        assertEquals(1, directory.size());
    }

    @Test
    void selfCheckNeedsThePlayerOnlyPermission() {
        var stripped = CommandTestSource.player(UUID.randomUUID(), "Bare");
        var result = harness.execute(stripped, "lives");
        assertInstanceOf(CommandResult.Failure.class, result);
        assertEquals(CommandFailure.NO_PERMISSION, ((CommandResult.Failure) result).type());
    }

    @Test
    void checkOnUnknownAccountFailsWithPlayerNotFound() {
        var result = harness.execute(admin, "lives check Ghost");
        assertInstanceOf(CommandResult.Failure.class, result);
        assertEquals(CommandFailure.PLAYER_NOT_FOUND, ((CommandResult.Failure) result).type());
    }

    @Test
    void setChangesLives() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        assertInstanceOf(CommandResult.Success.class, harness.execute(admin, "lives set Hydr4 8"));
        assertEquals(8, service.find(id).orElseThrow().lives());
    }

    @Test
    void setRejectsOutOfRangeAmounts() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var result = harness.execute(admin, "lives set Hydr4 11");
        assertInstanceOf(CommandResult.Failure.class, result);
        assertEquals(CommandFailure.INVALID_ARGUMENT, ((CommandResult.Failure) result).type());
        assertEquals(3, service.find(id).orElseThrow().lives());
    }

    @Test
    void addAndRemoveChangeLiveCounts() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        harness.execute(admin, "lives add Hydr4 2");
        assertEquals(5, service.find(id).orElseThrow().lives());
        harness.execute(admin, "lives remove Hydr4 5");
        assertEquals(0, service.find(id).orElseThrow().lives());
    }

    @Test
    void removeRejectsNonPositiveAmounts() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var result = harness.execute(admin, "lives remove Hydr4 0");
        assertEquals(CommandFailure.INVALID_ARGUMENT, ((CommandResult.Failure) result).type());
    }

    @Test
    void resetRestoresTheDefaultAmount() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        harness.execute(admin, "lives add Hydr4 4");
        harness.execute(admin, "lives reset Hydr4");
        assertEquals(3, service.find(id).orElseThrow().lives());
    }

    @Test
    void reloadReportsFailureWithTheParserMessage() throws IOException {
        Files.writeString(dir.resolve("settings.yml"), "version: 9");
        var result = harness.execute(admin, "lives reload");
        assertInstanceOf(CommandResult.Failure.class, result);
        assertTrue(plain(result).contains("version"), plain(result));
    }

    @Test
    void reloadSucceedsOnAValidFile() {
        var result = harness.execute(admin, "lives reload");
        assertInstanceOf(CommandResult.Success.class, result);
    }
}

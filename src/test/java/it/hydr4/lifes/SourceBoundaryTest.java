package it.hydr4.lifes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Architecture guard: Bukkit and Paper stay out of the domain layers.
 * MiniMessage is allowed only in {@code text}; the rest of the domain is
 * pure Java.
 */
class SourceBoundaryTest {
    private static final List<String> EVERYWHERE = List.of("api", "core", "config", "persistence", "text");
    private static final List<String> BUKKIT = List.of("org.bukkit", "io.papermc");
    private static final Map<String, List<String>> BANS = Map.of(
        "api", BUKKIT,
        "core", BUKKIT,
        "config", BUKKIT,
        "persistence", BUKKIT,
        "text", List.of("org.bukkit", "io.papermc", "me.clip")
    );

    @Test
    void domainPackagesStayPlatformFree() throws IOException {
        var roots = Path.of("src", "main", "java", "it", "hydr4", "lifes");
        assertTrue(Files.isDirectory(roots), "run from the project root");
        for (var pkg : EVERYWHERE) {
            try (Stream<Path> sources = Files.walk(roots.resolve(pkg))) {
                for (var file : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var content = Files.readString(file);
                    for (var banned : BANS.get(pkg)) {
                        assertTrue(!content.contains(banned), file + " must not reference " + banned);
                    }
                }
            }
        }
    }

    @Test
    void suggestionProvidersMustNotTouchBukkit() throws IOException {
        // Paper resolves Brigadier suggestions off the main thread. A provider that reads the
        // server API either throws there or, worse, has its failure swallowed into an empty list.
        var root = Path.of("src", "main", "java", "it", "hydr4", "lifes", "command", "suggest");
        assertTrue(Files.isDirectory(root), "run from the project root");
        try (Stream<Path> sources = Files.walk(root)) {
            for (var file : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                var content = Files.readString(file);
                for (var banned : BUKKIT) {
                    assertTrue(!content.contains(banned), file + " must not reference " + banned);
                }
            }
        }
    }

    @Test
    void discordDeliveryMustNotTouchBukkit() throws IOException {
        // Discord delivery happens on a worker thread. If that thread ever read a Player it would
        // either throw or corrupt state, so the action hands over an immutable snapshot and nothing
        // below it is allowed to reach back into the server API.
        var root = Path.of("src", "main", "java", "it", "hydr4", "lifes", "discord");
        assertTrue(Files.isDirectory(root), "run from the project root");
        try (Stream<Path> sources = Files.walk(root)) {
            for (var file : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                var content = Files.readString(file);
                for (var banned : BUKKIT) {
                    assertTrue(!content.contains(banned), file + " must not reference " + banned);
                }
            }
        }
    }
}

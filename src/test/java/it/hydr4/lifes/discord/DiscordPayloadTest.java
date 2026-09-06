package it.hydr4.lifes.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import it.hydr4.lifes.ConfigException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiscordPayloadTest {
    private static final Gson JSON = new Gson();
    private static final String PATH = "death.actions[0].payload";

    @Test
    void malformedJsonFailsWithThePath() {
        var exception = assertThrows(ConfigException.class,
            () -> DiscordPayload.compile("{oops", PATH));
        assertTrue(exception.getMessage().startsWith(PATH + ": is not valid JSON"),
            exception.getMessage());
    }

    @Test
    void arraysFailWithThePath() {
        var exception = assertThrows(ConfigException.class,
            () -> DiscordPayload.compile("[1, 2]", PATH));
        assertEquals(PATH + ": expected a JSON object, found a JSON array", exception.getMessage());
    }

    @Test
    void scalarsFailWithThePath() {
        var exception = assertThrows(ConfigException.class,
            () -> DiscordPayload.compile("\"just a string\"", PATH));
        assertEquals(PATH + ": expected a JSON object, found a JSON scalar", exception.getMessage());
    }

    @Test
    void blankTemplatesFailWithThePath() {
        var exception = assertThrows(ConfigException.class,
            () -> DiscordPayload.compile("   ", PATH));
        assertEquals(PATH + ": expected a JSON object, got empty text", exception.getMessage());
    }

    @Test
    void placeholdersAreSubstitutedEverywhereIncludingUrls() {
        var payload = DiscordPayload.compile(
            "{\"content\": \"{player} died\", \"avatar_url\": \"https://example.com/{uuid}.png\"}",
            PATH);
        var body = payload.render(Map.of("player", "Steve", "uuid", "some-id"));
        assertEquals("{\"content\": \"Steve died\", \"avatar_url\": \"https://example.com/some-id.png\"}", body);
    }

    @Test
    void valuesCannotBreakOutOfTheirString() {
        var payload = DiscordPayload.compile("{\"content\": \"{player} died\"}", PATH);
        var body = payload.render(Map.of("player", "a\"b\\c\nd"));
        var parsed = JSON.fromJson(body, com.google.gson.JsonObject.class);
        assertEquals("a\"b\\c\nd died", parsed.get("content").getAsString());
    }

    @Test
    void untouchedTextIsByteIdentical() {
        var template = "{\"username\": \"Lifes\", \"flags\": 32768, \"content\": \"plain\"}";
        var payload = DiscordPayload.compile(template, PATH);
        assertEquals(template, payload.render(Map.of()));
    }
}

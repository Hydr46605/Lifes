package it.hydr4.lifes.discord;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Posts prepared payloads with the JDK client, which is why this feature adds no dependency. */
public final class HttpDiscordTransport implements DiscordTransport {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String AGENT = "Lifes (Minecraft Paper plugin)";

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        // A redirect would move the payload, and with it the token, to another host.
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    @Override
    public Response post(WebhookEndpoint endpoint, String body) throws Failure {
        var request = HttpRequest.newBuilder(URI.create(endpoint.url()))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .header("User-Agent", AGENT)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            var content = response.body() == null ? "" : response.body();
            return new Response(response.statusCode(), content);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Failure("interrupted while posting to Discord", exception);
        } catch (IOException exception) {
            throw new Failure("could not reach Discord: " + exception.getMessage(), exception);
        }
    }
}

package it.hydr4.lifes.discord;

/** Hands a prepared payload to Discord. Kept as an interface so delivery is testable offline. */
public interface DiscordTransport {
    /** Posts the body and reports the response, or fails when Discord was never reached. */
    Response post(WebhookEndpoint endpoint, String body) throws Failure;

    /**
     * The reply Discord sent.
     *
     * @param status HTTP status code
     * @param body   response body, empty for the 204 a successful webhook send returns
     */
    record Response(int status, String body) {
        public Response {
            java.util.Objects.requireNonNull(body, "body");
        }

        public boolean successful() {
            return status >= 200 && status < 300;
        }

        /** Rate limits and server faults can resolve; a rejected payload never will. */
        public boolean retryable() {
            return status == 429 || status >= 500;
        }
    }

    /** A delivery that never reached Discord: name resolution, TLS, timeout or interruption. */
    final class Failure extends Exception {
        private static final long serialVersionUID = 1L;

        public Failure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package it.hydr4.lifes.paper;

import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.text.MessageKey;
import it.hydr4.lifes.text.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Enforces the zero-lives state when an already finished account connects.
 *
 * <p>The exit pipeline normally fires on the transition into zero lives. An account that is still
 * at zero lives when it joins (a lifted ban, or a crash between the save and the ban) produces no
 * transition, so this guard applies the configured {@code exhaustion.on-zero-lives-join} policy.
 */
public final class ZeroLivesGuard implements Listener {
    private final LivesService service;
    private final Supplier<LivesSettings> settings;
    private final Supplier<Messages> messages;
    private final Consumer<LivesAccount> exhaustion;

    public ZeroLivesGuard(
        LivesService service,
        Supplier<LivesSettings> settings,
        Supplier<Messages> messages,
        Consumer<LivesAccount> exhaustion
    ) {
        this.service = Objects.requireNonNull(service, "service");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.exhaustion = Objects.requireNonNull(exhaustion, "exhaustion");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var account = service.find(player.getUniqueId()).orElse(null);
        if (account == null || account.lives() > 0) {
            return;
        }
        switch (settings.get().zeroLivesJoin()) {
            case IGNORE -> {
                // The admin opted out; the account keeps playing at zero lives.
            }
            case REAPPLY -> exhaustion.accept(account);
            case KICK -> player.kick(messages.get().render(MessageKey.LIVES_EXHAUSTED_KICK));
        }
    }
}

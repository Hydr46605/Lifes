package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Static entry point for Skript scripts (via skript-reflect) and other integrations.
 *
 * <p>Kept free of any Skript API types on purpose: the plugin never depends on Skript at
 * compile time, so this works with every Skript version and can never break the build when
 * Skript is absent. {@link SkriptHook} publishes the running {@link LivesService} here on
 * enable and clears it on disable. Usage examples live in {@code docs/skript.md}.
 */
public final class LifesSkript {
    private static final AtomicReference<LivesService> SERVICE = new AtomicReference<>();
    private static final AtomicReference<Supplier<Collection<LivesAccount>>> ACCOUNTS =
        new AtomicReference<>(List::of);
    private static final AtomicReference<Supplier<PlaceholderResolver>> PLACEHOLDERS = new AtomicReference<>();
    private static final AtomicReference<UltimateUiHook> UI = new AtomicReference<>();

    private LifesSkript() {
    }

    static void publish(LivesService service) {
        SERVICE.set(service);
    }

    static void unpublish() {
        SERVICE.set(null);
        ACCOUNTS.set(List::of);
        PLACEHOLDERS.set(null);
        UI.set(null);
    }

    static void install(SkriptContext context) {
        ACCOUNTS.set(context.accounts());
        PLACEHOLDERS.set(context.placeholders());
        UI.set(context.uiOrNull());
    }

    /** The live service, or empty when the plugin is disabled. */
    public static Optional<LivesService> service() {
        return Optional.ofNullable(SERVICE.get());
    }

    /** Lives of an account by UUID, or empty when unknown. */
    public static Optional<Integer> livesOf(UUID id) {
        return service().flatMap(service -> service.find(id)).map(LivesAccount::lives);
    }

    /** Lives of an account by last known name, or empty when unknown. */
    public static Optional<Integer> livesOfName(String name) {
        return service().flatMap(service -> service.findByName(name)).map(LivesAccount::lives);
    }

    /** Full account snapshot by UUID, or empty when unknown. */
    public static Optional<LivesAccount> accountOf(UUID id) {
        return service().flatMap(service -> service.find(id));
    }

    /** Creates an account; fails when it already exists, mirroring the Java API. */
    public static LivesAccount create(UUID id, String name) {
        return requireService().create(id, name);
    }

    /** Records one death with the configured cost rules; creates the account on first death. */
    public static LifeChange applyDeath(UUID id, String name, int cost) {
        return requireService().applyDeath(id, name, cost);
    }

    /**
     * Adjusts an account from a Skript-friendly reason name (for example {@code "ADMIN_ADD"}).
     * Unknown accounts and unknown reason names throw, mirroring the Java API.
     */
    public static void adjust(UUID id, String reasonName, int amount) {
        requireService().adjust(id, LifeChangeReason.valueOf(reasonName), amount);
    }

    /** Number of exhausted (dead) accounts. */
    public static long deadCount() {
        return DeadAccounts.count(ACCOUNTS.get());
    }

    /** Dead account names A-Z, first page ({@value DeadAccounts#DEFAULT_LIMIT} entries). */
    public static List<String> deadNames() {
        return deadNames(DeadAccounts.DEFAULT_LIMIT);
    }

    /** Dead account names A-Z, capped at {@code limit} (1..100, plus a lookahead slot). */
    public static List<String> deadNames(int limit) {
        return DeadAccounts.names(ACCOUNTS.get(), limit);
    }

    /**
     * Resolves any Lifes placeholder ({@code lives}, {@code dead_count},
     * {@code dead_list_10_comma}, ...) for an account, or globally when {@code id} is null.
     */
    public static String placeholder(String params, UUID id) {
        var placeholders = PLACEHOLDERS.get();
        if (placeholders == null) {
            throw new IllegalStateException("Lifes placeholders are not installed");
        }
        var value = placeholders.get().resolve(id, params);
        return value == null ? "" : value;
    }

    /** Opens an UltimateUI GUI for an online player; false when UI or player is missing. */
    public static boolean openGui(UUID id, String gui, boolean hud, boolean autoclose) {
        var player = player(id);
        return player != null && requireUi().openGui(player, gui, hud, autoclose);
    }

    /** Closes an online player's GUI (or the named one); false when UI or player is missing. */
    public static boolean closeGui(UUID id, String guiOrNull) {
        var player = player(id);
        return player != null && requireUi().closeGui(player, guiOrNull);
    }

    /** Sets a text element of an online player's open GUI; false when UI or player is missing. */
    public static boolean setElement(UUID id, String element, String text) {
        var player = player(id);
        return player != null && requireUi().setElementText(player, element, text);
    }

    private static LivesService requireService() {
        var service = SERVICE.get();
        if (service == null) {
            throw new IllegalStateException("Lifes is not enabled");
        }
        return service;
    }

    private static UltimateUiHook requireUi() {
        var ui = UI.get();
        if (ui == null) {
            throw new IllegalStateException("UltimateUI is not attached");
        }
        return ui;
    }

    private static org.bukkit.entity.Player player(UUID id) {
        return id == null ? null : org.bukkit.Bukkit.getPlayer(id);
    }
}

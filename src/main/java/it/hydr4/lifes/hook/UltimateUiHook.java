package it.hydr4.lifes.hook;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional UltimateUI attachment, reflection-only and version-pinned.
 *
 * <p>Investigation (2026-09-07, on the provided premium build in the Permadeath
 * server {@code plugins/} folder):
 *
 * <ul>
 *   <li>The embedded {@code plugin.yml} declares
 *   {@code version: 1.2.1}, {@code main: dev.xqedii.ultimateUI.UltimateUI} — the version
 *   string is unreliable, so gating is by plugin name plus API shape, never by version.</li>
 *   <li>Public Java API (package {@code dev.xqedii.ultimateUI.api}): {@code UltimateUIAPI}
 *   (singleton {@code get()}/guard {@code isAvailable()}, {@code openGui/openGuiHud/closeGui},
 *   {@code setElementText/setElementValue}, {@code getOpenGuiName}, {@code getSession},
 *   {@code listGuis/guiExists}), {@code UiBuilder}, {@code UiSession}.</li>
 *   <li>All third-party classes are shaded ({@code io/}, {@code net/kyori/}, {@code org/},
 *   {@code xqedii.ultimateui.libs}); only {@code dev.xqedii.ultimateUI.**} is referenced.</li>
 * </ul>
 *
 * <p>Only this exact API shape is accepted: {@code UltimateUIAPI.get()} returning an object
 * with {@code openGui(Player, String)}, {@code closeGui(Player)}, {@code setElementText(Player,
 * String, String)} and {@code getOpenGuiName(Player)}. Any missing method refuses the hook
 * with a warning and leaves Lifes fully functional. No UltimateUI type is referenced at
 * compile time, so no {@code NoClassDefFoundError} is possible when it is absent.
 */
public final class UltimateUiHook implements AutoCloseable {
    private static final String PLUGIN_NAME = "UltimateUI";
    private static final String API_CLASS = "dev.xqedii.ultimateUI.api.UltimateUIAPI";

    private final Object api;
    private final Method setElementText;
    private final Method getOpenGuiName;

    private UltimateUiHook(Object api, Method setElementText, Method getOpenGuiName) {
        this.api = api;
        this.setElementText = setElementText;
        this.getOpenGuiName = getOpenGuiName;
    }

    /** Attaches to this exact UltimateUI build, or returns empty when absent or incompatible. */
    public static Optional<UltimateUiHook> tryAttach(Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin(PLUGIN_NAME) == null) {
            return Optional.empty();
        }
        Logger logger = plugin.getLogger();
        try {
            Class<?> apiClass = Class.forName(API_CLASS, false, UltimateUiHook.class.getClassLoader());
            Method get = apiClass.getMethod("get");
            Object api = get.invoke(null);
            Method openGui = apiClass.getMethod("openGui", Player.class, String.class);
            Method closeGui = apiClass.getMethod("closeGui", Player.class);
            Method setElementText = apiClass.getMethod(
                "setElementText", Player.class, String.class, String.class);
            Method getOpenGuiName = apiClass.getMethod("getOpenGuiName", Player.class);
            if (openGui.getReturnType() != boolean.class || closeGui.getReturnType() != void.class) {
                throw new NoSuchMethodException("UltimateUIAPI shape mismatch (open/close signatures)");
            }
            logger.info("UltimateUI hooks registered (this-version API shape verified).");
            return Optional.of(new UltimateUiHook(api, setElementText, getOpenGuiName));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING,
                "UltimateUI found but its API shape differs from the pinned build; UI hooks stay disabled.",
                exception);
            return Optional.empty();
        }
    }

    /** Refreshes a text element of the player's open GUI; false when no GUI is open or on error. */
    public boolean refreshText(Player player, String page, String text) {
        try {
            Object open = getOpenGuiName.invoke(api, player);
            if (open == null) {
                return false;
            }
            return (boolean) setElementText.invoke(api, player, page, text);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void close() {
    }
}

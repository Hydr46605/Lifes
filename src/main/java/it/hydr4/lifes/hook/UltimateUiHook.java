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
 * <p>Pinned against the investigated premium build:
 *
 * <ul>
 *   <li>Embedded {@code plugin.yml} declares {@code version: 1.2.1},
 *   {@code main: dev.xqedii.ultimateUI.UltimateUI} — the version
 *   string is unreliable, so gating is by plugin name plus API shape, never by version.</li>
 *   <li>Public Java API (package {@code dev.xqedii.ultimateUI.api}): {@code UltimateUIAPI}
 *   (singleton {@code get()}/guard {@code isAvailable()}, {@code openGui/openGuiHud/closeGui},
 *   {@code setElementText/setElementValue}, {@code getOpenGuiName}, {@code getSession},
 *   {@code listGuis/guiExists}), {@code UiBuilder}, {@code UiSession}.</li>
 *   <li>All third-party classes are shaded ({@code io/}, {@code net/kyori/}, {@code org/},
 *   {@code xqedii.ultimateui.libs}); only {@code dev.xqedii.ultimateUI.**} is referenced.</li>
 *   <li>{@code setElementText(Player, String, String)} sits next to element-targeted siblings
 *   ({@code setElementColor/Position/Scale/Item} take one element id), so the two strings are
 *   read as {@code (element, text)}. If a future build documents them otherwise, only the
 *   call sites here change.</li>
 * </ul>
 *
 * <p>Only this exact API shape is accepted: {@code UltimateUIAPI.get()} returning an object
 * with {@code openGui(Player, String, boolean, boolean)}, {@code closeGui(Player)},
 * {@code closeGui(Player, String)}, {@code setElementText(Player, String, String)} and
 * {@code getOpenGuiName(Player)}. Any missing method refuses the hook with a warning and
 * leaves Lifes fully functional. No UltimateUI type is referenced at compile time, so no
 * {@code NoClassDefFoundError} is possible when it is absent.
 */
public final class UltimateUiHook implements AutoCloseable {
    private static final String PLUGIN_NAME = "UltimateUI";
    private static final String API_CLASS = "dev.xqedii.ultimateUI.api.UltimateUIAPI";

    private final Object api;
    private final Method openGui;
    private final Method closeGui;
    private final Method closeNamedGui;
    private final Method setElementText;
    private final Method getOpenGuiName;

    private UltimateUiHook(Object api, Method openGui, Method closeGui, Method closeNamedGui,
        Method setElementText, Method getOpenGuiName) {
        this.api = api;
        this.openGui = openGui;
        this.closeGui = closeGui;
        this.closeNamedGui = closeNamedGui;
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
            Method openGui = apiClass.getMethod("openGui", Player.class, String.class, boolean.class, boolean.class);
            Method closeGui = apiClass.getMethod("closeGui", Player.class);
            Method closeNamedGui = apiClass.getMethod("closeGui", Player.class, String.class);
            Method setElementText = apiClass.getMethod(
                "setElementText", Player.class, String.class, String.class);
            Method getOpenGuiName = apiClass.getMethod("getOpenGuiName", Player.class);
            if (openGui.getReturnType() != boolean.class
                || closeGui.getReturnType() != void.class
                || closeNamedGui.getReturnType() != boolean.class
                || setElementText.getReturnType() != boolean.class) {
                throw new NoSuchMethodException("UltimateUIAPI shape mismatch (open/close/set signatures)");
            }
            logger.info("UltimateUI hooks registered (this-version API shape verified).");
            return Optional.of(new UltimateUiHook(api, openGui, closeGui, closeNamedGui, setElementText, getOpenGuiName));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING,
                "UltimateUI found but its API shape differs from the pinned build; UI hooks stay disabled.",
                exception);
            return Optional.empty();
        }
    }

    /** Opens a GUI for the player; false on reflection errors. */
    public boolean openGui(Player player, String gui, boolean hud, boolean autoclose) {
        try {
            return (boolean) openGui.invoke(api, player, gui, hud, autoclose);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    /** Closes the player's GUI (or the named one when given); false on reflection errors. */
    public boolean closeGui(Player player, String guiOrNull) {
        try {
            if (guiOrNull == null) {
                closeGui.invoke(api, player);
                return true;
            }
            return (boolean) closeNamedGui.invoke(api, player, guiOrNull);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    /** Sets a text element of the player's open GUI; false when no GUI is open or on error. */
    public boolean setElementText(Player player, String element, String text) {
        try {
            Object open = getOpenGuiName.invoke(api, player);
            if (open == null) {
                return false;
            }
            return (boolean) setElementText.invoke(api, player, element, text);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    /** Sets a text element only when the named GUI is the open one; false otherwise. */
    public boolean setElementTextIfGui(Player player, String gui, String element, String text) {
        try {
            Object open = getOpenGuiName.invoke(api, player);
            if (open == null || !gui.equals(String.valueOf(open))) {
                return false;
            }
            return (boolean) setElementText.invoke(api, player, element, text);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void close() {
    }
}

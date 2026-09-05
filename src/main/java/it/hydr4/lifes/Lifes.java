package it.hydr4.lifes;

import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.command.CommandWiring;
import it.hydr4.lifes.core.AccountDirectory;
import it.hydr4.lifes.core.DefaultLivesService;
import it.hydr4.lifes.death.ActionRunner;
import it.hydr4.lifes.hook.PlaceholderApiHook;
import it.hydr4.lifes.paper.AdminChangeNotifier;
import it.hydr4.lifes.paper.BukkitEventBridge;
import it.hydr4.lifes.paper.DeathListener;
import it.hydr4.lifes.paper.JoinListener;
import it.hydr4.lifes.persistence.AsyncSaveQueue;
import it.hydr4.lifes.persistence.YamlLivesRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;

/** Bootstrap only; all behavior lives in the composed packages. */
public final class Lifes extends JavaPlugin {
    private LifesRuntime runtime;
    private AccountDirectory directory;
    private DefaultLivesService service;
    private AsyncSaveQueue saveQueue;
    private CommandWiring commandWiring;
    private PlaceholderApiHook placeholderHook;

    @Override
    public void onEnable() {
        saveResourceIfAbsent("settings.yml");
        try {
            runtime = LifesRuntime.load(new File(getDataFolder(), "settings.yml").toPath());
        } catch (ConfigException exception) {
            getLogger().severe(exception.getMessage());
            getLogger().severe("Fix settings.yml and restart or use /lives reload once the file is valid.");
            return;
        }

        directory = new AccountDirectory();
        var repository = new YamlLivesRepository(getDataFolder().toPath().resolve("saves.yml"));
        try {
            directory.loadAll(repository.loadAll());
        } catch (ConfigException exception) {
            getLogger().severe(exception.getMessage());
            getLogger().severe("Lifes stays disabled to protect the saved data; resolve the file above first.");
            return;
        }

        service = new DefaultLivesService(directory, () -> runtime.settings());
        saveQueue = new AsyncSaveQueue(
            repository,
            this::snapshot,
            runtime.settings().saveOffThread(),
            getLogger()
        );
        saveQueue.startPeriodic(runtime.settings().saveIntervalSeconds());
        service.addListener(saveQueue);
        service.addListener(new ActionRunner(() -> runtime.actions(), () -> runtime.maximumLives(), getLogger()));
        service.addListener(new BukkitEventBridge());
        service.addListener(new AdminChangeNotifier(() -> runtime.messages()));

        getServer().getPluginManager().registerEvents(new DeathListener(service, () -> runtime.settings()), this);
        getServer().getPluginManager().registerEvents(new JoinListener(() -> service), this);

        try {
            commandWiring = CommandWiring.register(this, runtime, service);
        } catch (RuntimeException exception) {
            getLogger().severe("Registering commands failed: " + exception.getMessage());
            return;
        }

        placeholderHook = PlaceholderApiHook.tryAttach(runtime, service, this).orElse(null);
        getLogger().info("Enabled v" + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        if (placeholderHook != null) {
            placeholderHook.close();
            placeholderHook = null;
        }
        if (commandWiring != null) {
            commandWiring.close();
            commandWiring = null;
        }
        if (saveQueue != null) {
            saveQueue.close();
            saveQueue = null;
        }
    }

    private java.util.Map<java.util.UUID, LivesAccount> snapshot() {
        var map = new LinkedHashMap<java.util.UUID, LivesAccount>();
        for (var account : directory.all()) {
            map.put(account.uuid(), account);
        }
        return map;
    }

    private void saveResourceIfAbsent(String name) {
        if (!getDataFolder().isDirectory()) {
            getDataFolder().mkdirs();
        }
        var target = new File(getDataFolder(), name);
        if (!target.isFile()) {
            saveResource(name, false);
        }
    }
}

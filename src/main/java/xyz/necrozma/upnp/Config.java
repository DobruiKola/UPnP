package xyz.necrozma.upnp;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class Config {

    private static Config instance;
    private YamlDocument config;

    private Config() {
        try {
            File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "upnp");
            if (!configDir.exists()) configDir.mkdirs();

            File configFile = new File(configDir, "config.yml");

            // Пытаемся загрузить embedded config.yml из ресурсов
            InputStream defaultConfig = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream("config.yml"),
                    "Не удалось найти config.yml в resources!"
            );

            config = YamlDocument.create(
                    configFile,
                    defaultConfig,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("config-version"))
                            .build()
            );

        } catch (IOException e) {
            System.err.println("[UPNP] Ошибка загрузки config.yml: " + e.getMessage());
        }
    }

    public static Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }

    public boolean getBoolean(Route route) {
        return config.getBoolean(route);
    }

    public String getString(Route route) {
        return config.getString(route);
    }

    public int getInt(Route route) {
        return config.getInt(route);
    }

    public void set(Route route, Object value) {
        config.set(route, value);
    }

    public void save() {
        try {
            config.save();
        } catch (IOException e) {
            System.err.println("[UPNP] Ошибка сохранения config.yml: " + e.getMessage());
        }
    }
}

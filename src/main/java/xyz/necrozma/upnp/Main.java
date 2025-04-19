package xyz.necrozma.upnp;

import dev.dejvokep.boostedyaml.route.Route;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.bitlet.weupnp.GatewayDevice;
import org.bstats.fabric.Metrics;  // Метрики для Fabric
import java.util.Set;

public class Main implements ModInitializer {

    private GatewayDevice gatewayDevice;
    private Set<Integer> tcpPorts;
    private Set<Integer> udpPorts;
    private boolean shouldRemovePortsOnStop;

    @Override
    public void onInitialize() {
        Config configManager = Config.getInstance();

        // Включаем метрики, если это настроено в конфиге
        if (configManager.getBoolean(Route.from("bstats"))) {
            int pluginId = 20515;
            new Metrics(this, pluginId); // Для Fabric
            System.out.println("[UPNP] Метрики включены");
        } else {
            System.out.println("[UPNP] Отключаем bstats согласно конфигу");
        }

        shouldRemovePortsOnStop = configManager.getBoolean(Route.from("close-ports-on-stop"));

        // Разбираем TCP и UDP порты отдельно
        tcpPorts = UPnPUtils.parsePorts(configManager, "tcp");
        udpPorts = UPnPUtils.parsePorts(configManager, "udp");

        // Осуществляем поиск шлюза и открываем порты
        gatewayDevice = UPnPUtils.discoverGateway();
        if (gatewayDevice != null) {
            UPnPUtils.mapPorts(gatewayDevice, tcpPorts, udpPorts);
        }

        // Регистрируем событие для остановки сервера
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
    }

    // Метод, который вызывается при остановке сервера
    private void onServerStopping(net.minecraft.server.MinecraftServer server) {
        System.out.println("[UPNP] Остановка UPnP сервиса...");
        if (shouldRemovePortsOnStop && gatewayDevice != null) {
            UPnPUtils.closeMappedPorts(gatewayDevice, tcpPorts, "TCP", null);
            UPnPUtils.closeMappedPorts(gatewayDevice, udpPorts, "UDP", null);
        }
    }

    public GatewayDevice getGatewayDevice() {
        return gatewayDevice;
    }
}

package xyz.necrozma.upnp;

import dev.dejvokep.boostedyaml.route.Route;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class UPnPUtils {

    public static Set<Integer> parsePorts(Config configManager, String type) {
        Set<Integer> uniqueValidPorts = new HashSet<>();
        String portsString = configManager.getString(Route.from("ports", type));
        if (portsString != null) {
            for (String port : portsString.split(",")) {
                try {
                    int portNumber = Integer.parseInt(port.trim());
                    if (isPortValid(portNumber)) {
                        uniqueValidPorts.add(portNumber);
                    } else {
                        System.err.println("[UPNP] Порт вне допустимого диапазона: " + portNumber);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[UPNP] Неверный порт: " + port);
                }
            }
        }
        return uniqueValidPorts;
    }

    public static boolean isPortValid(int port) {
        return port >= 0 && port <= 65535;
    }

    public static GatewayDevice discoverGateway() {
        try {
            GatewayDiscover discover = new GatewayDiscover();
            System.out.println("[UPNP] Поиск шлюзов...");
            discover.discover();

            GatewayDevice gatewayDevice = discover.getValidGateway();
            if (gatewayDevice != null) {
                System.out.println("[UPNP] Найден шлюз: " + gatewayDevice.getModelName() + " - " + gatewayDevice.getModelDescription());
                System.out.println("[UPNP] Локальный адрес: " + gatewayDevice.getLocalAddress());
                System.out.println("[UPNP] Внешний IP: " + gatewayDevice.getExternalIPAddress());
            } else {
                System.out.println("[UPNP] Не найдено допустимых шлюзов.");
            }
            return gatewayDevice;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            System.err.println("[UPNP] Ошибка при поиске шлюза: " + e.getLocalizedMessage());
            return null;
        }
    }

    public static void mapPorts(GatewayDevice gatewayDevice, Set<Integer> tcpPorts, Set<Integer> udpPorts) {
        tcpPorts.forEach(port -> mapPort(gatewayDevice, port, "TCP"));
        udpPorts.forEach(port -> mapPort(gatewayDevice, port, "UDP"));
    }

    private static void mapPort(GatewayDevice gatewayDevice, int port, String protocol) {
        try {
            System.out.println("[UPNP] Открытие порта " + port + " по " + protocol);
            PortMappingEntry portMapping = new PortMappingEntry();
            if (gatewayDevice.getSpecificPortMappingEntry(port, protocol, portMapping)) {
                System.out.println("[UPNP] Порт " + port + " уже проброшен, пропускаем.");
            } else {
                addPortMapping(gatewayDevice, port, protocol);
            }
        } catch (IOException | SAXException e) {
            System.err.println("[UPNP] Ошибка при пробросе " + protocol + " порта " + port + ": " + e.getLocalizedMessage());
        }
    }

    private static void addPortMapping(GatewayDevice gatewayDevice, int port, String protocol) throws IOException, SAXException {
        if (gatewayDevice.addPortMapping(port, port, gatewayDevice.getLocalAddress().getHostAddress(), protocol, "Fabric UPnP Mod")) {
            System.out.println("[UPNP] Успешно проброшен " + protocol + " порт " + port);
        } else {
            System.out.println("[UPNP] Не удалось пробросить " + protocol + " порт " + port);
        }
    }

    public static void closeMappedPorts(GatewayDevice gatewayDevice, Set<Integer> ports, String protocol) {
        for (int port : ports) {
            try {
                gatewayDevice.deletePortMapping(port, protocol);
                System.out.println("[UPNP] Закрыт порт " + port + " по протоколу " + protocol);
            } catch (IOException | SAXException e) {
                System.err.println("[UPNP] Ошибка при закрытии " + protocol + " порта " + port + ": " + e.getLocalizedMessage());
            }
        }
    }
}

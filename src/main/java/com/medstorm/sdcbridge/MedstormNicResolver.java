import java.net.*;
import java.util.*;

public class MedstormNicResolver {

    public static NetworkInterface resolve(String ifName) throws SocketException {
        if (ifName == null || ifName.isBlank()) {
            throw new IllegalArgumentException("Interface name is null or empty");
        }

        NetworkInterface nic = NetworkInterface.getByName(ifName);

        // If not found by system name, try by display name
        if (nic == null) {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (ni.getDisplayName().equalsIgnoreCase(ifName)
                        || ni.getName().equalsIgnoreCase(ifName)) {
                    nic = ni;
                    break;
                }
            }
        }

        if (nic == null) {
            throw new SocketException("NetworkInterface not found for name/display: " + ifName);
        }

        // Optional: log
        InetAddress ipv4 = getFirstIpv4(nic);
        System.out.printf("[MedstormNicResolver] resolved NIC '%s' (%s) -> IPv4=%s%n",
                ifName, nic.getDisplayName(), ipv4);

        return nic;
    }

    private static InetAddress getFirstIpv4(NetworkInterface ni) {
        Enumeration<InetAddress> addrs = ni.getInetAddresses();
        while (addrs.hasMoreElements()) {
            InetAddress addr = addrs.nextElement();
            if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                return addr;
            }
        }
        return null;
    }
}

package com.hotelmanager.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ClientIpResolver {

    @Value("${app.security.trusted-proxy-cidrs:}")
    private String configuredCidrs;

    private List<Cidr> trustedProxies = List.of();

    @PostConstruct
    void init() {
        if (configuredCidrs == null || configuredCidrs.isBlank()) {
            trustedProxies = List.of();
            return;
        }
        List<Cidr> parsed = new ArrayList<>();
        for (String value : configuredCidrs.split(",")) {
            String cidr = value.trim();
            if (!cidr.isBlank()) {
                parsed.add(Cidr.parse(cidr));
            }
        }
        trustedProxies = List.copyOf(parsed);
    }

    public String resolve(HttpServletRequest request) {
        InetAddress remote = parseIp(request.getRemoteAddr());
        if (remote == null) {
            return "unknown";
        }

        if (isTrusted(remote)) {
            // nginx overwrites X-Real-IP at the trusted boundary. Never consume
            // client-supplied X-Forwarded-For chains here.
            InetAddress proxiedClient = parseSingleHeaderIp(request.getHeader("X-Real-IP"));
            if (proxiedClient != null) {
                return proxiedClient.getHostAddress();
            }
        }
        return remote.getHostAddress();
    }

    private boolean isTrusted(InetAddress address) {
        return trustedProxies.stream().anyMatch(cidr -> cidr.matches(address));
    }

    private InetAddress parseSingleHeaderIp(String value) {
        if (value == null || value.isBlank() || value.contains(",")) {
            return null;
        }
        return parseIp(value.trim());
    }

    private static InetAddress parseIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        int zone = candidate.indexOf('%');
        if (zone >= 0) {
            candidate = candidate.substring(0, zone);
        }
        if (!candidate.matches("[0-9A-Fa-f:.]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private record Cidr(byte[] network, int prefixLength) {
        static Cidr parse(String value) {
            String[] parts = value.split("/", -1);
            InetAddress address = parseIp(parts[0]);
            if (address == null || parts.length > 2) {
                throw new IllegalStateException("Invalid trusted proxy CIDR: " + value);
            }
            int maxBits = address.getAddress().length * 8;
            int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : maxBits;
            if (prefix < 0 || prefix > maxBits) {
                throw new IllegalStateException("Invalid trusted proxy CIDR prefix: " + value);
            }
            return new Cidr(address.getAddress(), prefix);
        }

        boolean matches(InetAddress candidate) {
            byte[] address = candidate.getAddress();
            if (address.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}

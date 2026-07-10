package com.hotelmanager.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver();
        ReflectionTestUtils.setField(resolver, "configuredCidrs", "172.16.0.0/12");
        resolver.init();
    }

    @Test
    void ignoresForwardingHeadersFromUntrustedPeers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Real-IP", "198.51.100.25");
        request.addHeader("X-Forwarded-For", "192.0.2.99");

        assertEquals("203.0.113.10", resolver.resolve(request));
    }

    @Test
    void acceptsOnlyProxyOverwrittenRealIpFromTrustedPeer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.20.0.4");
        request.addHeader("X-Real-IP", "198.51.100.25");
        request.addHeader("X-Forwarded-For", "192.0.2.99");

        assertEquals("198.51.100.25", resolver.resolve(request));
    }
}

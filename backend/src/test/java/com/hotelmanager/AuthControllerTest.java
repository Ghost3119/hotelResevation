package com.hotelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        User u = TestData.userWithPassword(userRepository, passwordEncoder,
                "authctrl@hotel.test", "pass123", UserRole.ADMIN);
    }

    private MvcResult login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String extractRefreshToken(MvcResult result) {
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        if (setCookie == null) {
            return null;
        }
        for (String part : setCookie.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("hotel_refresh=")) {
                return trimmed.substring("hotel_refresh=".length());
            }
        }
        return null;
    }

    @Test
    void meWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsTokenSetsRefreshCookieAndMeWorks() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "authctrl@hotel.test", "password", "pass123"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("authctrl@hotel.test"))
                .andExpect(jsonPath("$.user.active").value(true))
                .andExpect(header().string("Set-Cookie", containsString("hotel_refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")))
                .andReturn();

        String token = extractAccessToken(loginResult);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("authctrl@hotel.test"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void loginWrongPasswordReturnsUnauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "authctrl@hotel.test", "password", "nope"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithCookieReturnsNewAccessTokenAndCookie() throws Exception {
        MvcResult loginResult = login("authctrl@hotel.test", "pass123");
        String refreshToken = extractRefreshToken(loginResult);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("hotel_refresh", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string("Set-Cookie", containsString("hotel_refresh=")));
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsCookie() throws Exception {
        MvcResult loginResult = login("authctrl@hotel.test", "pass123");
        String accessToken = extractAccessToken(loginResult);
        String refreshToken = extractRefreshToken(loginResult);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("hotel_refresh", refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("hotel_refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void logoutWithoutAccessTokenStillClearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void refreshWithRevokedTokenReturnsUnauthorized() throws Exception {
        MvcResult loginResult = login("authctrl@hotel.test", "pass123");
        String accessToken = extractAccessToken(loginResult);
        String refreshToken = extractRefreshToken(loginResult);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("hotel_refresh", refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("hotel_refresh", refreshToken)))
                .andExpect(status().isUnauthorized());
    }
}

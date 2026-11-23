package com.mundomanitas.api.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.monitoring.username=monitor",
        "app.monitoring.password=monitor-secret"
})
@AutoConfigureMockMvc
@DisplayName("Acceso a Actuator")
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("health exige credenciales de monitoreo")
    void healthRequiresMonitoringCredentials() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/actuator/health")
                        .header("Authorization", basic("monitor", "monitor-secret")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("prometheus exige credenciales de monitoreo")
    void prometheusRequiresMonitoringCredentials() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/actuator/prometheus")
                        .header("Authorization", basic("monitor", "monitor-secret")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("métricas e info no quedan expuestas al público")
    void metricsAndInfoStayPrivate() throws Exception {
        mvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    private String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}

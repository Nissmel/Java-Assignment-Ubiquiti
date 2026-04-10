package com.network.deployment.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.network.deployment.service.DeviceService;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class DeviceControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService.clear();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void registerDevice_validRequest_shouldReturn201() throws Exception {
        String body =
                """
                {
                    "deviceType": "GATEWAY",
                    "macAddress": "AA:BB:CC:DD:EE:01"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceType", is("GATEWAY")))
                .andExpect(jsonPath("$.macAddress", is("AA:BB:CC:DD:EE:01")));
    }

    static Stream<String> invalidRegistrationBodies() {
        return Stream.of(
                """
                {"deviceType": "GATEWAY", "macAddress": "INVALID"}
                """,
                """
                {"macAddress": "AA:BB:CC:DD:EE:01"}
                """,
                """
                {"deviceType": "GATEWAY", "macAddress": "AA:BB:CC:DD:EE:01", "uplinkMacAddress": "AA:BB:CC:DD:EE:01"}
                """);
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationBodies")
    void registerDevice_invalidInput_shouldReturn400(String body) throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerDevice_duplicateMac_shouldReturn409() throws Exception {
        String body =
                """
                {
                    "deviceType": "GATEWAY",
                    "macAddress": "AA:BB:CC:DD:EE:01"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllDevices_shouldReturnSortedList() throws Exception {
        registerTestDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:03", null);
        registerTestDevice("GATEWAY", "AA:BB:CC:DD:EE:01", null);
        registerTestDevice("SWITCH", "AA:BB:CC:DD:EE:02", null);

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].deviceType", is("GATEWAY")))
                .andExpect(jsonPath("$[1].deviceType", is("SWITCH")))
                .andExpect(jsonPath("$[2].deviceType", is("ACCESS_POINT")));
    }

    @Test
    void getAllDevices_emptyRegistry_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/devices")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getDeviceByMacAddress_existingDevice_shouldReturn200() throws Exception {
        registerTestDevice("GATEWAY", "AA:BB:CC:DD:EE:01", null);

        mockMvc.perform(get("/api/devices/AA:BB:CC:DD:EE:01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceType", is("GATEWAY")))
                .andExpect(jsonPath("$.macAddress", is("AA:BB:CC:DD:EE:01")));
    }

    @Test
    void getDeviceByMacAddress_nonExistent_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/devices/FF:FF:FF:FF:FF:FF")).andExpect(status().isNotFound());
    }

    @Test
    void getFullTopology_shouldReturnTreeStructure() throws Exception {
        registerTestDevice("GATEWAY", "AA:BB:CC:DD:EE:01", null);
        registerTestDevice("SWITCH", "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01");
        registerTestDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:03", "AA:BB:CC:DD:EE:02");

        mockMvc.perform(get("/api/devices/topology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].macAddress", is("AA:BB:CC:DD:EE:01")))
                .andExpect(jsonPath("$[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].macAddress", is("AA:BB:CC:DD:EE:02")))
                .andExpect(jsonPath("$[0].children[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].children[0].macAddress", is("AA:BB:CC:DD:EE:03")));
    }

    @Test
    void getTopologyFromDevice_existingDevice_shouldReturnSubtree() throws Exception {
        registerTestDevice("GATEWAY", "AA:BB:CC:DD:EE:01", null);
        registerTestDevice("SWITCH", "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01");
        registerTestDevice("ACCESS_POINT", "AA:BB:CC:DD:EE:03", "AA:BB:CC:DD:EE:02");

        mockMvc.perform(get("/api/devices/topology/AA:BB:CC:DD:EE:02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.macAddress", is("AA:BB:CC:DD:EE:02")))
                .andExpect(jsonPath("$.children", hasSize(1)))
                .andExpect(jsonPath("$.children[0].macAddress", is("AA:BB:CC:DD:EE:03")));
    }

    @Test
    void getTopologyFromDevice_nonExistent_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/devices/topology/FF:FF:FF:FF:FF:FF")).andExpect(status().isNotFound());
    }

    @Test
    void registerDevice_uplinkToNonExistentDevice_shouldReturn404() throws Exception {
        String body =
                """
                {
                    "deviceType": "SWITCH",
                    "macAddress": "AA:BB:CC:DD:EE:02",
                    "uplinkMacAddress": "FF:FF:FF:FF:FF:FF"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    private void registerTestDevice(String type, String mac, String uplinkMac) throws Exception {
        String uplinkField = uplinkMac != null ? ", \"uplinkMacAddress\": \"" + uplinkMac + "\"" : "";
        String body =
                """
                {
                    "deviceType": "%s",
                    "macAddress": "%s"%s
                }
                """
                        .formatted(type, mac, uplinkField);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}

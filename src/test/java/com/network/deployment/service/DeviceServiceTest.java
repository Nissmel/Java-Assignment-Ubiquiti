package com.network.deployment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.network.deployment.exception.DeviceAlreadyExistsException;
import com.network.deployment.exception.DeviceNotFoundException;
import com.network.deployment.model.DeviceRegistrationRequest;
import com.network.deployment.model.DeviceResponse;
import com.network.deployment.model.DeviceType;
import com.network.deployment.model.TopologyNode;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class DeviceServiceTest {

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService();
    }

    @Test
    void registerDevice_withoutUplink_shouldSucceed() {
        DeviceRegistrationRequest request =
                new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null);

        DeviceResponse response = deviceService.registerDevice(request);

        assertEquals(DeviceType.GATEWAY, response.deviceType());
        assertEquals("AA:BB:CC:DD:EE:01", response.macAddress());
    }

    @Test
    void registerDevice_withUplink_shouldSucceed() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceRegistrationRequest request =
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01");

        DeviceResponse response = deviceService.registerDevice(request);

        assertEquals(DeviceType.SWITCH, response.deviceType());
        assertEquals("AA:BB:CC:DD:EE:02", response.macAddress());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AA:BB:CC:DD:EE:01", "AA-BB-CC-DD-EE-01", "AABB.CCDD.EE01"})
    void registerDevice_duplicateMac_shouldThrowException(String duplicateMac) {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceRegistrationRequest duplicate = new DeviceRegistrationRequest(DeviceType.SWITCH, duplicateMac, null);

        assertThrows(DeviceAlreadyExistsException.class, () -> deviceService.registerDevice(duplicate));
    }

    @Test
    void registerDevice_withNonExistentUplink_shouldThrowException() {
        DeviceRegistrationRequest request =
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "FF:FF:FF:FF:FF:FF");

        assertThrows(DeviceNotFoundException.class, () -> deviceService.registerDevice(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aa:bb:cc:dd:ee:01", "AA-BB-CC-DD-EE-01", "AABB.CCDD.EE01"})
    void registerDevice_shouldNormalizeMacToColon(String inputMac) {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest(DeviceType.GATEWAY, inputMac, null);

        DeviceResponse response = deviceService.registerDevice(request);

        assertEquals("AA:BB:CC:DD:EE:01", response.macAddress());
    }

    @Test
    void getAllDevicesSorted_shouldReturnDevicesInCorrectOrder() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:03", null));
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", null));

        List<DeviceResponse> devices = deviceService.getAllDevicesSorted();

        assertEquals(3, devices.size());
        assertEquals(DeviceType.GATEWAY, devices.get(0).deviceType());
        assertEquals(DeviceType.SWITCH, devices.get(1).deviceType());
        assertEquals(DeviceType.ACCESS_POINT, devices.get(2).deviceType());
    }

    @Test
    void getAllDevicesSorted_emptyRegistry_shouldReturnEmptyList() {
        List<DeviceResponse> devices = deviceService.getAllDevicesSorted();

        assertTrue(devices.isEmpty());
    }

    @Test
    void getDeviceByMacAddress_existingDevice_shouldReturnDevice() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceResponse response = deviceService.getDeviceByMacAddress("AA:BB:CC:DD:EE:01");

        assertEquals(DeviceType.GATEWAY, response.deviceType());
        assertEquals("AA:BB:CC:DD:EE:01", response.macAddress());
    }

    @Test
    void getDeviceByMacAddress_nonExistentDevice_shouldThrowException() {
        assertThrows(DeviceNotFoundException.class, () -> deviceService.getDeviceByMacAddress("FF:FF:FF:FF:FF:FF"));
    }

    static Stream<String> macAddressFormats() {
        return Stream.of("aa:bb:cc:dd:ee:01", "AA-BB-CC-DD-EE-01", "AABB.CCDD.EE01");
    }

    @ParameterizedTest
    @MethodSource("macAddressFormats")
    void getDeviceByMacAddress_shouldFindDeviceRegardlessOfFormat(String lookupMac) {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceResponse response = deviceService.getDeviceByMacAddress(lookupMac);

        assertNotNull(response);
        assertEquals("AA:BB:CC:DD:EE:01", response.macAddress());
    }

    @Test
    void getFullTopology_shouldReturnTreeStructure() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));
        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01"));
        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:03", "AA:BB:CC:DD:EE:02"));

        List<TopologyNode> topology = deviceService.getFullTopology();

        assertEquals(1, topology.size());

        TopologyNode root = topology.getFirst();
        assertEquals("AA:BB:CC:DD:EE:01", root.getMacAddress());
        assertEquals(1, root.getChildren().size());

        TopologyNode switchNode = root.getChildren().getFirst();
        assertEquals("AA:BB:CC:DD:EE:02", switchNode.getMacAddress());
        assertEquals(1, switchNode.getChildren().size());

        TopologyNode apNode = switchNode.getChildren().getFirst();
        assertEquals("AA:BB:CC:DD:EE:03", apNode.getMacAddress());
        assertTrue(apNode.getChildren().isEmpty());
    }

    @Test
    void getFullTopology_multipleRoots_shouldReturnAllRoots() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:04", null));

        List<TopologyNode> topology = deviceService.getFullTopology();

        assertEquals(2, topology.size());
    }

    @Test
    void getFullTopology_emptyRegistry_shouldReturnEmptyList() {
        List<TopologyNode> topology = deviceService.getFullTopology();

        assertTrue(topology.isEmpty());
    }

    @Test
    void getTopologyFromDevice_shouldReturnSubtree() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));
        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01"));
        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:03", "AA:BB:CC:DD:EE:02"));
        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:04", "AA:BB:CC:DD:EE:02"));

        TopologyNode topology = deviceService.getTopologyFromDevice("AA:BB:CC:DD:EE:02");

        assertEquals("AA:BB:CC:DD:EE:02", topology.getMacAddress());
        assertEquals(2, topology.getChildren().size());
    }

    @Test
    void getTopologyFromDevice_leafNode_shouldReturnNodeWithNoChildren() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));
        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01"));

        TopologyNode topology = deviceService.getTopologyFromDevice("AA:BB:CC:DD:EE:02");

        assertEquals("AA:BB:CC:DD:EE:02", topology.getMacAddress());
        assertTrue(topology.getChildren().isEmpty());
    }

    @Test
    void getTopologyFromDevice_nonExistentDevice_shouldThrowException() {
        assertThrows(DeviceNotFoundException.class, () -> deviceService.getTopologyFromDevice("FF:FF:FF:FF:FF:FF"));
    }

    @Test
    void getDeviceByMacAddress_withHyphenSeparator_shouldFindDevice() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceResponse response = deviceService.getDeviceByMacAddress("AA-BB-CC-DD-EE-01");

        assertNotNull(response);
        assertEquals("AA:BB:CC:DD:EE:01", response.macAddress());
    }

    @Test
    void registerDevice_withHyphenUplink_shouldMatchColonRegistered() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceRegistrationRequest request =
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "AA-BB-CC-DD-EE-01");

        DeviceResponse response = deviceService.registerDevice(request);

        assertEquals(DeviceType.SWITCH, response.deviceType());
    }

    @Test
    void getTopologyFromDevice_withHyphenSeparator_shouldFindDevice() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));
        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01"));

        TopologyNode topology = deviceService.getTopologyFromDevice("AA-BB-CC-DD-EE-01");

        assertEquals("AA:BB:CC:DD:EE:01", topology.getMacAddress());
        assertEquals(1, topology.getChildren().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AA:BB:CC:DD:EE:01", "aa:bb:cc:dd:ee:01", "AA-BB-CC-DD-EE-01"})
    void registerDevice_selfReferencingUplink_shouldThrowException(String macAddress) {
        DeviceRegistrationRequest request =
                new DeviceRegistrationRequest(DeviceType.GATEWAY, macAddress, "AA:BB:CC:DD:EE:01");

        assertThrows(IllegalArgumentException.class, () -> deviceService.registerDevice(request));
    }

    @Test
    void getFullTopology_standaloneDevice_shouldAppearAsRootWithNoChildren() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:01", null));

        List<TopologyNode> topology = deviceService.getFullTopology();

        assertEquals(1, topology.size());
        assertEquals("AA:BB:CC:DD:EE:01", topology.getFirst().getMacAddress());
        assertTrue(topology.getFirst().getChildren().isEmpty());
    }

    @Test
    void getAllDevicesSorted_multipleDevicesOfSameType_shouldGroupTogether() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:05", null));
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.ACCESS_POINT, "AA:BB:CC:DD:EE:06", null));
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:03", null));
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:02", null));

        List<DeviceResponse> devices = deviceService.getAllDevicesSorted();

        assertEquals(5, devices.size());
        assertEquals(DeviceType.GATEWAY, devices.get(0).deviceType());
        assertEquals(DeviceType.GATEWAY, devices.get(1).deviceType());
        assertEquals(DeviceType.SWITCH, devices.get(2).deviceType());
        assertEquals(DeviceType.ACCESS_POINT, devices.get(3).deviceType());
        assertEquals(DeviceType.ACCESS_POINT, devices.get(4).deviceType());
    }

    @Test
    void getTopologyFromDevice_parentShouldShowNewlyAddedChild() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        TopologyNode before = deviceService.getTopologyFromDevice("AA:BB:CC:DD:EE:01");
        assertTrue(before.getChildren().isEmpty());

        deviceService.registerDevice(
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:01"));

        TopologyNode after = deviceService.getTopologyFromDevice("AA:BB:CC:DD:EE:01");
        assertEquals(1, after.getChildren().size());
        assertEquals("AA:BB:CC:DD:EE:02", after.getChildren().getFirst().getMacAddress());
    }

    @Test
    void getDeviceByMacAddress_withDotSeparator_shouldFindDevice() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceResponse response = deviceService.getDeviceByMacAddress("AABB.CCDD.EE01");

        assertNotNull(response);
        assertEquals("AA:BB:CC:DD:EE:01", response.macAddress());
    }

    @Test
    void registerDevice_withDotUplink_shouldMatchColonRegistered() {
        deviceService.registerDevice(new DeviceRegistrationRequest(DeviceType.GATEWAY, "AA:BB:CC:DD:EE:01", null));

        DeviceRegistrationRequest request =
                new DeviceRegistrationRequest(DeviceType.SWITCH, "AA:BB:CC:DD:EE:02", "AABB.CCDD.EE01");

        DeviceResponse response = deviceService.registerDevice(request);

        assertEquals(DeviceType.SWITCH, response.deviceType());
    }
}

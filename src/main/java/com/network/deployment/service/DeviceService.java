package com.network.deployment.service;

import com.network.deployment.exception.DeviceAlreadyExistsException;
import com.network.deployment.exception.DeviceNotFoundException;
import com.network.deployment.model.Device;
import com.network.deployment.model.DeviceRegistrationRequest;
import com.network.deployment.model.DeviceResponse;
import com.network.deployment.model.TopologyNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    public DeviceResponse registerDevice(DeviceRegistrationRequest request) {
        String mac = normalizeMac(request.macAddress());
        String uplinkMac = request.uplinkMacAddress() != null ? normalizeMac(request.uplinkMacAddress()) : null;

        if (devices.containsKey(mac)) {
            throw new DeviceAlreadyExistsException(mac);
        }

        if (uplinkMac != null && uplinkMac.equals(mac)) {
            throw new IllegalArgumentException("Device cannot be its own uplink: " + mac);
        }

        if (uplinkMac != null && !devices.containsKey(uplinkMac)) {
            throw new DeviceNotFoundException(uplinkMac);
        }

        Device device = new Device(request.deviceType(), mac, uplinkMac);
        devices.put(mac, device);

        return DeviceResponse.fromDevice(device);
    }

    public List<DeviceResponse> getAllDevicesSorted() {
        Comparator<Device> byType = Comparator.comparingInt(d -> d.deviceType().ordinal());

        return devices.values().stream()
                .sorted(byType)
                .map(DeviceResponse::fromDevice)
                .toList();
    }

    public DeviceResponse getDeviceByMacAddress(String macAddress) {
        String mac = normalizeMac(macAddress);
        Device device = devices.get(mac);
        if (device == null) {
            throw new DeviceNotFoundException(mac);
        }
        return DeviceResponse.fromDevice(device);
    }

    public List<TopologyNode> getFullTopology() {
        List<TopologyNode> roots = new ArrayList<>();

        for (Device device : devices.values()) {
            if (device.uplinkMacAddress() == null) {
                roots.add(buildTopologyNode(device.macAddress()));
            }
        }

        return roots;
    }

    public TopologyNode getTopologyFromDevice(String macAddress) {
        String mac = normalizeMac(macAddress);
        if (!devices.containsKey(mac)) {
            throw new DeviceNotFoundException(mac);
        }
        return buildTopologyNode(mac);
    }

    private TopologyNode buildTopologyNode(String macAddress) {
        TopologyNode node = new TopologyNode(macAddress);

        for (Device device : devices.values()) {
            if (macAddress.equals(device.uplinkMacAddress())) {
                node.addChild(buildTopologyNode(device.macAddress()));
            }
        }

        return node;
    }

    public void clear() {
        devices.clear();
    }

    private String normalizeMac(String mac) {
        String raw = mac.toUpperCase().replaceAll("[:\\-.]", "");
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < raw.length(); i += 2) {
            if (i > 0) {
                normalized.append(':');
            }
            normalized.append(raw, i, i + 2);
        }
        return normalized.toString();
    }
}

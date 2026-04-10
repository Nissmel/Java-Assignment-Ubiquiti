package com.network.deployment.model;

public record DeviceResponse(DeviceType deviceType, String macAddress) {

    public static DeviceResponse fromDevice(Device device) {
        return new DeviceResponse(device.deviceType(), device.macAddress());
    }
}

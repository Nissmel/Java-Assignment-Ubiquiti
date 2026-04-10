package com.network.deployment.exception;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String macAddress) {
        super("Device not found with MAC address: " + macAddress);
    }
}

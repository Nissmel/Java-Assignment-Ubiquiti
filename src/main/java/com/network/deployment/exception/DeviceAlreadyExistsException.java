package com.network.deployment.exception;

public class DeviceAlreadyExistsException extends RuntimeException {

    public DeviceAlreadyExistsException(String macAddress) {
        super("Device already registered with MAC address: " + macAddress);
    }
}

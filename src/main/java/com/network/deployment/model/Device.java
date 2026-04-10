package com.network.deployment.model;

public record Device(DeviceType deviceType, String macAddress, String uplinkMacAddress) {}

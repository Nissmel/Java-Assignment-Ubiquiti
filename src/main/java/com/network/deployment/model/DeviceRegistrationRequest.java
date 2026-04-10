package com.network.deployment.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DeviceRegistrationRequest(
        @NotNull(message = "Device type is required") DeviceType deviceType,
        @NotNull(message = "MAC address is required") @Pattern(
                        regexp = "^(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}|([0-9A-Fa-f]{4}\\.){2}[0-9A-Fa-f]{4})$",
                        message = "Invalid MAC address format")
                String macAddress,
        @Pattern(
                        regexp = "^(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}|([0-9A-Fa-f]{4}\\.){2}[0-9A-Fa-f]{4})$",
                        message = "Invalid uplink MAC address format")
                String uplinkMacAddress) {}

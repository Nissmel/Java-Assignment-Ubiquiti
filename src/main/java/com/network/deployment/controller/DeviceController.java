package com.network.deployment.controller;

import com.network.deployment.model.DeviceRegistrationRequest;
import com.network.deployment.model.DeviceResponse;
import com.network.deployment.model.TopologyNode;
import com.network.deployment.service.DeviceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        DeviceResponse response = deviceService.registerDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevicesSorted());
    }

    @GetMapping("/{macAddress}")
    public ResponseEntity<DeviceResponse> getDeviceByMacAddress(@PathVariable String macAddress) {
        return ResponseEntity.ok(deviceService.getDeviceByMacAddress(macAddress));
    }

    @GetMapping("/topology")
    public ResponseEntity<List<TopologyNode>> getFullTopology() {
        return ResponseEntity.ok(deviceService.getFullTopology());
    }

    @GetMapping("/topology/{macAddress}")
    public ResponseEntity<TopologyNode> getTopologyFromDevice(@PathVariable String macAddress) {
        return ResponseEntity.ok(deviceService.getTopologyFromDevice(macAddress));
    }
}

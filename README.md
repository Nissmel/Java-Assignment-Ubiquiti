# Network Deployment Device API

REST API for managing network deployment devices. Supports registering devices (Gateway, Switch, Access Point), querying them by MAC address, listing them sorted by type, and retrieving the network topology as a tree structure.

## Prerequisites

- Java 25
- No other tools required (Gradle wrapper is included)

## Build and Run

Build the project:

```bash
./gradlew build
```

Run the application:

```bash
./gradlew bootRun
```

The API starts on `http://localhost:8443`.

Run tests only:

```bash
./gradlew test
```

## Tech Stack

- Java 25
- Spring Boot 4.0.0
- Gradle 8.14 (Kotlin DSL, wrapper included)
- JUnit 5 for testing
- In-memory storage (no database or additional configuration required)

## Quick Start

With the application running, register a sample network deployment:

```bash
# Gateway
curl -X POST http://localhost:8443/api/devices -H "Content-Type: application/json" \
  -d '{"deviceType":"GATEWAY","macAddress":"24:A4:3C:D3:E4:01"}'

# Switches (connected to gateway)
curl -X POST http://localhost:8443/api/devices -H "Content-Type: application/json" \
  -d '{"deviceType":"SWITCH","macAddress":"FC:EC:DA:D3:E4:02","uplinkMacAddress":"24:A4:3C:D3:E4:01"}'

curl -X POST http://localhost:8443/api/devices -H "Content-Type: application/json" \
  -d '{"deviceType":"SWITCH","macAddress":"FC:EC:DA:D3:E4:03","uplinkMacAddress":"24:A4:3C:D3:E4:01"}'

# Access Points (connected to switches)
curl -X POST http://localhost:8443/api/devices -H "Content-Type: application/json" \
  -d '{"deviceType":"ACCESS_POINT","macAddress":"78:8A:20:D3:E4:04","uplinkMacAddress":"FC:EC:DA:D3:E4:02"}'

curl -X POST http://localhost:8443/api/devices -H "Content-Type: application/json" \
  -d '{"deviceType":"ACCESS_POINT","macAddress":"78:8A:20:D3:E4:05","uplinkMacAddress":"FC:EC:DA:D3:E4:03"}'

# Standalone Access Point (no uplink)
curl -X POST http://localhost:8443/api/devices -H "Content-Type: application/json" \
  -d '{"deviceType":"ACCESS_POINT","macAddress":"78:8A:20:D3:E4:06"}'
```

This creates the following network:

```
Gateway (24:A4:3C:D3:E4:01)
+-- Switch (FC:EC:DA:D3:E4:02)
|   +-- Access Point (78:8A:20:D3:E4:04)
+-- Switch (FC:EC:DA:D3:E4:03)
    +-- Access Point (78:8A:20:D3:E4:05)

Access Point (78:8A:20:D3:E4:06)  [standalone]
```

Then query the API:

```bash
# All devices sorted by type
curl http://localhost:8443/api/devices

# Single device by MAC
curl http://localhost:8443/api/devices/24:A4:3C:D3:E4:01

# Full network topology
curl http://localhost:8443/api/devices/topology

# Topology from a specific device
curl http://localhost:8443/api/devices/topology/FC:EC:DA:D3:E4:02
```

## API Endpoints

### Register a device

```
POST /api/devices
```

Request body:

```json
{
  "deviceType": "GATEWAY",
  "macAddress": "24:A4:3C:D3:E4:01",
  "uplinkMacAddress": null
}
```

- `deviceType` -- required, one of: `GATEWAY`, `SWITCH`, `ACCESS_POINT`
- `macAddress` -- required, valid MAC address format (e.g. `AA:BB:CC:DD:EE:01`, `AA-BB-CC-DD-EE-01`, or `AABB.CCDD.EE01`)
- `uplinkMacAddress` -- optional, MAC address of an already registered device

Response `201 Created`:

```json
{
  "deviceType": "GATEWAY",
  "macAddress": "24:A4:3C:D3:E4:01"
}
```

### Get all devices (sorted by type)

```
GET /api/devices
```

Returns all registered devices sorted by type: Gateway, then Switch, then Access Point.

Response `200 OK`:

```json
[
  { "deviceType": "GATEWAY", "macAddress": "24:A4:3C:D3:E4:01" },
  { "deviceType": "SWITCH", "macAddress": "FC:EC:DA:D3:E4:02" },
  { "deviceType": "ACCESS_POINT", "macAddress": "78:8A:20:D3:E4:03" }
]
```

### Get device by MAC address

```
GET /api/devices/{macAddress}
```

Response `200 OK`:

```json
{
  "deviceType": "GATEWAY",
  "macAddress": "24:A4:3C:D3:E4:01"
}
```

### Get full network topology

```
GET /api/devices/topology
```

Returns the full device topology as a tree. Root nodes are devices without an uplink.

Response `200 OK`:

```json
[
  {
    "macAddress": "24:A4:3C:D3:E4:01",
    "children": [
      {
        "macAddress": "FC:EC:DA:D3:E4:02",
        "children": [
          {
            "macAddress": "78:8A:20:D3:E4:03",
            "children": []
          }
        ]
      }
    ]
  }
]
```

### Get topology from a specific device

```
GET /api/devices/topology/{macAddress}
```

Returns the topology subtree starting from the given device.

Response `200 OK`:

```json
{
  "macAddress": "FC:EC:DA:D3:E4:02",
  "children": [
    {
      "macAddress": "78:8A:20:D3:E4:03",
      "children": []
    }
  ]
}
```

## Error Handling

| Status | Condition                              |
|--------|----------------------------------------|
| 400    | Invalid MAC address or missing fields  |
| 400    | Device cannot be its own uplink        |
| 404    | Device not found                       |
| 409    | Device with given MAC already exists   |

Error response format:

```json
{
  "timestamp": "2026-04-10T10:00:00Z",
  "status": 404,
  "message": "Device not found with MAC address: FF:FF:FF:FF:FF:FF"
}
```

## Project Structure

```
src/main/java/com/network/deployment/
    NetworkDeploymentApplication.java        -- Application entry point
    controller/
        DeviceController.java                -- REST controller
    service/
        DeviceService.java                   -- Business logic
    model/
        Device.java                          -- Device entity
        DeviceType.java                      -- Enum (GATEWAY, SWITCH, ACCESS_POINT)
        DeviceRegistrationRequest.java       -- Request DTO with validation
        DeviceResponse.java                  -- Response DTO
        TopologyNode.java                    -- Tree node for topology
    exception/
        DeviceNotFoundException.java         -- 404 exception
        DeviceAlreadyExistsException.java    -- 409 exception
        GlobalExceptionHandler.java          -- Centralized error handling

src/test/java/com/network/deployment/
    service/
        DeviceServiceTest.java               -- Unit tests for service layer (31 tests)
    controller/
        DeviceControllerTest.java            -- Integration tests for REST API (13 tests)
```

## Design Decisions

- **In-memory storage** using `ConcurrentHashMap` for thread safety. No database is needed, which means the application runs without any external configuration.
- **MAC address normalization** -- all MAC addresses are normalized to uppercase with colon separators. Colon (`AA:BB:CC:DD:EE:01`), hyphen (`AA-BB-CC-DD-EE-01`), and Cisco dot (`AABB.CCDD.EE01`) formats are all accepted and treated as the same address.
- **Uplink validation** -- when registering a device with an uplink, the uplink device must already be registered. A device cannot reference itself as its own uplink.
- **Topology as tree** -- the full topology returns a list of root nodes (devices with no uplink), each containing their children recursively. The per-device topology returns the subtree rooted at the given device.
- **Sorting order** -- devices are sorted by the enum ordinal: Gateway (0) > Switch (1) > Access Point (2).
- **Java records** -- immutable DTOs (Device, DeviceResponse, DeviceRegistrationRequest) are implemented as records for conciseness.
- **Spotless** -- code formatting is enforced via the Spotless Gradle plugin with Palantir Java Format.

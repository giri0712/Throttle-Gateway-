# ThrottleGate Spring Boot Starter

This starter provides easy integration with the ThrottleGate rate limiting service for Spring Boot applications.

## Usage

Add the dependency to your project:

```xml
<dependency>
    <groupId>com.throttlegate</groupId>
    <artifactId>throttle-gate-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Configure the ThrottleGate service URL in your `application.properties` or `application.yml`:

```properties
throttlegate.service-url=http://localhost:8080
throttlegate.timeout=5
```

Then inject the `ThrottleGateClient` where needed:

```java
@Service
public class MyService {

    private final ThrottleGateClient throttleGateClient;

    public MyService(ThrottleGateClient throttleGateClient) {
        this.throttleGateClient = throttleGateClient;
    }

    public void handleRequest(String clientId, String endpoint) {
        if (throttleGateClient.isAllowed(clientId, endpoint, "free")) {
            // process request
        } else {
            // reject request or handle rate limiting
        }
    }
}
```

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `throttlegate.service-url` | Base URL of the ThrottleGate service | `http://localhost:8080` |
| `throttlegate.timeout` | HTTP timeout in seconds | `5` |
| `throttlegate.enabled` | Enable the starter | `true` |

## API

The starter provides a `ThrottleGateClient` bean with two methods:

- `boolean isAllowed(String clientId, String endpoint, String tier)` - simple allow/deny check
- `ThrottleGateResponse check(String clientId, String endpoint, String tier)` - detailed response with additional metadata

## Building

```bash
mvn clean install
```
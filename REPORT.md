# Lab 2 Web Server -- Project Report

## What I specified

### Objective

I set out to complete the three required parts of the lab and then add their extensions. I planned to verify application behavior manually and using automated tests and TLS/HTTP/2 against the running server.

1. I planned to replace Spring Boot's default whitelabel response with a custom HTML error page for browser requests.
    - Extension: I planned to show the error status and request path on that page and assert both in the 404 test.
2. I planned to add `/time` and return the server's current time as JSON.
    - Extension: I planned to inject a fixed `TimeProvider` in a test and assert an exact timestamp.
    - Extension: I planned to accept an optional IANA time zone, such as `Europe/Madrid`, and return that code in the JSON. For example `GET /time?zone=Europe/Madrid`.
3. I planned to enable HTTP/2 (`h2` over ALPN) and TLS on port 8443 with a self-signed certificate.
    - Extension: I planned to include `IP:127.0.0.1` alongside `DNS:localhost` in the certificate SAN, select the PKCS12 alias explicitly, and verify that ALPN still selects `h2`.

## What I changed

### Objective

I implemented the required application behavior and tests in the existing Kotlin and Spring Boot project.

1. I added `src/main/resources/templates/error.html` and updated `ApplicationTests.kt` to request an unknown path from a real random-port server with `Accept: text/html`.
    - Extension: I displayed Spring's error status and request path in the template and asserted both in the 404 test.
    - Extension: I enabled Spring MVC Problem Details and added exception advice. API requests receive `application/problem+json`; invalid time zones produce a 400 response with the `invalidZone` extension, and unexpected API exceptions produce a sanitized 500 response. HTML requests continue to use the Thymeleaf page.
2. I added `TimeDTO`, `TimeProvider`, `TimeService`, the `LocalDateTime.toDTO()` extension, and the `/time` controller in `TimeComponent.kt`. I added MockMvc coverage in `TimeControllerTest.kt`.
2.1 Bonus: I supplied a fixed `TimeProvider` from the test configuration and asserted its exact serialized timestamp.
2.2 Bonus: I added optional `zone` handling to `/time`; the tests check the Madrid conversion and the RFC 9457 response for an invalid zone.
3. I added `openssl-localhost.cnf`, generated the local certificate and key, packaged them in `src/main/resources/localhost.p12` with password `secret`, and configured TLS plus HTTP/2 on port 8443 in `src/main/resources/application.yml`. The certificate, key, and OpenSSL configuration are ignored by Git; the PKCS12 keystore is tracked.
3.1 Bonus: I added the loopback IP to the certificate SAN and set the server's key alias to `localhost`, matching the PKCS12 alias.

I generated and packaged the certificate with:

```bash
openssl req -x509 -newkey rsa:2048 -nodes -sha256 \
	-keyout localhost.key -out localhost.crt \
	-config openssl-localhost.cnf
```
```bash
openssl pkcs12 -export \
	-in localhost.crt -inkey localhost.key \
	-name localhost -out src/main/resources/localhost.p12 \
	-passout pass:secret
```


## Technical decisions

### Objective

1. I kept the HTML error page for browsers and used Spring's `status` and `path` error attributes so the displayed information matches the failed request.
1.1 Bonus: I used Spring's `ProblemDetail` for API errors rather than defining a separate JSON schema. I retained content negotiation so browser requests still render HTML, while API requests receive Problem Details. The generic 500 response does not expose internal exception messages.
2. I represented the timestamp as `LocalDateTime` in the `time` field and injected `TimeProvider` into the controller so tests can control the time source.
2.1 Bonus: I fixed the test clock to a known value to make the serialized timestamp assertion deterministic.
2.2 Bonus: I use `ZoneId` to produce the requested local time. Invalid zone names are treated as client errors rather than server errors.
3. I used a self-signed RSA certificate because the lab has no certificate authority. Spring Boot loads the PKCS12 keystore, and embedded Tomcat terminates TLS and negotiates HTTP/2 through ALPN.
3.1 Bonus: I included DNS and IP SAN entries for localhost requests and explicitly configured the keystore alias. I disabled SSL in test resources so automated tests continue to use plain HTTP.

## How I verified

### Objective

1. I ran the test suite to verify that browser requests still render HTML and API requests receive RFC 9457 responses. The tests check the 400, 404, 405, and 500 statuses, the problem media type and fields, the HTML error content, and the invalid-zone extension.
2. I verified the normal time response, fixed timestamp, requested-zone conversion, and invalid-zone behavior with MockMvc tests.
3. I ran the full project check, which includes the test suite and ktlint:

```bash
./gradlew check
```

I checked the generated certificate's SAN and the PKCS12 alias with:

```bash
openssl x509 -in localhost.crt -noout -ext subjectAltName
keytool -list -storetype PKCS12 -keystore src/main/resources/localhost.p12 -storepass secret
```

3.1 Bonus: During a successful live run, I started the server and queried the IP address over HTTP/2. Curl reported `ALPN: server accepted h2`, returned HTTP/2 200, and displayed the JSON time response:

I started the server in one terminal:

```bash
./gradlew bootRun
```

With it running, I sent the request from another terminal:

```bash
curl -vk --http2 'https://127.0.0.1:8443/time?zone=Europe/Madrid'
```

On a later retry, the server could not bind because port 8443 was already occupied. A subsequent curl request could not connect after that server process exited, so I do not count that retry as a successful manual verification. The automated check passed after the RFC 9457 changes.

## AI disclosure

- **Tools / skills:** Copilot in VS Code
- **Purpose:** assistance planning the process and developing tests
- **Representative prompts:** "modifify applicationtests.kt to check that the status and the request path appear correctly on the error page. Thymeleaf can read status and path from the error model Spring already provides."
"make a plan to turn on HTTP/2 over TLS (h2, ALPN) with a self-signed certificate in this server"
- **Affected files/sections:** tests
- **Validation steps:** I ran the automated Gradle checks and tests, inspected the generated certificate and keystore, reviewed the HTTP response and TLS handshake output from curl and manually reviewed the code and functionality.
- **Citations:** none
- **Human-reviewed:** I reviewed the generated implementation and test results, retained content negotiation between HTML and Problem Details and kept internal exception details out of generic 500 responses. I also rejected too complex tests.


# Lab 2 Web Server -- Project Report

## What I specified

### Objective

I set out to complete the three required parts of the lab and then add their extensions. I planned to verify application behavior manually and using automated tests and TLS/HTTP/2 against the running server.

1. Replace Spring Boot's default whitelabel response with a custom HTML error page for browser requests.
    - Extension: Show the error status and request path on that page and assert both in the 404 test.
2. Add `/time` and return the server's current time as JSON.
    - Extension: Inject a fixed `TimeProvider` in a test and assert an exact timestamp.
    - Extension: Accept an optional IANA time zone, such as `Europe/Madrid`, and return that code in the JSON. For example `GET /time?zone=Europe/Madrid`.
3. Enable HTTP/2 (`h2` over ALPN) and TLS on port 8443 with a self-signed certificate.
    - Extension: Include `IP:127.0.0.1` alongside `DNS:localhost` in the certificate SAN, select the PKCS12 alias explicitly and verify that ALPN still selects `h2`.

### Bonus

I also planned two extensions on separate branches, that would be verified manually and using automated tests: 
1. RFC 9457 Problem Details for API errors on `feature/rfc_9457`. The JSON response when an error occurs is normalized via RFC.
2. Content negotiation on `/time` on `feature/content_negotiation`. It covers response format, language, compression and conditional request headers using Spring Boot's built-in support.

## What I changed

### Objective

I implemented the required application behavior and tests in the existing Kotlin and Spring Boot project.

1. I added `src/main/resources/templates/error.html` and updated `ApplicationTests.kt` to request an unknown path from a real random-port server with `Accept: text/html`.
    - Extension: I displayed Spring's error status and request path in the template and asserted both in the 404 test.
    - Extension: I enabled Spring MVC Problem Details and added exception advice. API requests receive `application/problem+json`; invalid time zones produce a 400 response with the `invalidZone` extension, and unexpected API exceptions produce a sanitized 500 response. HTML requests continue to use the Thymeleaf page.
2. I added `TimeDTO`, `TimeProvider`, `TimeService`, the `LocalDateTime.toDTO()` extension and the `/time` controller in `TimeComponent.kt`. I added MockMvc coverage in `TimeControllerTest.kt`.
    - Extension: I supplied a fixed `TimeProvider` from the test configuration and asserted its exact serialized timestamp.
    - Extension: I added optional `zone` handling to `/time`; the tests check the Madrid conversion and the RFC 9457 response for an invalid zone.
3. I added `openssl-localhost.cnf`, generated the local certificate and key, packaged them in `src/main/resources/localhost.p12` with password `secret` and configured TLS plus HTTP/2 on port 8443 in `src/main/resources/application.yml`. The certificate, key and OpenSSL configuration are ignored by Git; the PKCS12 keystore is tracked.
    - Extension: I added the loopback IP to the certificate SAN and set the server's key alias to `localhost`, matching the PKCS12 alias.

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

### Bonus

1. On `feature/rfc_9457`, I separated API error responses from browser error pages using Spring MVC content negotiation. Requests accepting HTML render the error page; API requests receive an RFC 9457 `ProblemDetail` response. Invalid time zones return 400 with an `invalidZone` extension, and unexpected API failures return a sanitized 500 response. I added `exception/GlobalExceptionHandler.kt` and `exception/HtmlErrorNegotiationHandler.kt`, and updated `TimeComponent.kt`, `application.yml` and `ApplicationTests.kt`.
2. On `feature/content_negotiation`, I retained JSON, plain text and HTML representations of `/time`, selected through `Accept`. I added English and Spanish message bundles and use Spring's resolved `Locale` to translate the response text. I enabled server-side compression and added conditional response handling for ETag and Last-Modified. Automated tests cover these responses, including gzip from a running embedded server. I added `config/WebConfig.kt`, `resources/messages.properties`, `resources/messages_es.properties` and `TimeCompressionTest.kt`. I updated `TimeComponent.kt`, `application.yml` and `TimeControllerTest.kt`.


## Technical decisions

### Objective

1. I kept the HTML error page for browsers and used Spring's `status` and `path` error attributes so the displayed information matches the failed request.
    - Extension: I used Spring's `ProblemDetail` for API errors rather than defining a separate JSON schema. I retained content negotiation so browser requests still render HTML, while API requests receive Problem Details. The generic 500 response does not expose internal exception messages.
2. I represented the timestamp as `LocalDateTime` in the `time` field and injected `TimeProvider` into the controller so tests can control the time source.
    - Extension: I fixed the test clock to a known value to make the serialized timestamp assertion deterministic.
    - Extension: I use `ZoneId` to produce the requested local time. Invalid zone names are treated as client errors rather than server errors.
3. I used a self-signed RSA certificate because the lab has no certificate authority. Spring Boot loads the PKCS12 keystore, and embedded Tomcat terminates TLS and negotiates HTTP/2 through ALPN.
    - Extension: I included DNS and IP SAN entries for localhost requests and explicitly configured the keystore alias. I disabled SSL in test resources so automated tests continue to use plain HTTP.

### Bonus

1. I used separate Spring advice handlers for HTML and API errors, selected by the request's accepted media type. `ProblemDetail` provides the standard RFC 9457 fields, while the generic 500 response avoids exposing internal exception details.
2. I used Spring's `MessageSource` and the resolved `Locale` for `Accept-Language`, and Spring Boot's server compression settings for `Accept-Encoding`. `WebRequest.checkNotModified` handles `If-Modified-Since`; `ShallowEtagHeaderFilter` calculates ETags. I configured weak ETags because Tomcat skips compression when a response has a strong ETag. The `/time` response also varies on `Accept`, `Accept-Language` and `Accept-Encoding` so caches distinguish representations.

## How I verified

### Objective

1. I ran the test suite to verify that browser requests still render HTML and API requests receive RFC 9457 responses. The tests check the 400 status, the problem media type and fields, the HTML error content and the invalid-zone extension.
2. I verified the normal time response, fixed timestamp, requested-zone conversion and invalid-zone behavior with MockMvc tests.
3. I ran the full project check, which includes the test suite and ktlint:

```bash
./gradlew check
```

I checked the generated certificate's SAN and the PKCS12 alias with:

```bash
openssl x509 -in localhost.crt -noout -ext subjectAltName
keytool -list -storetype PKCS12 -keystore src/main/resources/localhost.p12 -storepass secret
```

During a successful live run, I started the server and queried the IP address over HTTP/2. Curl reported `ALPN: server accepted h2`, returned HTTP/2 200 and displayed the JSON time response:

I started the server in one terminal:

```bash
./gradlew bootRun
```

With it running, I sent the request from another terminal:

```bash
curl -vk --http2 'https://127.0.0.1:8443/time?zone=Europe/Madrid'
```

The automated and manual checks were successful.

### Bonus

1. The RFC 9457 tests verify that API clients receive Problem Details and that browser requests continue to render the HTML error page. They cover invalid time zones and the relevant error statuses and fields.
2. I added tests for the JSON, plain-text and HTML representations, Spanish translations, ETag and Last-Modified conditional requests, and gzip using an embedded server. I also ran the complete project check:

```bash
./gradlew check
```

Since a browser address bar does not let me set `Accept`, I also used curl to request each representation manually (JSON, plain text and HTML):

```bash
curl -k -H "Accept: application/json" "https://localhost:8443/time?zone=UTC"
```

```bash
curl -k -H "Accept: text/plain" "https://localhost:8443/time?zone=UTC"
```

```bash
curl -k -H "Accept: text/html" "https://localhost:8443/time?zone=UTC"
```

I checked Spanish localization with:

```bash
curl -k -i -H "Accept: application/json" -H "Accept-Language: es-ES" \
    "https://localhost:8443/time?zone=UTC"
```

I checked transparent gzip compression with:

```bash
curl -k --compressed -D - -o /dev/null \
    -H "Accept: application/json" -H "Accept-Encoding: gzip" \
    "https://localhost:8443/time?zone=UTC"
```

To check `If-None-Match`, the first command saves the returned ETag and the second sends it back:

```bash
curl -k -i -D /tmp/time-etag.headers \
    -H "Accept: text/plain" "https://localhost:8443/time?zone=UTC"
```

```bash
curl -k -i \
    -H "If-None-Match: $(awk 'tolower($1) == "etag:" { sub("\\r$", "", $2); print $2 }' /tmp/time-etag.headers)" \
    -H "Accept: text/plain" "https://localhost:8443/time?zone=UTC"
```

I checked `If-Modified-Since` in the same way using the returned `Last-Modified` date:

```bash
curl -k -i -D /tmp/time-last-modified.headers \
    -H "Accept: application/json" "https://localhost:8443/time?zone=UTC"
```

```bash
curl -k -i \
    -H "If-Modified-Since: $(awk 'tolower($1) == "last-modified:" { sub("\\r$", ""); sub(/^[^:]*:[[:space:]]*/, ""); print }' /tmp/time-last-modified.headers)" \
    -H "Accept: application/json" "https://localhost:8443/time?zone=UTC"
```

The validation requests return 304 only while the corresponding `/time` representation has not changed. Since the endpoint reports the current time, a second request may return 200 if the time changes between requests.

## Comparison with other classmates work

Classmate A and B proposed similar bonus functionality to the one I implemented. Here I highlight why our proposals are similar but especially why they are different.

### Similarities

All three projects implement the custom error page, `/time` endpoint and HTTPS/HTTP/2 setup. For the bonus, we kept `/time` as the time resource and used Spring support for content negotiation, localization, compression and conditional requests, with automated tests for the added behavior.

### Differences

I chose JSON, plain text and HTML; both classmates chose JSON, XML and HTML. My HTML is a lightweight time widget, unlike Classmate A's Zaragoza tram panel or Classmate B's MVC views selected with `ContentNegotiatingViewResolver`. I localized the greeting in English and Spanish; Classmate A also supports French and conditionally adds a `text` value, while Classmate B translates a `label` value. For compression, my 1-byte threshold allows even short `/time` responses to be compressed, like Classmate B's approach; Classmate A keeps Tomcat's 2 KB threshold and compresses the larger HTML panel. For caching, I use `ShallowEtagHeaderFilter` and set `Last-Modified` on the response. Classmate A builds validators around the current-minute version, while Classmate B caches a time snapshot per minute; neither uses my filter-based ETag approach.

## AI disclosure

- **Tools / skills:** Gemini and Copilot in VS Code
- **Purpose:** 
    - Help outline the backend changes before implementation and plan the process.
    - Assistance explaining the code of the initial lab.
    - Suggest and assist the implementation of test cases.
    - Assistance for documenting the code and the report.
- **Representative prompts:** 
    - "Modify applicationtests.kt to check that the status and the request path appear correctly on the error page. Thymeleaf can read status and path from the error model Spring already provides."
    - "Make a plan to turn on HTTP/2 over TLS (h2, ALPN) with a self-signed certificate in this server."
- **Affected files/sections:** the implementation and documentation assistance affected `TimeComponent.kt`, test files and `REPORT.md`.
- **Validation steps:** I ran the automated tests, inspected the generated certificate and keystore, reviewed the HTTP response and TLS handshake output from curl and manually reviewed the code and functionality. I also ran the project checks before submission and reviewed the final diff.
- **Citations:** none.
- **Human-reviewed:** I reviewed the generated implementation and test results and retained content negotiation between HTML and Problem Details. I also rejected too complex tests.


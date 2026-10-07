# Lab 2 Web Server

Individual starter for Web Engineering 2026–27. Stack matches the group project: **Java 25 LTS**, **Kotlin 2.4.0**, **Spring Boot 4.1.0**, **Gradle 9.6.0**.

The assignment, AI rules, and deadline are in [`docs/GUIDE.md`](docs/GUIDE.md). Fill [`REPORT.md`](REPORT.md) before you submit. Delivery is the Moodle zip only (`docs/GUIDE.md`).

## Run

Java 25 is required (`./gradlew` uses the wrapper). OpenSSL is required for the TLS task. GitHub Codespaces is optional (`docs/GUIDE.md`). Clone this course repository; you do not fork it to submit.

```bash
git clone https://github.com/UNIZAR-30246-WebEngineering/lab2-web-server.git
cd lab2-web-server
./gradlew check
./gradlew bootRun
```

Before the TLS task the app listens on <http://localhost:8080>. After that task it listens on <https://127.0.0.1:8443>.

```bash
./gradlew test
./gradlew ktlintCheck
```

## Layout

```
src/main/kotlin/es/unizar/webeng/lab2/Application.kt
src/test/kotlin/es/unizar/webeng/lab2/ApplicationTests.kt
docs/GUIDE.md
```

## Additions

- ***main* branch**: 
    - **Error page:** Renders an HTML error page for browser requests and displays the HTTP status and requested path.
    - **`/time` endpoint:** Returns the current server time as JSON and accepts an optional IANA time zone through the `zone` parameter.
    - **TLS configuration:** Serves the application over HTTPS on port 8443 and negotiates HTTP/2 using a local self-signed certificate.
- ***feature/rfc_9457* branch**: 
    - Separates browser error pages from API errors, which are returned as RFC 9457 Problem Details with appropriate status and error information.
- ***feature/content_negotiation* branch**: 
    - Lets clients select JSON, plain text or HTML and adds localized messages, transparent gzip compression and ETag/Last-Modified cache validation.


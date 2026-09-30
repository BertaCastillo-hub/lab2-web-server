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

- Error page
- `/time` endpoint
- TLS configuration.


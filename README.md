# Mock Evaluation System (backend)

Spring Boot API for the FirstBit Mock Evaluation portal.

## Run locally

1. Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties` and fill in local values. Do not commit that file.
2. Java 17 and Maven:
   ```bash
   ./mvnw -DskipTests package
   java -jar target/mock-evaluation-system-0.0.1-SNAPSHOT.jar
   ```
3. Default port is `8080`. Do not activate the `prod` profile unless you intend to use its environment-variable database settings.

## Notes

- JWT, mail, database passwords, and `groq.api.key` belong only in local or server sidecar config, never in git.
- Frontend lives in a separate repository.

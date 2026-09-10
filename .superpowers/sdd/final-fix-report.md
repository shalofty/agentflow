# Final Branch Review Fix Report

## Changes

- Form validation now rejects select values outside configured options and non-ISO local dates, with unit coverage for invalid and valid payloads.
- The SOAP client uses configurable 1-second connect and 2-second read timeouts. SOAP I/O and timeout failures are converted to retryable integration failures.
- Every handled API `ProblemDetail` includes the MDC correlation ID when present. Unexpected exceptions return a sanitized 500 `ProblemDetail` and are logged.
- Submission logs cover submit start, every integration attempt result, and terminal status with correlation ID, application ID, integration, and attempt fields.
- Maven Failsafe runs `*IT` classes during `./mvnw verify`; the README documents this command.
- Application status badges now distinguish success, failure, review, in-progress, and draft states.
- Starting a different workflow clears stale form values and page state.

## Verification

- `JAVA_HOME=/opt/homebrew/opt/openjdk ./mvnw -Dtest=FormDefinitionValidatorTest,PolicyEligibilitySoapClientTest,GlobalExceptionHandlerTest test`: 13 passed.
- `JAVA_HOME=/opt/homebrew/opt/openjdk ./mvnw test`: 17 passed.
- `npm test`: 5 passed.
- `npm run lint`: completed with two pre-existing React effect warnings.
- `npm run build`: passed.
- `git diff --check`: passed.

## Docker Gap

Docker is unavailable in this environment. `JAVA_HOME=/opt/homebrew/opt/openjdk ./mvnw verify` reached Failsafe and selected all `*IT` classes, confirming the Maven wiring, but finished with six Docker environment errors and ten Docker-dependent tests skipped. Re-run `./mvnw verify` with Docker available for complete integration verification.

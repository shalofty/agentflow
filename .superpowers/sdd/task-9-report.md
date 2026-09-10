# Task 9 Report

Status: DONE_WITH_CONCERNS

Implemented submit orchestration with explicit short transaction boundaries, atomic DRAFT claiming,
REST verification retries, durable per-attempt activity records, stub eligibility mapping, correlation
IDs, submit/activity endpoints, and basic portal submit/status UI.

Verification:
- `mvn test`: passed. Docker-dependent integration tests auto-skipped because no Docker daemon exists.
- `CustomerVerificationClientTest`: runs against MockWebServer as part of the passing backend suite.
- `ApplicationSubmitIT`: covers approved, unverified/manual review, retries with FAILED/FAILED/SUCCESS,
  correlation propagation, activity retrieval, and double-submit 409; compiled but skipped locally due
  to unavailable Docker/PostgreSQL Testcontainers.
- `npm test -- --run`: 3 tests passed.
- `npm run lint`: passed with two pre-existing React set-state-in-effect warnings.
- `npm run build`: passed.

Concern: Run `ApplicationSubmitIT` with Docker available before release to execute the full HTTP,
Flyway, PostgreSQL, and MockWebServer path.

## Task 9 review fix (integration name + httpStatus)

- `ApplicationSubmitIT.retryableFailuresCreateFailedRowsBeforeSuccess`: filter now uses
  `"CustomerVerification"` (matches `ApplicationSubmitService` activity records); asserts
  `httpStatus` 503/500/200 on verification attempts.
- `RetryableIntegrationException` carries optional `httpStatus`; `CustomerVerificationClient`
  sets it for 5xx responses; `ApplicationSubmitService.recordFailure` persists it on FAILED rows.
- Focused unit tests: `mvn test -Dtest=CustomerVerificationClientTest,FormDefinitionValidatorTest`
  — 7 passed (no Docker).

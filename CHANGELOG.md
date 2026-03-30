# Changelog
All notable changes to this project are documented in this file.
## [5.5.1] - 2026-03-30
### Changed
- Upgraded to Spring Boot `4.0.4`.
- Upgraded Spring Framework to `7.0.6` and Spring Security to `7.0.4`.
- Moved test stack to JUnit Jupiter `6.0.3`.
- Updated Jakarta API baselines (`jakarta.annotation-api` to `3.0.0`, `jakarta.servlet-api` to `6.1.0`).
- Set Maven compiler target to Java `21`.
### Fixed
- Updated `ResponseErrorHandler` integration for Spring Framework 7 signature changes.
- Updated response header handling to use `HttpHeaders.asMultiValueMap()`.
- Reworked Spring Boot REST client factory after `RestTemplateBuilder` removal in Spring Boot 4.
- Filtered Spring Security 7 factor authorities from application permissions mapping.
- Restored Spring `@Import` registrations for bean context, tenant scope, and datastore configuration.
[5.5.1]: https://github.com/holon-platform/holon-core/releases/tag/v5.5.1

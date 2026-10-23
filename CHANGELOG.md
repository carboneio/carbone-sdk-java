# Changelog

All notable changes to this project will be documented in this file. This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.1.0
- Security: replaced `feign-okhttp` with `feign-java11` (JDK `HttpClient`). This removes `okhttp`, `okio` and `kotlin-stdlib` from the dependency tree, which some scanners flagged for CVE-2020-29582 ([#11](https://github.com/carboneio/carbone-sdk-java/issues/11)).
- Security: updated `jackson-databind` from `2.18.3` to `2.22.0` to fix multiple CVEs (including CVE-2026-54512, CVE-2026-54513, CVE-2026-68497, CVE-2026-91776 and CVE-2026-91777, High severity).
- Security: Feign logging is now disabled (`NONE` instead of `FULL`). It previously logged the `Authorization` header (API token) and full request/response bodies when SLF4J was at DEBUG level.
- `CarboneResponse` and `CarboneResponseData` are now public, so the API error code and message can be read via `CarboneException.getCarboneResponse()`.
- CI: added a GitHub Actions workflow running the tests on Java 11, 17, 21 and 25.

## 2.0.4
- Security: Updated `assertj-core` to `3.27.7` to fix CVE-2026-24400 (High severity).

## 2.0.3 - 2026-03-10
- Security: Updated dependencies to fix CVE-2022-42003 (jackson-databind), CVE-2022-45690/CVE-2023-5072 (org.json, removed), CVE-2023-24998 (feign-form), CVE-2026-24400 (assertj-core).
- Updated all OpenFeign modules to `13.5`, Lombok to `1.18.38`, Mockito to `5.14.2`, JaCoCo to `0.8.13`, slf4j to `2.0.17`.
- Build: switched to `maven.compiler.release=11`, added explicit Lombok annotation processor path.
- Tests: migrated all tests from JUnit 4 to JUnit 5 (`junit-jupiter 5.12.1`).

## 2.0.2
- Build: fixed `central-publishing-maven-plugin` configuration (removed duplicate `serverId` entry).
- CI: updated GitHub Actions publish workflow with correct environment variables and pipeline configuration.

## 2.0.1
- Setup automatic deployment with Github Action.
- Updated readme examples.

## 2.0.0
- Released on 2024/07/04: The package was originally made by Benjamin COLOMBE from Tennaxia. The Carbone team is now maintaining the SDK. This version brings all missing functions to interact with the Carbone API.
- The package is now available under the artifact `io.carbone`, [link to the Central Maven repository](https://central.sonatype.com/artifact/io.carbone/carbone-sdk).
- Added function `render`: Provide a template path as `String` and the JSON data-set as `String`, and the function will execute 3 actions: add the template, generate the document, and download the generated document.
- Added function `getStatus`: It return the current status and the version of the API as `String`.
- Added function `getTemplate`: Provide a template ID as `String` and it returns the file as `byte[]`.
- Modified for the `create` constructor: it takes two optional arguments: the `Carbone API key` and the `API version`.
- Modified for the `deleteTemplate`: it returns whether the request succeeded as a `Boolean`.
- Modified for the `renderReport`: it does not take a Map for render option anymore as argument.
- Renamed `SetCarboneUri` to `SetCarboneUrl`. 
- Added `GetCarboneUrl`. 
- Modified the `getReport` and `render` functions: They now return a `CarboneDocument` type to get the document as `Byte[]` and the document name as `String`.
- Added units tests.

### v1.0.0
- Released on 2023/06/06
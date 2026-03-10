# CLAUDE.md — Carbone SDK Java

## Build & Test Commands

```sh
mvn clean compile          # compile only
mvn clean test             # run all tests + generate JaCoCo coverage report
mvn clean package          # build the JAR (skips signing/publishing)
```

Coverage report is generated automatically at `target/site/jacoco/index.html` after `mvn clean test`.

## Project Structure

```
src/
  main/java/io/carbone/
    CarboneServicesFactory.java   # entry point / singleton factory (enum pattern)
    CarboneServices.java          # ICarboneServices implementation
    CarboneFeignClientBuilder.java # shared Feign.builder() config (auth headers, OkHttp)
    CarboneDecoder.java           # custom Feign decoder (binary + JSON responses)
    CarboneErrorDecoder.java      # custom Feign error decoder (maps HTTP errors to CarboneException)
    ICarboneServices.java         # public API interface
    ICarboneTemplateClient.java   # Feign interface for /template endpoints
    ICarboneRenderClient.java     # Feign interface for /render endpoints
    ICarboneStatusClient.java     # Feign interface for status endpoint
    CarboneDocument.java          # return type: byte[] content + String name (Lombok)
    CarboneFileResponse.java      # internal binary response wrapper (Lombok)
    CarboneResponse.java          # JSON API response DTO (Lombok)
    CarboneException.java         # custom exception with HTTP status
  test/java/io/carbone/
    CarboneTest.java              # main integration tests (23 tests, uses Mockito)
    CarboneDecoderTest.java       # CarboneDecoder unit tests
    CarboneErrorDecoderTest.java  # CarboneErrorDecoder unit tests
```

## Key Architecture

- **OpenFeign 13.5** is used for all HTTP calls (3 separate Feign clients: template, render, status).
- **Template client** uses `FormEncoder` (multipart upload). **Render/Status clients** use `GsonEncoder`.
- **CarboneDecoder** handles two response types: `CarboneFileResponse` (raw bytes) and `CarboneDocument` (bytes + filename from `content-disposition` header), falling back to `GsonDecoder` for JSON.
- **Lombok** generates all constructors, getters, and builders on model classes. It must be declared as an `annotationProcessorPath` in the compiler plugin (required since Maven Compiler Plugin 3.13+).

## Dependency Notes

| Dependency | Version | Notes |
|---|---|---|
| feign-* | 13.5 | All modules must stay aligned on the same version |
| jackson-databind | via `${jackson.version}` | Used only in CarboneErrorDecoder for error parsing |
| feign-gson | 13.5 | Used for encoding/decoding JSON bodies |
| feign-jackson | removed | Was unused — code uses feign-gson |
| org.json | removed | Was unused — no imports in source |
| lombok | `${lombok.version}` | Compile-time only; also declared in annotationProcessorPaths |

## Known Gotchas

- **Lombok + Java 25**: Produces `sun.misc.Unsafe::objectFieldOffset` deprecation warnings at compile time. These are harmless and come from Lombok internals — nothing to fix on our side.
- **Mockito byte-buddy warning**: `Dynamic loading of agents will be disallowed` appears during tests. Harmless; caused by Mockito's inline mock mechanism.
- **feign.Util.toString()** was removed in OpenFeign 12. Use `new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8)` instead.
- **Maven settings.xml** (`~/.m2/settings.xml`): `<server>` must be wrapped in `<servers>`, otherwise Maven emits an `Unrecognised tag` warning.

## Publishing

Publishing to Maven Central is handled by `central-publishing-maven-plugin`. Artifacts are signed by `sign-maven-plugin`. Credentials are in `~/.m2/settings.xml`.

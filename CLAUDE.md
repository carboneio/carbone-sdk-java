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
    CarboneFeignClientBuilder.java # shared Feign.builder() config (auth headers, JDK HttpClient via feign-java11)
    CarboneDecoder.java           # custom Feign decoder (binary + JSON responses)
    CarboneErrorDecoder.java      # custom Feign error decoder (maps HTTP errors to CarboneException)
    ICarboneServices.java         # public API interface
    ICarboneTemplateClient.java   # Feign interface for /template endpoints
    ICarboneRenderClient.java     # Feign interface for /render endpoints
    ICarboneStatusClient.java     # Feign interface for status endpoint
    CarboneDocument.java          # return type: byte[] content + String name (Lombok)
    CarboneFileResponse.java      # internal binary response wrapper (Lombok)
    CarboneResponse.java          # JSON API response DTO for render/legacy template IDs (Lombok)
    CarboneApiResponse.java       # generic JSON envelope for v5 endpoints (data, hasMore, nextCursor)
    CarboneTemplate.java          # v5 template metadata (id, versionId, legacy templateId...)
    CarboneTemplateOptions.java   # upload options (versioning, id, name, tags...)
    CarboneTemplateUpdate.java    # PATCH /template body
    CarboneTemplateQuery.java     # GET /templates filters
    CarboneTemplateList.java      # one page of templates
    CarboneRenderOptions.java     # per-request render headers (webhook, custom)
    CarboneException.java         # custom exception with HTTP status
  test/java/io/carbone/
    CarboneTest.java              # main tests (uses Mockito)
    CarboneV5Test.java            # v5 service methods (Mockito)
    CarboneHttpTest.java          # Feign wiring against a local JDK HttpServer (paths, headers, multipart order)
    CarboneDecoderTest.java       # CarboneDecoder unit tests
    CarboneErrorDecoderTest.java  # CarboneErrorDecoder unit tests
```

## Key Architecture

- **OpenFeign 13.5** is used for all HTTP calls (3 separate Feign clients: template, render, status). The template and status clients target the API root URL, the render client targets `/render`.
- **Carbone API v5** is the default (`carbone-version: 5`). Upload form fields must keep their order: `id` before `template`, `template` last — `addTemplate(Map<String, ?>)` relies on `LinkedHashMap` iteration order.
- The API **ignores multipart text fields that have a `Content-Type` header** (silently: upload falls back to legacy mode). feign-form adds `text/plain` by default, so the template client uses `CarboneFormFieldWriter.formEncoder()`. Don't replace it with `new FormEncoder()`.
- `CarboneLiveTest` runs against the real API only when `CARBONE_TOKEN` is set: `CARBONE_TOKEN=$(cat ~/.config/carbone/test-token) mvn test -Dtest=CarboneLiveTest`.
- Javadoc runs on the sources before Lombok: `{@link}` to a Lombok-generated getter breaks `mvn package`, use `{@code}`.
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

Publishing to Maven Central runs in CI via `.github/workflows/publish.yml` (`mvn deploy` with `central-publishing-maven-plugin`, `autoPublish=true`; artifacts signed by `sign-maven-plugin`).

Release steps:
1. Bump `<version>` in `pom.xml` and add a `CHANGELOG.md` entry; merge to `master` once the Tests workflow passes (publish runs with `-DskipTests`).
2. `gh release create vX.Y.Z --target master` (triggers the workflow), or `gh workflow run publish.yml --ref master`. A release runs the workflow file as it is at the tag.
3. The job ends once Central has *validated* the bundle; the version appears on `repo1.maven.org` 10–30 min later.

CI gotchas:
- Secrets (`CENTRAL_USERNAME`/`CENTRAL_PASSWORD` = Central **user token**, `SIGN_KEY` = ASCII-armored private key, `SIGN_KEY_PASS`) live in the **`MAVEN CENTRAL` environment**, not repo secrets. The job must declare `environment: MAVEN CENTRAL`, otherwise they are empty → 401 on upload.
- `sign-maven-plugin` reads the key from the `SIGN_KEY` env var, not from the gpg keyring set up by `setup-java`. If it is missing the plugin logs `Sign - key not found - skip execution` and Central rejects the unsigned bundle.
- `central-publishing-maven-plugin` 0.5.0 bundled `maven-metadata-local.xml`/`_remote.repositories` with recent Maven → `content that does NOT have a .pom file` (fixed by 0.12.0). Keep it up to date.
- To inspect the bundle locally without uploading: `mvn deploy -DskipTests -DskipUpload=true` (needs a `central` server entry in settings.xml, dummy values are fine), then `unzip -l target/central-publishing/central-bundle.zip`.
- Actions are pinned by commit SHA (repo setting requires it); update the SHA and the `# vX.Y.Z` comment together.

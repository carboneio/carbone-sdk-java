package io.carbone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Runs the v5 methods against the real Carbone API. Skipped unless CARBONE_TOKEN is set:
 * CARBONE_TOKEN=... mvn test -Dtest=CarboneLiveTest
 * Templates are uploaded with an expiration date and deleted at the end.
 */
@EnabledIfEnvironmentVariable(named = "CARBONE_TOKEN", matches = ".+")
public class CarboneLiveTest {

    static final String JSON = "{\"data\":{\"invoiceId\":\"SDK-LIVE\",\"name\":\"Carbone SDK Java\"},"
        + "\"convertTo\":\"pdf\",\"reportName\":\"invoice-{d.invoiceId}\"}";

    ICarboneServices carboneServices = CarboneServicesFactory.CARBONE_SERVICES_FACTORY_INSTANCE.create(System.getenv("CARBONE_TOKEN"));

    private static void log(String message) {
        System.out.println("[live] " + message);
    }

    private static boolean isPdf(CarboneDocument document) {
        return new String(document.getFileContent(), 0, 4, StandardCharsets.US_ASCII).equals("%PDF");
    }

    @Test
    public void Test_V5_Template_Lifecycle_And_Render() throws Exception {
        assertTrue(carboneServices.getStatus().contains("\"version\":\"5."), "status should report a v5 API");

        byte[] file = Files.readAllBytes(Paths.get(System.getProperty("user.dir"), "src/test/java/io/carbone/template3.odt"));
        String tag = "sdk-java-" + Instant.now().getEpochSecond();
        long expireAt = Instant.now().getEpochSecond() + 3600;

        CarboneTemplate first = carboneServices.addTemplate(file, CarboneTemplateOptions.builder()
            .versioning(true).name("SDK Java live test").comment("created by CarboneLiveTest")
            .category("sdk-java-tests").tags(Arrays.asList(tag, "sdk-java")).expireAt(expireAt).build());
        log("upload: " + first);
        assertNotNull(first.getId());
        assertNotNull(first.getVersionId());
        try {
            log("versionId == local SHA-256 of the file: " + first.getVersionId().equals(carboneServices.generateTemplateId(
                Paths.get(System.getProperty("user.dir"), "src/test/java/io/carbone/template3.odt").toString())));

            byte[] secondFile = Arrays.copyOf(file, file.length);
            CarboneTemplate second = carboneServices.addTemplate(secondFile, CarboneTemplateOptions.builder()
                .versioning(true).id(first.getId()).name("SDK Java live test v2").expireAt(expireAt)
                .deployedAt(CarboneTemplateOptions.DEPLOY_NOW).build());
            log("second version: " + second);
            assertEquals(first.getId(), second.getId());

            CarboneTemplate updated = carboneServices.updateTemplate(first.getVersionId(), CarboneTemplateUpdate.builder()
                .name("SDK Java live test renamed").build());
            log("update: " + updated);

            CarboneTemplateList versions = carboneServices.listTemplates(CarboneTemplateQuery.builder()
                .id(first.getId()).includeVersions(true).build());
            log("list by id with versions: " + versions);
            assertTrue(versions.getTemplates().stream().allMatch(t -> first.getId().equals(t.getId())));
            assertTrue(versions.getTemplates().size() >= 1);

            CarboneTemplateList page = carboneServices.listTemplates(CarboneTemplateQuery.builder().limit(1).build());
            log("first page with limit 1: hasMore=" + page.isHasMore() + " nextCursor=" + page.getNextCursor());
            assertEquals(1, page.getTemplates().size());
            if (page.isHasMore()) {
                CarboneTemplateList next = carboneServices.listTemplates(CarboneTemplateQuery.builder().limit(1).cursor(page.getNextCursor()).build());
                log("second page: " + next.getTemplates().stream().map(CarboneTemplate::getVersionId).collect(Collectors.toList()));
            }

            List<String> categories = carboneServices.listTemplateCategories();
            List<String> tags = carboneServices.listTemplateTags();
            log("categories: " + categories + " / tags contain test tag: " + tags.contains(tag));
            assertTrue(categories.contains("sdk-java-tests"));
            assertTrue(tags.contains(tag));

            CarboneDocument downloaded = carboneServices.renderAndDownload(JSON, first.getId());
            log("renderAndDownload: " + downloaded.getName() + " " + downloaded.getFileContent().length + " bytes");
            assertTrue(isPdf(downloaded));

            String renderId = carboneServices.renderReport(JSON, first.getVersionId(), null);
            CarboneDocument report = carboneServices.getReport(renderId);
            log("renderReport(versionId) + getReport: " + report.getName() + " " + report.getFileContent().length + " bytes");
            assertTrue(isPdf(report));

            CarboneDocument inline = carboneServices.renderInline(JSON, file);
            log("renderInline: " + inline.getName() + " " + inline.getFileContent().length + " bytes");
            assertTrue(isPdf(inline));

            String webhookRenderId = carboneServices.renderReport(JSON, first.getId(), CarboneRenderOptions.builder()
                .webhookUrl("https://example.com/carbone-sdk-java-live-test").webhookHeader("x-test", "1").build());
            log("renderReport with webhook returned: " + webhookRenderId);
            assertNull(webhookRenderId);

            CarboneDocument legacyRender = carboneServices.render(JSON, Paths.get(System.getProperty("user.dir"), "src/test/java/io/carbone/template3.odt").toString());
            log("legacy render(path) on v5: " + legacyRender.getName());
            assertTrue(isPdf(legacyRender));
        } finally {
            log("delete template: " + carboneServices.deleteTemplate(first.getId()));
        }
    }
}

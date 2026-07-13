package com.storage.common.db;

import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationHistoryTest {

    private static final int PUBLISHED_V001_CHECKSUM = 311517955;
    private static final String V001 = "db/migration/V001__baseline_schema.sql";
    private static final String V007 = "db/migration/V007__material_ledger_natural_key_unique.sql";
    private static final String V013 = "db/migration/V013__optimize_navigation_hierarchy.sql";
    private static final String V017 = "db/migration/V017__flatten_system_management_menus.sql";
    private static final String V018 = "db/migration/V018__strip_pristine_demo_data_when_disabled.sql";
    private static final String V019 = "db/migration/V019__strip_pristine_demo_data_unconditionally.sql";
    private static final String V024 = "db/migration/V024__normalize_email_lowercase.sql";
    private static final String V025 = "db/migration/V025__sys_user_email_unique.sql";
    private static final String V026 = "db/migration/V026__registration_verification_code.sql";

    @Test
    void publishedBaselineMigration_keepsOriginalFlywayChecksum() {
        int checksum = ChecksumCalculator.calculate(new ClasspathMigrationResource(V001));

        assertThat(checksum).isEqualTo(PUBLISHED_V001_CHECKSUM);
    }

    @Test
    void materialLedgerNaturalKeyChange_staysInIncrementalMigration() {
        String baselineSql = readResource(V001);
        String incrementalSql = readResource(V007);

        assertThat(baselineSql).doesNotContain("uk_material_ledger_natural_key");
        assertThat(incrementalSql).contains("uk_material_ledger_natural_key");
    }

    @Test
    void systemMenuFlattenMigration_restoresSiblingRoutesUnderCatalog200() {
        String migrationSql = readResource(V013);

        assertThat(migrationSql).contains("WHERE id = 202");
        assertThat(migrationSql).contains("path = '/system/roles'");
        assertThat(migrationSql).contains("component_key = 'views/system/UserManageView.vue'");
    }

    @Test
    void systemMenuFlattenMigration_reappliesFlatRoutesByMenuId() {
        String migrationSql = readResource(V017);

        assertThat(migrationSql).contains("WHERE id = 201");
        assertThat(migrationSql).contains("WHERE id = 202");
        assertThat(migrationSql).contains("WHERE id = 203");
        assertThat(migrationSql).contains("WHERE id = 204");
        assertThat(migrationSql).contains("path = '/system/customers'");
    }

    @Test
    void demoDataCleanupMigration_isGuardedByLoadDemoDataPlaceholder() {
        String migrationSql = readResource(V018);

        assertThat(migrationSql).contains("${loadDemoData}");
        assertThat(migrationSql).contains("@strip_pristine_demo");
        assertThat(migrationSql).contains("DELETE FROM material_ledger");
        assertThat(migrationSql).contains("DELETE FROM design_guide");
        assertThat(migrationSql).contains("DELETE FROM experience_type");
        assertThat(migrationSql).doesNotContain("DELETE FROM sys_user");
        assertThat(migrationSql).doesNotContain("DELETE FROM sys_menu");
        assertThat(migrationSql).doesNotContain("DELETE FROM sys_role");
    }

    @Test
    void demoDataCleanupMigration_stripsPristineSeedWithoutEnvironmentSwitch() {
        String migrationSql = readResource(V019);

        assertThat(migrationSql).doesNotContain("${loadDemoData}");
        assertThat(migrationSql).contains("@strip_pristine_demo");
        assertThat(migrationSql).contains("DELETE FROM material_ledger");
        assertThat(migrationSql).contains("DELETE FROM design_guide");
        assertThat(migrationSql).contains("DELETE FROM experience_type");
        assertThat(migrationSql).doesNotContain("DELETE FROM sys_user");
        assertThat(migrationSql).doesNotContain("DELETE FROM sys_menu");
        assertThat(migrationSql).doesNotContain("DELETE FROM sys_role");
    }

    @Test
    void normalizeEmailMigration_lowercasesExistingUserAndCustomerEmails() {
        String migrationSql = readResource(V024);

        assertThat(migrationSql).contains("UPDATE sys_user");
        assertThat(migrationSql).contains("UPDATE sys_customer");
        assertThat(migrationSql).contains("LOWER(TRIM(email))");
        assertThat(migrationSql).contains("WHERE email IS NOT NULL");
    }

    @Test
    void sysUserEmailUniqueMigration_blocksDuplicatesBeforeAddingIndex() {
        String migrationSql = readResource(V025);

        assertThat(migrationSql).contains("sys_user_duplicate_email_guard");
        assertThat(migrationSql).contains("uk_sys_user_email");
        assertThat(migrationSql).doesNotContain("DELETE FROM sys_user");
    }

    @Test
    void registrationVerificationCodeMigration_createsPendingRegistrationTable() {
        String migrationSql = readResource(V026);

        assertThat(migrationSql).contains("registration_verification_code");
        assertThat(migrationSql).contains("email VARCHAR(128) NOT NULL");
        assertThat(migrationSql).contains("code_hash");
    }

    private static String readResource(String path) {
        try (InputStream input = openResource(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }

    private static InputStream openResource(String path) {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("Missing classpath resource " + path);
        }
        return input;
    }

    private static final class ClasspathMigrationResource extends LoadableResource {

        private final String path;

        private ClasspathMigrationResource(String path) {
            this.path = path;
        }

        @Override
        public Reader read() {
            return new InputStreamReader(openResource(path), StandardCharsets.UTF_8);
        }

        @Override
        public String getAbsolutePath() {
            return path;
        }

        @Override
        public String getAbsolutePathOnDisk() {
            return path;
        }

        @Override
        public String getFilename() {
            int index = path.lastIndexOf('/');
            return index >= 0 ? path.substring(index + 1) : path;
        }

        @Override
        public String getRelativePath() {
            return path;
        }
    }
}

package com.entitykart.monolith.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * FlywayRepairConfig — Smart schema repair strategy.
 *
 * <p>Automatically detects and fixes schema corruption caused by:
 * - A pre-existing database with wrong column names (e.g., from an old
 *   ddl-auto:create run) that Flyway's CREATE TABLE IF NOT EXISTS skipped.
 * - Flyway baseline-on-migrate silently skipping migrations.
 *
 * <p>Logic:
 * 1. If the 'users' table exists but has NO 'id' column → schema is corrupted
 *    → run flyway.clean() to drop ALL tables → re-run all 19 migrations fresh.
 * 2. If 'users' table does NOT exist → fresh database → run migrations normally.
 * 3. If 'users' table exists WITH 'id' column → healthy schema → just migrate.
 *
 * <p>This is a one-time self-healing mechanism. Once the schema is correct,
 * the condition never triggers again.
 *
 * @author Md Sadique Amin
 */
@Configuration
public class FlywayRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairConfig.class);

    @Bean
    public FlywayMigrationStrategy smartRepairMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            boolean corrupted = isSchemaCorrupted(dataSource);
            if (corrupted) {
                log.warn("╔══════════════════════════════════════════════════════════════╗");
                log.warn("║  SCHEMA REPAIR: Detected corrupted schema (missing 'id'      ║");
                log.warn("║  column in users table). Running flyway.clean() + migrate(). ║");
                log.warn("╚══════════════════════════════════════════════════════════════╝");
                flyway.clean();
                log.info("✅ Flyway clean complete — all old tables dropped.");
            } else {
                log.info("✅ Schema check passed — no repair needed.");
            }
            flyway.migrate();
        };
    }

    /**
     * Returns true if the 'users' table exists but is missing the 'id' primary key column.
     * This indicates a corrupted schema that needs a clean rebuild.
     */
    private boolean isSchemaCorrupted(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();

            // Step 1: Check if 'users' table exists at all
            try (ResultSet tables = meta.getTables(catalog, null, "users", new String[]{"TABLE"})) {
                if (!tables.next()) {
                    log.info("Schema check: 'users' table not found — fresh database, no repair needed.");
                    return false;
                }
            }

            // Step 2: 'users' table exists — check for 'id' column
            try (ResultSet cols = meta.getColumns(catalog, null, "users", "id")) {
                if (!cols.next()) {
                    log.warn("Schema check: 'users' table exists but 'id' column is MISSING — repair required!");
                    return true;
                }
            }

            log.info("Schema check: 'users' table exists with 'id' column — schema is healthy.");
            return false;

        } catch (Exception e) {
            log.warn("Schema check failed with exception (will let Flyway decide): {}", e.getMessage());
            return false;
        }
    }
}

package com.veccy.rest.handlers;

import com.veccy.rest.VeccyRestServer;
import com.veccy.rest.config.RestConfig;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ImportExportHandler.
 */
class ImportExportHandlerTest {

    private Javalin createTestApp(int port) {
        RestConfig config = new RestConfig.Builder()
                .port(port)
                .build();
        VeccyRestServer server = new VeccyRestServer(config);
        return server.getApp();
    }

    @Test
    void testImportData() {
        Javalin app = createTestApp(8300);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database first
            String createDbBody = """
                {
                    "name": "import_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // Import data with actual vectors
            String importBody = """
                {
                    "vectors": [
                        {
                            "vector": [1.0, 2.0, 3.0],
                            "metadata": {"label": "vector1"}
                        },
                        {
                            "vector": [4.0, 5.0, 6.0],
                            "metadata": {"label": "vector2"}
                        }
                    ]
                }
                """;

            var response = client.post("/api/v1/databases/import_db/import", importBody);
            assertEquals(201, response.code());
            String body = response.body().string();
            assertTrue(body.contains("imported"));
            assertTrue(body.contains("2"));
        });
    }

    @Test
    void testImportDataDatabaseNotFound() {
        Javalin app = createTestApp(8301);

        JavalinTest.test(app, (testServer, client) -> {
            String importBody = """
                {
                    "format": "json",
                    "data": []
                }
                """;

            var response = client.post("/api/v1/databases/nonexistent/import", importBody);
            assertEquals(404, response.code());
            String body = response.body().string();
            assertTrue(body.contains("not found"));
        });
    }

    @Test
    void testExportData() {
        Javalin app = createTestApp(8302);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database first
            String createDbBody = """
                {
                    "name": "export_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // Export data (now fully implemented)
            var response = client.get("/api/v1/databases/export_db/export");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("export_db"));
            assertTrue(body.contains("stats"));
            assertTrue(body.contains("vectors"));
            assertTrue(body.contains("count"));
        });
    }

    @Test
    void testExportDataDatabaseNotFound() {
        Javalin app = createTestApp(8303);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/api/v1/databases/nonexistent/export");
            assertEquals(404, response.code());
            String body = response.body().string();
            assertTrue(body.contains("not found"));
        });
    }

    @Test
    void testExportDataWithVectors() {
        Javalin app = createTestApp(8304);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database and insert vectors
            String createDbBody = """
                {
                    "name": "export_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            String insertBody = """
                {
                    "vectors": [[1.0, 2.0, 3.0], [4.0, 5.0, 6.0]]
                }
                """;
            client.post("/api/v1/databases/export_db/vectors", insertBody);

            // Export data - should now include actual vectors
            var response = client.get("/api/v1/databases/export_db/export");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("export_db"));
            assertTrue(body.contains("stats"));
            assertTrue(body.contains("vectors"));
            // Should have exported 2 vectors
            assertTrue(body.contains("\"count\":2") || body.contains("\"count\": 2"));
        });
    }

    @Test
    void testImportDataWithEmptyPayload() {
        Javalin app = createTestApp(8305);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database first
            String createDbBody = """
                {
                    "name": "import_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // Try to import with empty payload
            String importBody = "{}";

            var response = client.post("/api/v1/databases/import_db/import", importBody);
            // Should return 400 (bad request) - vectors are required
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("required"));
        });
    }

    @Test
    void testExportDataEmptyDatabase() {
        Javalin app = createTestApp(8306);

        JavalinTest.test(app, (testServer, client) -> {
            // Create empty database
            String createDbBody = """
                {
                    "name": "empty_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // Export empty database
            var response = client.get("/api/v1/databases/empty_db/export");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("empty_db"));
            assertTrue(body.contains("stats"));
        });
    }

    @Test
    void testImportDataMultipleTimes() {
        Javalin app = createTestApp(8307);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database
            String createDbBody = """
                {
                    "name": "import_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            String importBody = """
                {
                    "vectors": [
                        {
                            "vector": [1.0, 2.0, 3.0]
                        }
                    ]
                }
                """;

            // First import attempt
            var response1 = client.post("/api/v1/databases/import_db/import", importBody);
            assertEquals(201, response1.code());

            // Second import attempt - should also succeed
            var response2 = client.post("/api/v1/databases/import_db/import", importBody);
            assertEquals(201, response2.code());
        });
    }

    @Test
    void testExportDataMultipleTimes() {
        Javalin app = createTestApp(8308);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database
            String createDbBody = """
                {
                    "name": "export_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // First export
            var response1 = client.get("/api/v1/databases/export_db/export");
            assertEquals(200, response1.code());

            // Second export
            var response2 = client.get("/api/v1/databases/export_db/export");
            assertEquals(200, response2.code());
        });
    }

    @Test
    void testExportDataIncludesStats() {
        Javalin app = createTestApp(8309);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database with vectors
            String createDbBody = """
                {
                    "name": "stats_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            String insertBody = """
                {
                    "vectors": [[1.0, 2.0, 3.0]]
                }
                """;
            client.post("/api/v1/databases/stats_db/vectors", insertBody);

            // Export and verify stats are included
            var response = client.get("/api/v1/databases/stats_db/export");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("stats"));
            assertTrue(body.contains("stats_db"));
        });
    }

    @Test
    void testExportDataWithLimit() {
        Javalin app = createTestApp(8310);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database with multiple vectors
            String createDbBody = """
                {
                    "name": "limit_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            String insertBody = """
                {
                    "vectors": [[1.0, 2.0, 3.0], [4.0, 5.0, 6.0], [7.0, 8.0, 9.0]]
                }
                """;
            client.post("/api/v1/databases/limit_db/vectors", insertBody);

            // Export with limit=2
            var response = client.get("/api/v1/databases/limit_db/export?limit=2");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("\"count\":2") || body.contains("\"count\": 2"));
        });
    }

    @Test
    void testExportDataInvalidLimit() {
        Javalin app = createTestApp(8311);

        JavalinTest.test(app, (testServer, client) -> {
            String createDbBody = """
                {
                    "name": "test_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // Try invalid limit
            var response = client.get("/api/v1/databases/test_db/export?limit=invalid");
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid"));
        });
    }

    @Test
    void testImportExportRoundTrip() {
        Javalin app = createTestApp(8312);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database
            String createDbBody = """
                {
                    "name": "roundtrip_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // Import vectors
            String importBody = """
                {
                    "vectors": [
                        {
                            "vector": [1.0, 2.0, 3.0],
                            "metadata": {"label": "test1"}
                        },
                        {
                            "vector": [4.0, 5.0, 6.0],
                            "metadata": {"label": "test2"}
                        }
                    ]
                }
                """;
            var importResponse = client.post("/api/v1/databases/roundtrip_db/import", importBody);
            assertEquals(201, importResponse.code());

            // Export and verify
            var exportResponse = client.get("/api/v1/databases/roundtrip_db/export");
            assertEquals(200, exportResponse.code());
            String exportBody = exportResponse.body().string();
            assertTrue(exportBody.contains("\"count\":2") || exportBody.contains("\"count\": 2"));
            assertTrue(exportBody.contains("test1"));
            assertTrue(exportBody.contains("test2"));
        });
    }

    @Test
    void testImportDataInvalidDimensions() {
        Javalin app = createTestApp(8313);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database with 3 dimensions
            String createDbBody = """
                {
                    "name": "dim_db",
                    "dimensions": 3
                }
                """;
            client.post("/api/v1/databases", createDbBody);

            // Insert one vector to establish dimensions
            String validInsert = """
                {
                    "vectors": [[1.0, 2.0, 3.0]]
                }
                """;
            client.post("/api/v1/databases/dim_db/vectors", validInsert);

            // Try to import vector with wrong dimensions
            String importBody = """
                {
                    "vectors": [
                        {
                            "vector": [1.0, 2.0]
                        }
                    ]
                }
                """;
            var response = client.post("/api/v1/databases/dim_db/import", importBody);
            assertEquals(400, response.code()); // Validation errors return 400, not 500
            String body = response.body().string();
            assertTrue(body.contains("validation failed") || body.contains("dimension"));
        });
    }
}

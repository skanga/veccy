package com.veccy.rest.handlers;

import com.veccy.rest.VeccyRestServer;
import com.veccy.rest.config.RestConfig;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseHandler.
 */
class DatabaseHandlerTest {

    private Javalin createTestApp(int port) {
        RestConfig config = new RestConfig.Builder()
                .port(port)
                .build();
        VeccyRestServer server = new VeccyRestServer(config);
        return server.getApp();
    }

    @Test
    void testCreateDatabase() {
        Javalin app = createTestApp(8001);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "test_db",
                    "dimensions": 128,
                    "indexConfig": {
                        "type": "hnsw",
                        "metric": "cosine"
                    },
                    "storageConfig": {
                        "type": "memory"
                    }
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(201, response.code());
            String body = response.body().string();
            assertTrue(body.contains("test_db"));
            assertTrue(body.contains("created"));
        });
    }

    @Test
    void testCreateDatabaseWithDefaultConfig() {
        Javalin app = createTestApp(8002);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "simple_db",
                    "dimensions": 64
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(201, response.code());
            String body = response.body().string();
            assertTrue(body.contains("simple_db"));
            assertTrue(body.contains("hnsw"));  // Default index type
        });
    }

    @Test
    void testCreateDatabaseWithInvalidName() {
        Javalin app = createTestApp(8003);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "ab",
                    "dimensions": 128
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testCreateDatabaseWithInvalidNameSpecialChars() {
        Javalin app = createTestApp(8004);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "test@db",
                    "dimensions": 128
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testCreateDatabaseWithInvalidDimensions() {
        Javalin app = createTestApp(8005);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "test_db",
                    "dimensions": 0
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid dimensions"));
        });
    }

    @Test
    void testCreateDatabaseWithNegativeDimensions() {
        Javalin app = createTestApp(8006);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "test_db",
                    "dimensions": -10
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid dimensions"));
        });
    }

    @Test
    void testCreateDuplicateDatabase() {
        Javalin app = createTestApp(8007);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "duplicate_db",
                    "dimensions": 128
                }
                """;

            // First creation should succeed
            var response1 = client.post("/api/v1/databases", requestBody);
            assertEquals(201, response1.code());

            // Second creation should fail
            var response2 = client.post("/api/v1/databases", requestBody);
            assertEquals(409, response2.code());
            String body = response2.body().string();
            assertTrue(body.contains("already exists"));
        });
    }

    @Test
    void testListDatabases() {
        Javalin app = createTestApp(8008);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/api/v1/databases");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("success"));
        });
    }

    @Test
    void testListDatabasesWithPagination() {
        Javalin app = createTestApp(8009);

        JavalinTest.test(app, (testServer, client) -> {
            // Create multiple databases
            for (int i = 1; i <= 5; i++) {
                String requestBody = String.format("""
                    {
                        "name": "db_%d",
                        "dimensions": 128
                    }
                    """, i);
                client.post("/api/v1/databases", requestBody);
            }

            // List with pagination
            var response = client.get("/api/v1/databases?page=1&pageSize=2");
            assertEquals(200, response.code());
            String body = response.body().string();
            // Check for paginated response structure
            assertTrue(body.contains("items") || body.contains("data"));
            assertTrue(body.contains("page") || body.contains("success"));
        });
    }

    @Test
    void testGetDatabase() {
        Javalin app = createTestApp(8010);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database first
            String createBody = """
                {
                    "name": "get_test_db",
                    "dimensions": 128
                }
                """;
            client.post("/api/v1/databases", createBody);

            // Get the database
            var response = client.get("/api/v1/databases/get_test_db");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("get_test_db"));
            assertTrue(body.contains("dimensions"));
        });
    }

    @Test
    void testGetDatabaseNotFound() {
        Javalin app = createTestApp(8011);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/api/v1/databases/nonexistent");
            assertEquals(404, response.code());
            String body = response.body().string();
            assertTrue(body.contains("not found"));
        });
    }

    @Test
    void testGetDatabaseWithInvalidName() {
        Javalin app = createTestApp(8012);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/api/v1/databases/ab");
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testDeleteDatabase() {
        Javalin app = createTestApp(8013);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database first
            String createBody = """
                {
                    "name": "delete_test_db",
                    "dimensions": 128
                }
                """;
            client.post("/api/v1/databases", createBody);

            // Delete the database
            var response = client.delete("/api/v1/databases/delete_test_db");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("deleted"));
        });
    }

    @Test
    void testDeleteDatabaseNotFound() {
        Javalin app = createTestApp(8014);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.delete("/api/v1/databases/nonexistent");
            assertEquals(404, response.code());
            String body = response.body().string();
            assertTrue(body.contains("not found"));
        });
    }

    @Test
    void testDeleteDatabaseWithInvalidName() {
        Javalin app = createTestApp(8015);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.delete("/api/v1/databases/ab");
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testGetDatabaseStats() {
        Javalin app = createTestApp(8016);

        JavalinTest.test(app, (testServer, client) -> {
            // Create database first
            String createBody = """
                {
                    "name": "stats_test_db",
                    "dimensions": 128
                }
                """;
            client.post("/api/v1/databases", createBody);

            // Get stats
            var response = client.get("/api/v1/databases/stats_test_db/stats");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("success"));
        });
    }

    @Test
    void testGetDatabaseStatsNotFound() {
        Javalin app = createTestApp(8017);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/api/v1/databases/nonexistent/stats");
            assertEquals(404, response.code());
            String body = response.body().string();
            assertTrue(body.contains("not found"));
        });
    }

    @Test
    void testGetDatabaseStatsWithInvalidName() {
        Javalin app = createTestApp(8018);

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/api/v1/databases/ab/stats");
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testDatabaseNameWithUnderscores() {
        Javalin app = createTestApp(8019);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "test_db_name",
                    "dimensions": 128
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(201, response.code());
        });
    }

    @Test
    void testDatabaseNameWithHyphens() {
        Javalin app = createTestApp(8020);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "test-db-name",
                    "dimensions": 128
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(201, response.code());
        });
    }

    @Test
    void testDatabaseNameConsecutiveUnderscores() {
        Javalin app = createTestApp(8021);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "test__db",
                    "dimensions": 128
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testDatabaseNameMaxLength() {
        Javalin app = createTestApp(8022);

        JavalinTest.test(app, (testServer, client) -> {
            // 64 characters (max allowed)
            String name = "a".repeat(64);
            String requestBody = String.format("""
                {
                    "name": "%s",
                    "dimensions": 128
                }
                """, name);

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(201, response.code());
        });
    }

    @Test
    void testDatabaseNameTooLong() {
        Javalin app = createTestApp(8023);

        JavalinTest.test(app, (testServer, client) -> {
            // 65 characters (exceeds max)
            String name = "a".repeat(65);
            String requestBody = String.format("""
                {
                    "name": "%s",
                    "dimensions": 128
                }
                """, name);

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testDatabaseNameStartsWithHyphen() {
        Javalin app = createTestApp(8024);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "-testdb",
                    "dimensions": 128
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }

    @Test
    void testDatabaseNameEndsWithHyphen() {
        Javalin app = createTestApp(8025);

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "name": "testdb-",
                    "dimensions": 128
                }
                """;

            var response = client.post("/api/v1/databases", requestBody);
            assertEquals(400, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Invalid database name"));
        });
    }
}

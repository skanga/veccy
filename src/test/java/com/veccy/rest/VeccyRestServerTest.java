package com.veccy.rest;

import com.veccy.rest.config.RestConfig;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for VeccyRestServer.
 */
public class VeccyRestServerTest {

    @Test
    public void testServerCreation() {
        RestConfig config = RestConfig.defaultConfig();
        VeccyRestServer server = new VeccyRestServer(config);
        assertNotNull(server);
        assertNotNull(server.getContext());
        assertNotNull(server.getApp());
    }

    @Test
    public void testHealthEndpoint() {
        RestConfig config = new RestConfig.Builder()
                .port(7879)  // Use different port for testing
                .build();

        VeccyRestServer server = new VeccyRestServer(config);
        Javalin app = server.getApp();

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/health");
            // Health endpoint returns 200 if healthy, 503 if not
            // Since no health checks are registered, it returns UNKNOWN (200)
            assertTrue(response.code() == 200 || response.code() == 503);
            assertTrue(response.body().string().contains("status"));
        });
    }

    @Test
    public void testRootEndpoint() {
        RestConfig config = new RestConfig.Builder()
                .port(7880)
                .build();

        VeccyRestServer server = new VeccyRestServer(config);
        Javalin app = server.getApp();

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("Veccy Vector Database"));
            assertTrue(body.contains("version"));
        });
    }

    @Test
    public void testCreateDatabase() {
        RestConfig config = new RestConfig.Builder()
                .port(7881)
                .build();

        VeccyRestServer server = new VeccyRestServer(config);
        Javalin app = server.getApp();

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
    public void testListDatabases() {
        RestConfig config = new RestConfig.Builder()
                .port(7882)
                .build();

        VeccyRestServer server = new VeccyRestServer(config);
        Javalin app = server.getApp();

        JavalinTest.test(app, (testServer, client) -> {
            // Initially should be empty
            var response = client.get("/api/v1/databases");
            assertEquals(200, response.code());
            String body = response.body().string();
            assertTrue(body.contains("success"));
        });
    }

    @Test
    public void testDatabaseNotFound() {
        RestConfig config = new RestConfig.Builder()
                .port(7883)
                .build();

        VeccyRestServer server = new VeccyRestServer(config);
        Javalin app = server.getApp();

        JavalinTest.test(app, (testServer, client) -> {
            var response = client.get("/api/v1/databases/nonexistent");
            assertEquals(404, response.code());
            String body = response.body().string();
            assertTrue(body.contains("not found"));
        });
    }

    @Test
    public void testInsertVectorsWithoutDatabase() {
        RestConfig config = new RestConfig.Builder()
                .port(7884)
                .build();

        VeccyRestServer server = new VeccyRestServer(config);
        Javalin app = server.getApp();

        JavalinTest.test(app, (testServer, client) -> {
            String requestBody = """
                {
                    "vectors": [[0.1, 0.2, 0.3]],
                    "metadata": [{"id": "doc1"}]
                }
                """;

            var response = client.post("/api/v1/databases/nonexistent/vectors", requestBody);
            assertEquals(404, response.code());
        });
    }
}

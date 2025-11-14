package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UpdateCommand.
 */
class UpdateCommandTest {

    private UpdateCommand command;
    private CLIContext context;
    private VectorDBClient client;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        command = new UpdateCommand();
        client = VectorDBFactory.createSimple();
        context = new CLIContext();
        context.setClient(client);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testGetName() {
        assertEquals("update", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Update a vector or its metadata", command.getDescription());
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertTrue(usage.contains("update"));
        assertTrue(usage.contains("--vector"));
        assertTrue(usage.contains("--metadata"));
    }

    @Test
    void testUpdateVector() throws Exception {
        // Insert a vector
        double[][] vectors = {{1.0, 2.0, 3.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        // Update the vector
        command.execute(context, new String[]{id, "--vector", "4.0,5.0,6.0"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated vector"));
        assertTrue(output.contains(id));
    }

    @Test
    void testUpdateVectorWithBrackets() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--vector", "[7.0,8.0]"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }

    @Test
    void testUpdateMetadata() throws Exception {
        double[][] vectors = {{1.0, 2.0, 3.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--metadata", "label=updated"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated vector"));
        assertTrue(output.contains("Updated metadata"));
        assertTrue(output.contains("label=updated"));
    }

    @Test
    void testUpdateMultipleMetadataFields() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{
                id,
                "--metadata", "label=test",
                "--metadata", "value=100",
                "--metadata", "active=true"
        });

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
        assertTrue(output.contains("Updated metadata"));
    }

    @Test
    void testUpdateVectorAndMetadata() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{
                id,
                "--vector", "3.0,4.0",
                "--metadata", "updated=true"
        });

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }

    @Test
    void testUpdateNonExistentVector() throws Exception {
        command.execute(context, new String[]{"nonexistent-id", "--vector", "1.0,2.0"});

        String error = errContent.toString();
        assertTrue(error.contains("Failed to update vector"));
        assertTrue(error.contains("may not exist"));
    }

    @Test
    void testUpdateWithVerboseMode() throws Exception {
        context.setVerbose(true);

        double[][] vectors = {{1.0, 2.0, 3.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--vector", "4.0,5.0,6.0"});

        String output = outContent.toString();
        assertTrue(output.contains("dimension"));
    }

    @Test
    void testUpdateWithShortFlags() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{
                id,
                "-v", "3.0,4.0",
                "-m", "test=value"
        });

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }

    @Test
    void testUpdateMetadataWithInteger() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--metadata", "count=42"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }

    @Test
    void testUpdateMetadataWithDouble() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--metadata", "score=3.14"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }

    @Test
    void testUpdateMetadataWithBoolean() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--metadata", "active=true"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }

    @Test
    void testUpdateMetadataWithString() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--metadata", "name=testName"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }

    @Test
    void testUpdateWithInvalidVector() {
        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{"id1", "--vector", "invalid,data"});
        });
    }

    @Test
    void testUpdateWithNoVectorOrMetadata() {
        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{"id1"});
        });
    }

    @Test
    void testUpdateWithUnknownOption() {
        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{"id1", "--unknown", "value"});
        });
    }

    @Test
    void testUpdateWithNoArgs() {
        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{});
        });
    }

    @Test
    void testUpdateWithoutOpenDatabase() {
        CLIContext emptyContext = new CLIContext();

        assertThrows(Exception.class, () -> {
            command.execute(emptyContext, new String[]{"id1", "--vector", "1.0,2.0"});
        });
    }

    @Test
    void testUpdateOnlyMetadata() throws Exception {
        double[][] vectors = {{1.0, 2.0, 3.0}};
        List<Map<String, Object>> metadata = List.of(Map.of("original", "value"));
        List<String> ids = client.insert(vectors, metadata);
        String id = ids.get(0);

        command.execute(context, new String[]{id, "--metadata", "updated=new"});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
        assertTrue(output.contains("Updated metadata"));
    }
}

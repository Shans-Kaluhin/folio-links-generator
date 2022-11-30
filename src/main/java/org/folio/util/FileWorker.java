package org.folio.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.folio.FolioLinksGeneratorApp.exitWithError;

public class FileWorker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Path writeFile(String name, List<String> strings) {
        try {
            return Files.write(Paths.get(name), strings, StandardCharsets.UTF_8);
        } catch (IOException e) {
            exitWithError("Failed to write file: " + name);
            return null;
        }
    }

    public static InputStream getResourceFile(String name) {
        try {
            return ResourceUtils.getURL("classpath:" + name).openStream();
        } catch (IOException e) {
            exitWithError("Failed to read file: " + name);
            return null;
        }
    }

    public static <T> T getMappedResourceFile(String name, Class<T> clazz) {
        try {
            var file = getResourceFile(name);
            return OBJECT_MAPPER.readValue(file, clazz);
        } catch (IOException e) {
            exitWithError("Failed to map file value: " + name);
            return null;
        }
    }

    public static <T> T getMappedResourceFile(File file, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(file, clazz);
        } catch (IOException e) {
            exitWithError("Failed to map file value: " + file.getName());
            return null;
        }
    }

    public static ObjectNode getJsonObject(String name) {
        try {
            var file = getResourceFile(name);
            return (ObjectNode) OBJECT_MAPPER.readTree(file);
        } catch (IOException e) {
            exitWithError("Failed to map json file value: " + name);
            return null;
        }
    }
}

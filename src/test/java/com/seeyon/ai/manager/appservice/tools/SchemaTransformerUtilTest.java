package com.seeyon.ai.manager.appservice.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class SchemaTransformerUtilTest {
    public static String readResourcesFile(String path) throws IOException {
        ClassLoader classLoader = SchemaTransformerUtilTest.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
} 
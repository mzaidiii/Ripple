package org.personal.ripple.github;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class RepoZipReader {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("java", "xml","sql", "md", "txt");

    private static final Set<String> SKIPPED_FOLDERS =
            Set.of("target", "node_modules", "build");

    private static final int MAX_FILE_BYTES = 100_000;

    public List<RepoFile> read(byte[] zip) {
        List<RepoFile> files = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                int firstSlash = name.indexOf('/');
                if (firstSlash < 0) {
                    continue;
                }
                String path = name.substring(firstSlash + 1);

                if (!isWanted(path)) {
                    continue;
                }

                byte[] bytes = zis.readAllBytes();
                if (bytes.length > MAX_FILE_BYTES || isBinary(bytes)) {
                    continue;
                }

                files.add(new RepoFile(path, new String(bytes, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read repository zip", e);
        }

        return files;
    }

    private boolean isWanted(String path) {
        String[] parts = path.split("/");

        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].startsWith(".") || SKIPPED_FOLDERS.contains(parts[i])) {
                return false;
            }
        }

        String fileName = parts[parts.length - 1];
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase());
    }

    private boolean isBinary(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }
}
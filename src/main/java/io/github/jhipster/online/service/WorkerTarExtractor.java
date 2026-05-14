package io.github.jhipster.online.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

/**
 * Extracts a gzip-compressed tar stream into a directory (used by HTTP generator workers).
 */
public final class WorkerTarExtractor {

    private WorkerTarExtractor() {}

    public static void extractTarGz(InputStream raw, Path root) throws IOException {
        Path rootNorm = root.toAbsolutePath().normalize();
        try (
            GzipCompressorInputStream gz = new GzipCompressorInputStream(new BufferedInputStream(raw));
            TarArchiveInputStream tar = new TarArchiveInputStream(gz)
        ) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null) {
                if (!tar.canReadEntryData(entry)) {
                    continue;
                }
                String name = entry.getName().replaceFirst("^\\./", "");
                if (name.isBlank() || ".".equals(name)) {
                    continue;
                }
                Path dest = rootNorm.resolve(name).normalize();
                if (!dest.startsWith(rootNorm)) {
                    throw new IOException("Illegal tar entry path: " + name);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Path parent = dest.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        IOUtils.copy(tar, out);
                    }
                    if (entry.getLastModifiedTime() != null) {
                        try {
                            Files.setLastModifiedTime(dest, entry.getLastModifiedTime());
                        } catch (IOException ignored) {
                            // best-effort
                        }
                    }
                }
            }
        }
    }
}

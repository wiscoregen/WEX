package com.wex.assessment.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.assessment.error.AppException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

final class FileStorageSupport {

    private FileStorageSupport() {
    }

    static <T> T readOrDefault(ObjectMapper objectMapper, Path path, Class<T> type, Supplier<T> fallback) {
        if (!Files.exists(path)) {
            return fallback.get();
        }

        try {
            return objectMapper.readValue(path.toFile(), type);
        } catch (IOException ex) {
            throw AppException.internal("load persisted data from " + path, ex);
        }
    }

    static void writeAtomically(ObjectMapper objectMapper, Path path, Object payload) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path tempFile = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), payload);

            try {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw AppException.internal("persist data to " + path, ex);
        }
    }
}


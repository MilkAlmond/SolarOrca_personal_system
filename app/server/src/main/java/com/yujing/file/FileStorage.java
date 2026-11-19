package com.yujing.file;

import com.yujing.common.error.BizException;
import com.yujing.common.error.ErrorCode;
import com.yujing.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * PNG-only local storage. The file header is validated instead of trusting the
 * name or the browser-provided content type (plan 5.3, 9.4).
 */
@Component
public class FileStorage {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final Path root;

    public FileStorage(AppProperties properties) {
        this.root = Paths.get(properties.upload().dir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void createDirectories() throws IOException {
        Files.createDirectories(root.resolve("files"));
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        if (bytes.length > MAX_BYTES) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!isPng(bytes)) {
            throw new BizException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        String relativePath = "files/" + UUID.randomUUID().toString().replace("-", "") + ".png";
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return new StoredFile(relativePath, sha256(bytes), bytes.length);
    }

    /**
     * Writes an internally generated artefact (export ZIP). Unlike {@link #store},
     * the PNG check does not apply; the path is always chosen by the server.
     */
    public String write(String relativePath, byte[] bytes) {
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return sha256(bytes);
    }

    public byte[] read(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException ex) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public void deleteQuietly(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException ignored) {
            // Orphan files are handled by the periodic cleanup, never by failing the request.
        }
    }

    static boolean isPng(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private Path resolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return resolved;
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record StoredFile(String relativePath, String sha256, long sizeBytes) {
    }
}

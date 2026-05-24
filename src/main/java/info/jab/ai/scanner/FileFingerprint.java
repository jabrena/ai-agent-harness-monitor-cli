package info.jab.ai.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class FileFingerprint {

    private FileFingerprint() {
    }

    public static String hash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (Files.isDirectory(path)) {
                digest.update(path.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
                try (var stream = Files.list(path).sorted()) {
                    for (Path child : stream.toList()) {
                        digest.update(child.getFileName().toString().getBytes(StandardCharsets.UTF_8));
                        digest.update(Long.toString(size(child)).getBytes(StandardCharsets.UTF_8));
                        digest.update(Long.toString(modifiedMillis(child)).getBytes(StandardCharsets.UTF_8));
                    }
                }
            } else {
                try (InputStream inputStream = Files.newInputStream(path)) {
                    inputStream.transferTo(new DigestOutputStreamAdapter(digest));
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static long size(Path path) throws IOException {
        return Files.isRegularFile(path) ? Files.size(path) : 0L;
    }

    private static long modifiedMillis(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    private static final class DigestOutputStreamAdapter extends java.io.OutputStream {
        private final MessageDigest digest;

        private DigestOutputStreamAdapter(MessageDigest digest) {
            this.digest = digest;
        }

        @Override
        public void write(int value) {
            digest.update((byte) value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            digest.update(bytes, offset, length);
        }
    }
}

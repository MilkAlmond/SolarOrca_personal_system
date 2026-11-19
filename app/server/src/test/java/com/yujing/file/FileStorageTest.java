package com.yujing.file;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageTest {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Test
    void acceptsPngSignature() {
        assertThat(FileStorage.isPng(PNG_SIGNATURE)).isTrue();
    }

    @Test
    void rejectsJpegAndTextDisguisedAsPng() {
        assertThat(FileStorage.isPng(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46})).isFalse();
        assertThat(FileStorage.isPng("not a png".getBytes(StandardCharsets.UTF_8))).isFalse();
    }

    @Test
    void rejectsTruncatedHeader() {
        assertThat(FileStorage.isPng(new byte[]{(byte) 0x89, 0x50, 0x4E})).isFalse();
    }
}

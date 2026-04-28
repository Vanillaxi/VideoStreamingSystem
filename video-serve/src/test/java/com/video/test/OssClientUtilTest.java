package com.video.test;

import com.video.utils.OssClientUtil;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OssClientUtilTest {
    @Test
    public void validateVideoFileRejectsEmptyFile() {
        assertThrows(IllegalArgumentException.class,
                () -> OssClientUtil.validateVideoFile(new MemoryPart("empty.mp4", "video/mp4", new byte[]{})));
    }

    @Test
    public void validateVideoFileRejectsNonVideoContentType() {
        assertThrows(IllegalArgumentException.class,
                () -> OssClientUtil.validateVideoFile(new MemoryPart("text.mp4", "text/plain", new byte[]{1})));
    }

    @Test
    public void validateVideoFileAcceptsVideoContentType() {
        assertDoesNotThrow(
                () -> OssClientUtil.validateVideoFile(new MemoryPart("course.mp4", "video/mp4", new byte[]{1, 2, 3})));
    }

    @Test
    public void getVideoContentTypeUsesFileExtension() {
        assertEquals("video/mp4", OssClientUtil.getVideoContentType("course.mp4"));
        assertEquals("video/quicktime", OssClientUtil.getVideoContentType("course.mov"));
        assertEquals("video/webm", OssClientUtil.getVideoContentType("course.webm"));
    }

    @Test
    public void validateAvatarFileRejectsNonImageContentType() {
        assertThrows(IllegalArgumentException.class,
                () -> OssClientUtil.validateAvatarFile(new MemoryPart("avatar.png", "text/plain", new byte[]{1})));
    }

    @Test
    public void validateAvatarFileAcceptsImageContentType() {
        assertDoesNotThrow(
                () -> OssClientUtil.validateAvatarFile(new MemoryPart("avatar.png", "image/png", new byte[]{1, 2, 3})));
    }

    @Test
    public void getAvatarContentTypeUsesFileExtension() {
        assertEquals("image/jpeg", OssClientUtil.getAvatarContentType("avatar.jpg"));
        assertEquals("image/jpeg", OssClientUtil.getAvatarContentType("avatar.jpeg"));
        assertEquals("image/png", OssClientUtil.getAvatarContentType("avatar.png"));
        assertEquals("image/webp", OssClientUtil.getAvatarContentType("avatar.webp"));
        assertEquals("image/gif", OssClientUtil.getAvatarContentType("avatar.gif"));
    }

    private static class MemoryPart implements Part {
        private final String submittedFileName;
        private final String contentType;
        private final byte[] content;

        private MemoryPart(String submittedFileName, String contentType, byte[] content) {
            this.submittedFileName = submittedFileName;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getSubmittedFileName() {
            return submittedFileName;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public void write(String fileName) {
        }

        @Override
        public void delete() {
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaderNames() {
            return null;
        }
    }
}

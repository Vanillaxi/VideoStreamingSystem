package com.video.test;

import com.video.utils.OssClientUtil;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class OssClientUtilLiveTest {
    @Test
    public void uploadAndDeleteTinyVideoOnOss() throws Exception {
        assumeTrue(Boolean.getBoolean("oss.live.test"), "需要 -Doss.live.test=true 才执行真实 OSS 上传测试");
        assumeTrue(System.getenv("OSS_ACCESS_KEY_ID") != null && !System.getenv("OSS_ACCESS_KEY_ID").isBlank(),
                "缺少 OSS_ACCESS_KEY_ID");
        assumeTrue(System.getenv("OSS_ACCESS_KEY_SECRET") != null && !System.getenv("OSS_ACCESS_KEY_SECRET").isBlank(),
                "缺少 OSS_ACCESS_KEY_SECRET");

        OssClientUtil ossClientUtil = new OssClientUtil();
        OssClientUtil.UploadedObject uploadedObject = ossClientUtil.uploadVideo(
                new MemoryPart("codex-live-test.mp4", "video/mp4", new byte[]{0, 0, 0, 24, 102, 116, 121, 112})
        );

        try {
            assertTrue(uploadedObject.getUrl().startsWith("https://video-streaming-system.oss-cn-hangzhou.aliyuncs.com/"));
            assertTrue(uploadedObject.getObjectKey().startsWith("videos/"));
            assertTrue(uploadedObject.getObjectKey().endsWith(".mp4"));
        } finally {
            ossClientUtil.deleteObject(uploadedObject.getObjectKey());
        }
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

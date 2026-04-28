package com.video.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.servlet.http.Part;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class OssClientUtil {
    private static final String CONFIG_FILE = "properties/OSS.properties";
    private static final String ACCESS_KEY_ID_ENV = "OSS_ACCESS_KEY_ID";
    private static final String ACCESS_KEY_SECRET_ENV = "OSS_ACCESS_KEY_SECRET";
    private static final long MAX_VIDEO_SIZE = 500L * 1024 * 1024;
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".mov", ".avi", ".mkv", ".webm", ".flv", ".m4v"
    );
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    private final OssConfig config;

    public OssClientUtil() {
        this.config = loadConfig();
    }

    public UploadedObject uploadVideo(Part file) throws IOException {
        validateVideoFile(file);

        String originalFileName = cleanFileName(file.getSubmittedFileName());
        String objectKey = buildObjectKey("videos", originalFileName);
        String contentType = getVideoContentType(originalFileName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(contentType);

        try {
            putObject(file, objectKey, metadata, true);
        } catch (ClientException e) {
            uploadWithCurl(file, objectKey, contentType);
        }

        return new UploadedObject(buildUrl(objectKey), objectKey, file.getSize());
    }

    public UploadedObject uploadAvatar(Part file) throws IOException {
        validateAvatarFile(file);

        String originalFileName = cleanFileName(file.getSubmittedFileName());
        String objectKey = buildObjectKey("avatars", originalFileName);
        String contentType = getAvatarContentType(originalFileName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(contentType);

        try {
            putObject(file, objectKey, metadata, true);
        } catch (ClientException e) {
            uploadWithCurl(file, objectKey, contentType);
        }

        return new UploadedObject(buildUrl(objectKey), objectKey, file.getSize());
    }

    public void deleteObject(String objectKey) throws IOException {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            deleteObject(objectKey, true);
        } catch (ClientException e) {
            deleteWithCurl(objectKey);
        }
    }

    public static void validateVideoFile(Part file) {
        if (file == null || file.getSize() <= 0) {
            throw new IllegalArgumentException("请上传视频文件");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("视频文件不能超过 500MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("video/")) {
            throw new IllegalArgumentException("只能上传 video/* 类型的视频文件");
        }

        String originalFileName = file.getSubmittedFileName();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("视频文件名不能为空");
        }

        String extension = getExtension(originalFileName);
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 mp4、mov、avi、mkv、webm、flv、m4v 视频文件");
        }
    }

    public static void validateAvatarFile(Part file) {
        if (file == null || file.getSize() <= 0) {
            throw new IllegalArgumentException("请上传头像文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("只能上传 image/* 类型的头像文件");
        }

        String originalFileName = file.getSubmittedFileName();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("头像文件名不能为空");
        }

        String extension = getExtension(originalFileName);
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp、gif 头像文件");
        }
    }

    private void putObject(Part file, String objectKey, ObjectMetadata metadata, boolean https) throws IOException {
        OSS ossClient = createClient(https);
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(config.getBucketName(), objectKey, inputStream, metadata);
        } finally {
            ossClient.shutdown();
        }
    }

    private void deleteObject(String objectKey, boolean https) {
        OSS ossClient = createClient(https);
        try {
            ossClient.deleteObject(config.getBucketName(), objectKey);
        } finally {
            ossClient.shutdown();
        }
    }

    private OSS createClient(boolean https) {
        ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
        clientConfig.setProtocol(https ? Protocol.HTTPS : Protocol.HTTP);
        clientConfig.setMaxErrorRetry(0);
        return new OSSClientBuilder().build(stripProtocol(config.getEndpoint()),
                config.getAccessKeyId(),
                config.getAccessKeySecret(),
                clientConfig);
    }

    private void uploadWithCurl(Part file, String objectKey, String contentType) throws IOException {
        Path tempFile = Files.createTempFile("oss-upload-", getExtension(file.getSubmittedFileName()));
        Path responseFile = Files.createTempFile("oss-upload-response-", ".xml");
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            String date = gmtDate();
            String authorization = buildAuthorization("PUT", contentType, date, objectKey);
            String httpCode = runCurl(
                    "-sS",
                    "-o", responseFile.toString(),
                    "-w", "%{http_code}",
                    "-X", "PUT",
                    buildObjectUrl(objectKey),
                    "-H", "Date: " + date,
                    "-H", "Content-Type: " + contentType,
                    "-H", "Authorization: " + authorization,
                    "--data-binary", "@" + tempFile
            );
            assertHttpSuccess(httpCode, responseFile, "上传 OSS 文件失败");
        } finally {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(responseFile);
        }
    }

    private void deleteWithCurl(String objectKey) throws IOException {
        Path responseFile = Files.createTempFile("oss-delete-response-", ".xml");
        try {
            String date = gmtDate();
            String authorization = buildAuthorization("DELETE", "", date, objectKey);
            String httpCode = runCurl(
                    "-sS",
                    "-o", responseFile.toString(),
                    "-w", "%{http_code}",
                    "-X", "DELETE",
                    buildObjectUrl(objectKey),
                    "-H", "Date: " + date,
                    "-H", "Authorization: " + authorization
            );
            assertHttpSuccess(httpCode, responseFile, "删除 OSS 文件失败");
        } finally {
            Files.deleteIfExists(responseFile);
        }
    }

    private String buildObjectUrl(String objectKey) {
        return trimRightSlash(config.getDomain()) + "/" + encodeObjectKey(objectKey);
    }

    private String buildAuthorization(String method, String contentType, String date, String objectKey) {
        String stringToSign = method + "\n"
                + "\n"
                + contentType + "\n"
                + date + "\n"
                + "/" + config.getBucketName() + "/" + objectKey;
        return "OSS " + config.getAccessKeyId() + ":" + hmacSha1(stringToSign, config.getAccessKeySecret());
    }

    private static String hmacSha1(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("生成 OSS 签名失败", e);
        }
    }

    private static String runCurl(String... args) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(buildCurlCommand(args));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("curl 执行失败，exitCode=" + exitCode + "，" + output);
            }
            return output.trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("curl 执行被中断", e);
        }
    }

    private static java.util.List<String> buildCurlCommand(String... args) {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("curl");
        command.addAll(java.util.Arrays.asList(args));
        return command;
    }

    private static void assertHttpSuccess(String httpCode, Path responseFile, String message) throws IOException {
        int code = Integer.parseInt(httpCode);
        if (code >= 200 && code < 300) {
            return;
        }
        String responseBody = Files.exists(responseFile) ? Files.readString(responseFile) : "";
        throw new IOException(message + "，HTTP " + code + "，" + responseBody);
    }

    private String buildUrl(String objectKey) {
        return trimRightSlash(config.getDomain()) + "/" + objectKey;
    }

    private static OssConfig loadConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取 OSS 配置失败", e);
        }

        String endpoint = getRequired(properties, "oss.endpoint", "OSS_ENDPOINT");
        String bucketName = getRequired(properties, "oss.bucket", "OSS_BUCKET");
        String domain = getRequired(properties, "oss.domain", "OSS_DOMAIN");
        String accessKeyId = getRequiredEnv(ACCESS_KEY_ID_ENV);
        String accessKeySecret = getRequiredEnv(ACCESS_KEY_SECRET_ENV);

        return new OssConfig(endpoint, bucketName, domain, accessKeyId, accessKeySecret);
    }

    private static String getRequired(Properties properties, String propertyName, String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = properties.getProperty(propertyName);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少 OSS 配置: " + propertyName + " 或环境变量 " + envName);
        }
        return value.trim();
    }

    private static String getRequiredEnv(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量: " + envName);
        }
        return value.trim();
    }

    private static String buildObjectKey(String prefix, String originalFileName) {
        String extension = getExtension(originalFileName);
        LocalDate now = LocalDate.now();
        return prefix + "/" + now.getYear() + "/" + leftPad2(now.getMonthValue()) + "/" + leftPad2(now.getDayOfMonth())
                + "/" + UUID.randomUUID() + extension;
    }

    private static String getExtension(String fileName) {
        String cleanName = cleanFileName(fileName).toLowerCase(Locale.ROOT);
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException("文件必须带扩展名");
        }
        return cleanName.substring(dotIndex);
    }

    public static String getVideoContentType(String fileName) {
        String extension = getExtension(fileName);
        switch (extension) {
            case ".mp4":
            case ".m4v":
                return "video/mp4";
            case ".mov":
                return "video/quicktime";
            case ".webm":
                return "video/webm";
            case ".avi":
                return "video/x-msvideo";
            case ".mkv":
                return "video/x-matroska";
            case ".flv":
                return "video/x-flv";
            default:
                return "application/octet-stream";
        }
    }

    public static String getAvatarContentType(String fileName) {
        String extension = getExtension(fileName);
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".webp":
                return "image/webp";
            case ".gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    private static String cleanFileName(String fileName) {
        String normalized = fileName.replace("\\", "/");
        return normalized.substring(normalized.lastIndexOf("/") + 1);
    }

    private static String trimRightSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String stripProtocol(String value) {
        return value.replaceFirst("^https?://", "");
    }

    private static String encodeObjectKey(String objectKey) {
        String[] segments = objectKey.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                builder.append("/");
            }
            builder.append(java.net.URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return builder.toString();
    }

    private static String leftPad2(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static String gmtDate() {
        return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.now());
    }

    public static class UploadedObject {
        private final String url;
        private final String objectKey;
        private final long size;

        public UploadedObject(String url, String objectKey, long size) {
            this.url = url;
            this.objectKey = objectKey;
            this.size = size;
        }

        public String getUrl() {
            return url;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public long getSize() {
            return size;
        }
    }

    private static class OssConfig {
        private final String endpoint;
        private final String bucketName;
        private final String domain;
        private final String accessKeyId;
        private final String accessKeySecret;

        private OssConfig(String endpoint, String bucketName, String domain, String accessKeyId, String accessKeySecret) {
            this.endpoint = endpoint;
            this.bucketName = bucketName;
            this.domain = domain;
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
        }

        private String getEndpoint() {
            return endpoint;
        }

        private String getBucketName() {
            return bucketName;
        }

        private String getDomain() {
            return domain;
        }

        private String getAccessKeyId() {
            return accessKeyId;
        }

        private String getAccessKeySecret() {
            return accessKeySecret;
        }
    }
}

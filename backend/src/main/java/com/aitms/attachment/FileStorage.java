package com.aitms.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.aitms.common.ApiException;

/**
 * 로컬 디스크 저장소 — 업로드 루트(app.upload.dir) 아래에 UUID 이름으로 저장하고 DB 에는 상대 경로만 둠.
 * 확장자 허용 목록(화이트리스트)으로 종류를 제한하고 Content-Type 은 확장자로 정함(클라이언트 값 불신).
 * 이름은 서버가 만들므로 경로 조작이 불가능하고, 읽을 때도 루트 밖으로 벗어나는지 다시 검사.
 */
@Component
public class FileStorage {

    /** 확장자 → Content-Type. SVG/HTML 등 스크립트 실행 가능한 형식은 제외 */
    static final Map<String, String> ALLOWED = Map.ofEntries(
            Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"), Map.entry("bmp", "image/bmp"),
            Map.entry("pdf", "application/pdf"), Map.entry("txt", "text/plain"), Map.entry("log", "text/plain"),
            Map.entry("csv", "text/csv"), Map.entry("json", "application/json"), Map.entry("zip", "application/zip"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

    private static final Set<String> INLINE_IMAGES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp");

    private final Path root;

    public FileStorage(@Value("${app.upload.dir:./uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    public static boolean isInlineImage(String contentType) {
        return contentType != null && INLINE_IMAGES.contains(contentType);
    }

    public record Stored(String path, String fileName, String contentType, long size) {
    }

    /** @param directory 업로드 루트 아래 하위 경로 (예: executions/12) */
    public Stored store(String directory, MultipartFile file) {
        String fileName = cleanName(file.getOriginalFilename());
        String ext = extension(fileName);
        String contentType = ALLOWED.get(ext);
        if (contentType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "허용되지 않는 파일 형식입니다: " + fileName + " (이미지·PDF·로그/텍스트·CSV·JSON·ZIP·xlsx·docx만 가능)");
        }
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "빈 파일입니다: " + fileName);
        }
        String relative = directory + "/" + UUID.randomUUID() + "." + ext;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다: " + fileName, e);
        }
        return new Stored(relative, fileName, contentType, file.getSize());
    }

    public Resource load(String relativePath) {
        Path p = resolve(relativePath);
        if (!Files.isReadable(p)) {
            throw ApiException.notFound("파일을 찾을 수 없습니다. (디스크에서 삭제되었을 수 있습니다)");
        }
        return new PathResource(p);
    }

    public void deleteQuietly(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException | RuntimeException ignored) {
            // DB 삭제가 우선 — 남은 파일은 무해
        }
    }

    private Path resolve(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로입니다.");
        }
        return p;
    }

    /** 경로 구분자·제어문자 제거, 길이 제한 */
    static String cleanName(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
        if (name.isEmpty()) {
            name = "file";
        }
        if (name.length() > 200) {
            String ext = extension(name);
            name = name.substring(0, 200 - (ext.isEmpty() ? 0 : ext.length() + 1)) + (ext.isEmpty() ? "" : "." + ext);
        }
        return name;
    }

    static String extension(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i < 0 || i == fileName.length() - 1 ? "" : fileName.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}

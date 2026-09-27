package com.aitms.attachment;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Attachment {
    private Long id;
    private Long ownerId;          // 실행 항목 id 또는 결함 id
    private String fileName;
    @JsonIgnore
    private String filePath;       // 서버 내부 경로는 노출하지 않음
    private String contentType;
    private long fileSize;
    private Long uploadedBy;
    private String uploadedByName;
    private LocalDateTime uploadedAt;

    /** 썸네일로 바로 보여줄 수 있는 이미지인지 */
    @JsonProperty("image")
    public boolean isImage() {
        return FileStorage.isInlineImage(contentType);
    }
}

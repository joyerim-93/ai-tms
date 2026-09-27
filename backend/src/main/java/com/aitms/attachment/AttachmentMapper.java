package com.aitms.attachment;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 두 첨부 테이블(구조 동일)을 target.table()/ownerColumn() 상수로 공용 처리 — XML 의 ${} 는 enum 상수만 받음 */
@Mapper
public interface AttachmentMapper {

    List<Attachment> findByOwner(@Param("t") AttachmentTarget target, @Param("ownerId") Long ownerId);

    Optional<Attachment> findById(@Param("t") AttachmentTarget target, @Param("ownerId") Long ownerId, @Param("id") Long id);

    int countByOwner(@Param("t") AttachmentTarget target, @Param("ownerId") Long ownerId);

    void insert(@Param("t") AttachmentTarget target, @Param("a") Attachment attachment);

    int delete(@Param("t") AttachmentTarget target, @Param("id") Long id);
}

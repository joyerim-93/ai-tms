package com.aitms.domain.defect;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DefectMapper {

    List<Defect> search(DefectSearch search);

    long count(DefectSearch search);

    Optional<Defect> findById(Long id);

    void insert(Defect defect);

    int update(Defect defect);

    int updateStatus(@Param("id") Long id, @Param("status") DefectStatus status);

    void insertComment(DefectComment comment);

    List<DefectComment> findComments(Long defectId);
}

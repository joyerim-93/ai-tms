package com.aitms.domain.project;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectMapper mapper;

    public List<Project> findAll() {
        return mapper.findAll();
    }

    public Project get(Long id) {
        return mapper.findById(id).orElseThrow(() -> ApiException.notFound("프로젝트를 찾을 수 없습니다. id=" + id));
    }

    public List<ProjectMember> members(Long projectId) {
        return mapper.findMembers(projectId);
    }

    /** 등록자는 PM 멤버로 자동 추가 (담당자 선택 목록이 비지 않도록) */
    @Transactional
    public Project create(ProjectRequest req) {
        String code = req.code().strip().toUpperCase();
        if (mapper.countByCode(code) > 0) {
            throw ApiException.conflict("이미 사용 중인 프로젝트 코드입니다: " + code);
        }
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "종료일이 시작일보다 빠릅니다.");
        }
        Project p = new Project();
        p.setCode(code);
        p.setName(req.name().strip());
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().strip());
        p.setStatus("ACTIVE");
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        mapper.insert(p);
        mapper.insertMember(p.getId(), CurrentUser.id(), "PM");
        return get(p.getId());
    }
}

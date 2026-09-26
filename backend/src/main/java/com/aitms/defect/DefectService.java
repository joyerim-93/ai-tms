package com.aitms.defect;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.CurrentUser;
import com.aitms.common.PageResponse;
import com.aitms.defect.DefectRequests.CommentRequest;
import com.aitms.defect.DefectRequests.DefectRequest;
import com.aitms.defect.DefectRequests.StatusChangeRequest;
import com.aitms.execution.TestCycleService;
import com.aitms.execution.TestExecution;
import com.aitms.execution.TestExecutionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefectService {

    private final DefectMapper mapper;
    private final TestExecutionService executionService;
    private final TestCycleService cycleService;

    public PageResponse<Defect> search(DefectSearch search) {
        if (search.getProjectId() == null && search.getExecutionId() == null) {
            throw badRequest("projectId 또는 executionId가 필요합니다.");
        }
        return new PageResponse<>(mapper.search(search), mapper.count(search), search.getPage(), search.getSize());
    }

    public Defect get(Long id) {
        return mapper.findById(id).orElseThrow(() -> ApiException.notFound("결함을 찾을 수 없습니다. id=" + id));
    }

    public List<DefectComment> comments(Long id) {
        get(id);
        return mapper.findComments(id);
    }

    @Transactional
    public Defect create(DefectRequest req) {
        if (req.projectId() == null) {
            throw badRequest("projectId: 필수 값입니다.");
        }
        if (req.executionId() != null) {
            TestExecution exec = executionService.get(req.executionId());
            if (!cycleService.get(exec.getCycleId()).getProjectId().equals(req.projectId())) {
                throw badRequest("다른 프로젝트의 수행 항목에는 결함을 연결할 수 없습니다.");
            }
        }
        Defect defect = apply(new Defect(), req);
        defect.setProjectId(req.projectId());
        defect.setExecutionId(req.executionId());
        defect.setStatus(DefectStatus.NEW);
        defect.setReporterId(CurrentUser.id());
        mapper.insert(defect);
        return get(defect.getId());
    }

    @Transactional
    public Defect update(Long id, DefectRequest req) {
        Defect defect = apply(get(id), req);
        mapper.update(defect);
        return get(id);
    }

    /** 허용된 전이만 가능, 변경 내역은 defect_comment 에 기록 */
    @Transactional
    public Defect changeStatus(Long id, StatusChangeRequest req) {
        Defect defect = get(id);
        DefectStatus from = defect.getStatus();
        if (!from.canMoveTo(req.status())) {
            throw ApiException.conflict("'" + from + "'에서 '" + req.status() + "'(으)로 변경할 수 없습니다.");
        }
        mapper.updateStatus(id, req.status());

        DefectComment history = new DefectComment();
        history.setDefectId(id);
        history.setAuthorId(CurrentUser.id());
        history.setContent(blankToNull(req.comment()));
        history.setStatusFrom(from);
        history.setStatusTo(req.status());
        mapper.insertComment(history);
        return get(id);
    }

    @Transactional
    public DefectComment addComment(Long id, CommentRequest req) {
        get(id);
        DefectComment comment = new DefectComment();
        comment.setDefectId(id);
        comment.setAuthorId(CurrentUser.id());
        comment.setContent(req.content().strip());
        mapper.insertComment(comment);
        return comment;
    }

    private Defect apply(Defect defect, DefectRequest req) {
        defect.setTitle(req.title().strip());
        defect.setDescription(blankToNull(req.description()));
        defect.setSeverity(req.severity());
        defect.setPriority(req.priority());
        defect.setAssigneeId(req.assigneeId());
        return defect;
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}

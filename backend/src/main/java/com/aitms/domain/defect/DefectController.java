package com.aitms.domain.defect;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aitms.common.PageResponse;
import com.aitms.domain.defect.DefectRequests.CommentRequest;
import com.aitms.domain.defect.DefectRequests.DefectRequest;
import com.aitms.domain.defect.DefectRequests.StatusChangeRequest;

import lombok.RequiredArgsConstructor;

/** 결함은 삭제하지 않음 (반려 REJECTED 로 처리) */
@RestController
@RequestMapping("/api/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectService service;

    @GetMapping
    public PageResponse<Defect> search(DefectSearch search) {
        return service.search(search);
    }

    @GetMapping("/{id}")
    public Defect get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Defect create(@Validated @RequestBody DefectRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public Defect update(@PathVariable Long id, @Validated @RequestBody DefectRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/status")
    public Defect changeStatus(@PathVariable Long id, @Validated @RequestBody StatusChangeRequest req) {
        return service.changeStatus(id, req);
    }

    @GetMapping("/{id}/comments")
    public List<DefectComment> comments(@PathVariable Long id) {
        return service.comments(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public DefectComment addComment(@PathVariable Long id, @Validated @RequestBody CommentRequest req) {
        return service.addComment(id, req);
    }
}

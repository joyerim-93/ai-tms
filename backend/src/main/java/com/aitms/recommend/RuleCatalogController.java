package com.aitms.recommend;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rule-catalog")
@RequiredArgsConstructor
public class RuleCatalogController {

    private final RuleCatalogService service;

    @GetMapping
    public List<RuleCatalog> list() {
        return service.findAll();
    }
}

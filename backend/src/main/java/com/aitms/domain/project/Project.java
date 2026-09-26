package com.aitms.domain.project;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Project {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}

package com.aitms.project;

import lombok.Getter;
import lombok.Setter;

/** 담당자 선택용 */
@Getter
@Setter
public class ProjectMember {
    private Long userId;
    private String name;
    private String role;         // users.role
    private String projectRole;  // project_member.project_role
}

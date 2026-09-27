package com.aitms.attachment;

/** 첨부파일이 붙는 대상 — table/ownerColumn 은 상수이며 MyBatis 에서 ${} 로 쓰이므로 사용자 입력이 들어오면 안 됨 */
public enum AttachmentTarget {
    EXECUTION("test_execution_attachment", "execution_id", "executions"),
    DEFECT("defect_attachment", "defect_id", "defects");

    private final String table;
    private final String ownerColumn;
    private final String directory;

    AttachmentTarget(String table, String ownerColumn, String directory) {
        this.table = table;
        this.ownerColumn = ownerColumn;
        this.directory = directory;
    }

    public String table() {
        return table;
    }

    public String ownerColumn() {
        return ownerColumn;
    }

    /** 업로드 루트 아래 하위 폴더 */
    public String directory() {
        return directory;
    }
}

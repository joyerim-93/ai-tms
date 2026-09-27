package com.aitms.domain.recommend;

public enum JobStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED;

    public boolean isActive() {
        return this == PENDING || this == RUNNING;
    }
}

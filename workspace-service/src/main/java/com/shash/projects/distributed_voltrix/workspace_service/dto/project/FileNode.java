package com.shash.projects.distributed_voltrix.workspace_service.dto.project;

public record FileNode(
        String path
) {
    @Override
    public String toString() {
        return path;
    }
}

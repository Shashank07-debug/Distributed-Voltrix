package com.shash.projects.distributed_voltrix.common_lib.dto;

public record FileNode(
        String path
) {

    @Override
    public String toString() {
        return path;
    }
}

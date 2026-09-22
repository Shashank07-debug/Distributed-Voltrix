package com.shash.projects.distributed_voltrix.workspace_service.service;

import com.shash.projects.distributed_voltrix.workspace_service.dto.project.DeployResponse;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Null;

public interface DeploymentService {

    @Nullable
    DeployResponse deploy(Long projectId);
}

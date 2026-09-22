package com.shash.projects.distributed_voltrix.workspace_service.service;

import com.shash.projects.distributed_voltrix.workspace_service.dto.project.DeployResponse;

public interface DeploymentService {

    DeployResponse deploy(Long projectId);
}

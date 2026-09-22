package com.shash.projects.distributed_voltrix.workspace_service.service;

import com.shash.projects.distributed_voltrix.common_lib.dto.FileTreeDto;
import com.shash.projects.distributed_voltrix.workspace_service.dto.project.FileContentResponse;

public interface ProjectFileService {
    FileTreeDto getFileTree(Long projectId);

    String getFileContent(Long projectId, String path);

    void saveFile(Long projectId, String filePath, String fileContent);
}

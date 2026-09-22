package com.shash.projects.distributed_voltrix.workspace_service.controller;

import com.shash.projects.distributed_voltrix.common_lib.dto.FileTreeDto;
import com.shash.projects.distributed_voltrix.workspace_service.service.ProjectFileService;
import com.shash.projects.distributed_voltrix.workspace_service.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/internal/v1/")
@RestController
public class InternalWorkspaceController {

    private final ProjectService projectService;
    private final ProjectFileService projectFileService;

    @GetMapping("/projects/{projectId}/files/tree")
    public FileTreeDto getFileTree(@PathVariable Long prjectId){
        return projectFileService.getFileTree(prjectId);
    }

    @GetMapping("/projects/{projectId}/files/content")
    public String getFileTree(@PathVariable Long projectId, @RequestParam String path){
        return projectFileService.getFileContent(projectId, path);
    }



}

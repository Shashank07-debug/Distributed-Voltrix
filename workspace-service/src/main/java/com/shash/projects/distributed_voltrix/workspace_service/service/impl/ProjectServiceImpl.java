package com.shash.projects.distributed_voltrix.workspace_service.service.impl;

import com.shash.projects.distributed_voltrix.common_lib.dto.PlanDto;
import com.shash.projects.distributed_voltrix.common_lib.enums.ProjectRole;
import com.shash.projects.distributed_voltrix.common_lib.error.BadRequestException;
import com.shash.projects.distributed_voltrix.common_lib.error.ResourceNotFoundException;
import com.shash.projects.distributed_voltrix.common_lib.security.AuthUtil;
import com.shash.projects.distributed_voltrix.workspace_service.client.AccountClient;
import com.shash.projects.distributed_voltrix.workspace_service.dto.project.ProjectRequest;
import com.shash.projects.distributed_voltrix.workspace_service.dto.project.ProjectResponse;
import com.shash.projects.distributed_voltrix.workspace_service.dto.project.ProjectSummaryResponse;
import com.shash.projects.distributed_voltrix.workspace_service.entity.Project;
import com.shash.projects.distributed_voltrix.workspace_service.entity.ProjectMember;
import com.shash.projects.distributed_voltrix.workspace_service.entity.ProjectMemberId;
import com.shash.projects.distributed_voltrix.workspace_service.mapper.ProjectMapper;
import com.shash.projects.distributed_voltrix.workspace_service.repository.ProjectMemberRepository;
import com.shash.projects.distributed_voltrix.workspace_service.repository.ProjectRepository;
import com.shash.projects.distributed_voltrix.workspace_service.service.ProjectService;
import com.shash.projects.distributed_voltrix.workspace_service.service.ProjectTemplateService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    ProjectMapper projectMapper;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;


    @Override
    public ProjectResponse createProject(ProjectRequest request) {

        if(!canCreateNewProject()){
            throw new BadRequestException("User cannot create a new project with current plan, Upgrade plan now.");
        }


        Long ownerUserId = authUtil.getCurrentUserId();

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();
        project = projectRepository.save(project);
        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), ownerUserId);
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();
        projectMemberRepository.save(projectMember);
        projectTemplateService.initializeProjectFormTemplate(project.getId());

        return projectMapper.toProjectResponse(project);
    }
    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId = authUtil.getCurrentUserId();
        var projectsWithRoles = projectRepository.findAllAccessibleByUser(userId);
        return projectsWithRoles.stream()
                .map(p -> projectMapper.toProjectSummaryResponse(p.getProject(), p.getRole()))
                .toList();
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        ProjectRepository.ProjectWithRole projectWithRole = projectRepository.findAccessibleProjectByIdWithRole(projectId, userId)
                .orElseThrow(() ->  new BadRequestException("Project Not Found"));

        return projectMapper.toProjectSummaryResponse(projectWithRole.getProject(), projectWithRole.getRole());
    }

    @Override
    @PreAuthorize("@security.canEditProjec(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);

        project.setName(request.name());
        project = projectRepository.save(project);

        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@@security.canDeleteProjec(#projectId)")
    public void softDelete(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);

        project.setDeletedAt(Instant.now());
        projectRepository.save(project);

    }

    public Project getAccessibleProjectById(Long projectId, Long userId){
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }

    private boolean canCreateNewProject() {
        Long userId = authUtil.getCurrentUserId();
        if(userId == null){
            return false;
        }
        PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();

        int maxAllowed = plan.maxProjects();
        int ownedCount = projectMemberRepository.countProjectOwnedByUser(userId);

        return ownedCount < maxAllowed;
    }


}

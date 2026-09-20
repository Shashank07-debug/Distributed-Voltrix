package com.shash.projects.distributed_voltrix.workspace_service.service.impl;

import com.shash.projects.distributed_voltrix.common_lib.dto.UserDto;
import com.shash.projects.distributed_voltrix.common_lib.error.ResourceNotFoundException;
import com.shash.projects.distributed_voltrix.common_lib.security.AuthUtil;
import com.shash.projects.distributed_voltrix.workspace_service.client.AccountClient;
import com.shash.projects.distributed_voltrix.workspace_service.dto.member.InviteMemberRequest;
import com.shash.projects.distributed_voltrix.workspace_service.dto.member.MemberResponse;
import com.shash.projects.distributed_voltrix.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.shash.projects.distributed_voltrix.workspace_service.entity.Project;
import com.shash.projects.distributed_voltrix.workspace_service.entity.ProjectMember;
import com.shash.projects.distributed_voltrix.workspace_service.entity.ProjectMemberId;
import com.shash.projects.distributed_voltrix.workspace_service.mapper.ProjectMemberMapper;
import com.shash.projects.distributed_voltrix.workspace_service.repository.ProjectMemberRepository;
import com.shash.projects.distributed_voltrix.workspace_service.repository.ProjectRepository;

import com.shash.projects.distributed_voltrix.workspace_service.service.ProjectMemberService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    AuthUtil authUtil;
    AccountClient accountClient;


    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
        return projectMemberRepository.findByIdProjectId(projectId)
                .stream()
                .map(projectMemberMapper::toProjectMemberResponseFromMember)
                .toList();
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);

        UserDto invitee = accountClient.getUserByEmail(request.username()).orElseThrow(
                ()-> new ResourceNotFoundException("User", request.username())
        );
        if(invitee.id().equals(userId)){
            throw new RuntimeException("Cannot Invite Yourself");
        }
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.id());
        if(projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Cannot Invite Once Again");
        }
        ProjectMember member = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
//                .user(invitee)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();
        projectMemberRepository.save(member);

        return projectMemberMapper.toProjectMemberResponseFromMember(member);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();

        projectMember.setProjectRole(request.role());

        projectMemberRepository.save(projectMember);

        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);

    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        if(!projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Member not found in Project");
        }

        projectMemberRepository.deleteById(projectMemberId);
    }

    public Project getAccessibleProjectById(Long projectId, Long userId){
        return projectRepository.findAccessibleProjectById(projectId, userId).orElseThrow();
    }
}

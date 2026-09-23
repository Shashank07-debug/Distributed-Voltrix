package com.shash.projects.distributed_voltrix.intelligence_service.service;


import com.shash.projects.distributed_voltrix.intelligence_service.dto.chat.ChatResponse;

import java.util.List;

public interface ChatService {

    List<ChatResponse> getProjectChatHistory(Long projectId);
}

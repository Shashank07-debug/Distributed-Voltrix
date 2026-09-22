package com.shash.projects.distributed_voltrix.intelligence_service.service;

import com.shash.projects.distributed_voltrix.intelligence_service.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;

public interface AiGenerationService {
    Flux<StreamResponse> streamResponse(String message, Long projectId);
}

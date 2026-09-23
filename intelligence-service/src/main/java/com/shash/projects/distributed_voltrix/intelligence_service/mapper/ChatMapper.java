package com.shash.projects.distributed_voltrix.intelligence_service.mapper;

import com.shash.projects.distributed_voltrix.intelligence_service.dto.chat.ChatResponse;
import com.shash.projects.distributed_voltrix.intelligence_service.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    List<ChatResponse> fromListOfChatMessage(List<ChatMessage> chatMessageList);
}

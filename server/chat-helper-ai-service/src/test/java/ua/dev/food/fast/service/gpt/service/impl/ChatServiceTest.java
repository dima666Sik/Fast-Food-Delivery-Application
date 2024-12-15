package ua.dev.food.fast.service.gpt.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ua.dev.food.fast.service.gpt.domain.MessageHistory;
import ua.dev.food.fast.service.gpt.domain.dto.response.ChatResponse;
import ua.dev.food.fast.service.gpt.domain.dto.response.Choice;
import ua.dev.food.fast.service.gpt.domain.dto.response.Message;
import ua.dev.food.fast.service.gpt.domain.dto.response.OpenAiResponse;
import ua.dev.food.fast.service.gpt.repository.MessageHistoryRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private MessageHistoryRepository historyRepository;

    @Test
    void getChatHistoryShouldReturnHistoryWhenHistoryExists() {
        Long userId = 1L;
        MessageHistory history = new MessageHistory(userId);
        when(historyRepository.getHistory(userId)).thenReturn(Mono.just(history));

        StepVerifier.create(chatService.getChatHistory(userId))
            .expectNext(history)
            .verifyComplete();

        verify(historyRepository, times(1)).getHistory(userId);
    }

    @Test
    void getChatHistoryShouldReturnEmptyHistoryWhenNoHistoryExists() {
        Long userId = 1L;
        when(historyRepository.getHistory(userId)).thenReturn(Mono.empty());

        StepVerifier.create(chatService.getChatHistory(userId))
            .expectNextCount(0)
            .verifyComplete();

        verify(historyRepository, times(1)).getHistory(userId);
    }

    @Test
    void processMessageShouldSaveNewHistoryWhenNoExistingHistory() {
        Long userId = 1L;
        String userMessage = "Hello!";
        MessageHistory newHistory = new MessageHistory(userId);
        when(historyRepository.getHistory(userId)).thenReturn(Mono.empty());

        when(historyRepository.saveHistory(any(MessageHistory.class))).thenReturn(Mono.empty());

        when(openAiClient.generateResponse(any(MessageHistory.class)))
            .thenReturn(Mono.just(new OpenAiResponse(null, null, null, null, List.of(new Choice(0, Message.builder()
                .content("Hi there!").build(), null)))));

        StepVerifier.create(chatService.processMessage(userId, userMessage))
            .assertNext(response -> {
                assert response.message().equals("Hi there!");
            })
            .verifyComplete();

        verify(historyRepository, times(1)).getHistory(userId);
        verify(historyRepository, times(1)).saveHistory(any(MessageHistory.class));
        verify(openAiClient, times(1)).generateResponse(any(MessageHistory.class));
    }

    @Test
    void processMessageShouldHandleExistingHistory() {
        Long userId = 1L;
        String userMessage = "What's on the menu?";
        MessageHistory existingHistory = new MessageHistory(userId);
        existingHistory.addMessage(new Message("system", ChatService.SYSTEM_THEMATIC_ASSISTANT));
        when(historyRepository.getHistory(userId)).thenReturn(Mono.just(existingHistory));
        when(openAiClient.generateResponse(any(MessageHistory.class)))
            .thenReturn(Mono.just(new OpenAiResponse(null, null, null, null, List.of(new Choice(0, Message.builder()
                .content("We have sushi, burgers, and pizza!").build(), null)))));

        when(historyRepository.saveHistory(any(MessageHistory.class))).thenReturn(Mono.empty());

        StepVerifier.create(chatService.processMessage(userId, userMessage))
            .assertNext(response -> {
                assert response.message().equals("We have sushi, burgers, and pizza!");
            })
            .verifyComplete();

        verify(historyRepository, times(1)).getHistory(userId);
        verify(openAiClient, times(1)).generateResponse(any(MessageHistory.class));
        verify(historyRepository, times(1)).saveHistory(any(MessageHistory.class));
    }

    @Test
    void processMessageShouldTruncateHistoryWhenHistoryExceedsMaxLength() {
        Long userId = 1L;
        String userMessage = "What else?";
        MessageHistory longHistory = new MessageHistory(userId);
        for (int i = 0; i < 6; i++) {
            longHistory.addMessage(new Message("user", "Message " + i));
        }
        when(historyRepository.getHistory(userId)).thenReturn(Mono.just(longHistory));
        when(openAiClient.generateResponse(any(MessageHistory.class)))
            .thenReturn(Mono.just(new OpenAiResponse(null, null, null, null, List.of(new Choice(0, Message.builder()
                .content("That's all for now.").build(), null)))));
        when(historyRepository.saveHistory(any(MessageHistory.class))).thenReturn(Mono.empty());


        StepVerifier.create(chatService.processMessage(userId, userMessage))
            .assertNext(response -> {
                assert response.message().equals("That's all for now.");
            })
            .verifyComplete();

        verify(historyRepository, times(1)).getHistory(userId);
        verify(openAiClient, times(1)).generateResponse(any(MessageHistory.class));
        verify(historyRepository, times(1)).saveHistory(any(MessageHistory.class));
    }
}

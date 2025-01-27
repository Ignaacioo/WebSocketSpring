package me.SeniorRest.SchoolProject.WebSocket.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import me.SeniorRest.SchoolProject.WebSocket.Configs.AppConfig;
import me.SeniorRest.SchoolProject.WebSocket.Services.VseGPTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
    @Value("${images.path}")
    protected String IMAGES_PATH;
    @Value("${history.path}")
    protected String CHAT_HISTORY_PATH;
    @Value("${history.max-size}")
    private int MAX_CHAT_HISTORY_SIZE;

    private final BalanceController balanceController;
    private final HistoryController historyController;
    private final VseGPTService vseGptService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    private void initialize() {
        createDirectory(IMAGES_PATH);
        createDirectory(CHAT_HISTORY_PATH);
    }

    private void createDirectory(String path) {
        Path directoryPath = Path.of(path);
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            System.err.println("Ошибка при создании директории: " + directoryPath);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String parseReplace(String text) {
        return text.replace("\\(", "$")
                .replace("\\)", "$")
                .replace("\\[", "$$")
                .replace("\\]", "$$");
    }

    @MessageMapping("/message/{key}/{room}")
    @SendTo("/topic/rooms/{key}/{room}")
    public Map<String, Object> processMessageFromClient(
            @DestinationVariable String key,
            @DestinationVariable String room,
            Map<String, Object> message
    ) throws JsonProcessingException {
        key = key.trim();
        room = room.trim();

        // Проверка баланса
        balanceController.initializeBalance(key);
        if (balanceController.getBalance(key) < 10) {
            return Map.of("error", "Сумма на балансе должна быть не меньше 10 рублей");
        }

        System.out.println("Получен запрос из комнаты: " + room + "! Ключ: " + key);

        // Обработка "приветствия" или пустого сообщения
        if (message == null || message.isEmpty() || message.get("content") == null) {
            System.out.println("Подписка на комнату!");
            List<Map<String, Object>> chatHistory = historyController.getHistory(key, room);
            return Map.of("type", "chat_history", "content", chatHistory);
        }


        List<Map<String, Object>> userMessageProc = new ArrayList<>();
        // Обработка текста
        String text = (String) message.get("content");
        text = parseReplace(text);
        userMessageProc.add(Map.of(
                "type", "text",
                "text", Objects.requireNonNullElse(text, "Ты дружелюбный учитель")
        ));

        // Обработка изображений
        List<String> base64Images = (List<String>) message.get("images");
        parseImages(base64Images, userMessageProc);


        // Формирование сообщения пользователя и отправка клиенту
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", userMessageProc
        );
        messagingTemplate.convertAndSend("/topic/rooms/" + key + "/" + room, userMessage);

        // Формирование истории чата
        List<Map<String, Object>> chatHistory = historyController.getMessages(key, room, MAX_CHAT_HISTORY_SIZE);
        chatHistory.add(userMessage);

        // Отправление запроса на VseGPT
        String gptResponse = vseGptService.processMessage(key, chatHistory);
        gptResponse = parseReplace(gptResponse);

        List<Map<String, Object>> answerContent = new ArrayList<>();
        answerContent.add(Map.of(
                "type", "text",
                "text", gptResponse
        ));
        Map<String, Object> assistantMessage = Map.of(
                "role", "assistant",
                "content", answerContent
        );

        // Обновление истории с учётом ответа GPT
        historyController.logMessage(key, room, userMessage);
        chatHistory.add(assistantMessage);
        historyController.logMessage(key, room, assistantMessage);

        // Отправление ответа VseGPT клиенту
        return assistantMessage;
    }

    private void parseImages(List<String> base64Images, List<Map<String, Object>> processedContent) {
        if (base64Images != null && !base64Images.isEmpty()) {
            for (String base64Image : base64Images) {
                String imageUrl = historyController.saveImage(base64Image);
                if (imageUrl != null) {
                    processedContent.add(Map.of(
                            "type", "image_url",
                            "image_url", Map.of("url", imageUrl)
                    ));
                }}
        }
    }

}

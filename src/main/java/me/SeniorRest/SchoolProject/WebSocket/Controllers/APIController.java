package me.SeniorRest.SchoolProject.WebSocket.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class APIController {

    @Value("${history.path}")
    protected String CHAT_HISTORY_PATH;
    private static final Logger logger = LoggerFactory.getLogger(APIController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Получить историю чата
    @GetMapping("/getHistory/{key}/{room}")
    public ResponseEntity<String> getChatHistory(@PathVariable String key, @PathVariable String room) {
        key = key.replace(" ", "");
        room = room.trim();
        Path filePath = Paths.get(CHAT_HISTORY_PATH, key, room + ".json");

        try {
            if (!Files.exists(filePath)) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(generateError("error", "Файл с историей чата не найден"));
            }

            String jsonContent = Files.readString(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(jsonContent);
        } catch (IOException e) {
            logger.error("Ошибка при чтении файла: {}", e.getMessage());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(generateError("error", "Ошибка чтения файла"));
        }
    }

    // Удалить историю чата
    @GetMapping("/deleteChat/{key}/{room}")
    public ResponseEntity<String> deleteChatHistory(@PathVariable String key, @PathVariable String room) {
        key = key.replace(" ", "");
        room = room.trim();
        Path filePath = Paths.get(CHAT_HISTORY_PATH, key, room + ".json");

        try {
            if (!Files.exists(filePath)) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(generateError("error", "Файл с историей чата не найден"));
            }

            Files.delete(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(generateError("message", "История чата удалена"));
        } catch (IOException e) {
            logger.error("Ошибка при удалении файла: {}", e.getMessage());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(generateError("error", "Ошибка при удалении файла"));
        }
    }

    // Основной эндпоинт
    @GetMapping("/keyInfo/{key}")
    public ResponseEntity<String> keyInfo(@PathVariable String key) {
        key = key.replace(" ", "");
        Path directoryPath = Paths.get(CHAT_HISTORY_PATH, key);

        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(generateError("error", "Ключ не найден"));
        }

        try {
            String balance = getBalance(directoryPath);
            List<String> chats = getChats(directoryPath);

            Map<String, Object> response = new HashMap<>();
            response.put("balance", balance);
            response.put("chats", chats);
            String jsonResponse = objectMapper.writeValueAsString(response);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(jsonResponse);
        } catch (IOException e) {
            logger.error("Ошибка обработки данных: {}", e.getMessage());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(generateError("error", "Ошибка обработки данных"));
        }
    }

    // Получение баланса
    private String getBalance(Path directoryPath) throws IOException {
        Path balanceFilePath = directoryPath.resolve("balance.json");
        if (Files.exists(balanceFilePath)) {
            String json = Files.readString(balanceFilePath);
            return objectMapper.readTree(json).get("balance").asText();
        }
        return "Файл с балансом не найден";
    }

    // Получение списка чатов
    private List<String> getChats(Path directoryPath) throws IOException {
        return Files.list(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> !path.getFileName().toString().equals("balance.json"))
                .map(path -> path.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toList());
    }

    private String generateError(String type, String message) {
        try {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put(type, message);

            return objectMapper.writeValueAsString(errorResponse);

        } catch (JsonProcessingException e) {
            logger.error("Ошибка формирования JSON с ошибкой: {}", e.getMessage());
            return "{\"error\": \"Неизвестная ошибка\"}";
        }
    }
}
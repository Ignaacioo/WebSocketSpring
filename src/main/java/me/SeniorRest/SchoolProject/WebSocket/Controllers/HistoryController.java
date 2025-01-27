package me.SeniorRest.SchoolProject.WebSocket.Controllers;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HistoryController {
    @Value("${images.path}")
    protected String IMAGES_PATH;
    @Value("${images.base-url}")
    protected String BASE_URL;
    @Value("${history.path}")
    protected String CHAT_HISTORY_PATH;
    private final ObjectMapper objectMapper;

    protected void logMessage(String key, String room, Map<String, Object> message) {
        // Работа с директорией
        File directory = new File(CHAT_HISTORY_PATH + "/" + key);
        if (!directory.exists() && !directory.mkdirs()) {
            System.err.println("Не удалось создать директорию: " + directory.getAbsolutePath());
            return;
        }

        File file = new File(directory, room + ".json");
        List<Map<String, Object>> chatHistory = new ArrayList<>();

        if (file.exists()) {
            try {
                chatHistory = objectMapper.readValue(file, new TypeReference<>() {});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Добавление нового сообщения в историю
        chatHistory.add(message);
        try {
            objectMapper.writeValue(file, chatHistory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected List<Map<String, Object>> getMessages(String key, String room, int limit) {
        File file = new File(CHAT_HISTORY_PATH + "/" + key + "/" + room + ".json");
        if (!file.exists()) {
            System.out.println("Файл истории чата не найден: " + file.getAbsolutePath());
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> fullHistory = objectMapper.readValue(file, new TypeReference<>() {});
            int startIndex = Math.max(fullHistory.size() - limit, 0);
            return fullHistory.subList(startIndex, fullHistory.size());
        } catch (IOException e) {
            System.err.println("Ошибка чтения истории чата: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    List<Map<String, Object>> getHistory(String key, String room) {
        Path filePath = Path.of(CHAT_HISTORY_PATH, key, room + ".json");
        if (!Files.exists(filePath)) {
            System.out.println("Файл истории чата не найден: " + filePath);
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(Files.readString(filePath), new TypeReference<>() {});
        } catch (IOException e) {
            System.err.println("Ошибка чтения истории чата: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    protected String saveImage(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image.split(",")[1]);
            String fileName = System.currentTimeMillis() + ".png";
            File file = new File(IMAGES_PATH, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(imageBytes);
            }
            return BASE_URL + fileName;
        } catch (IOException e) {
            return null;
        }
    }

}

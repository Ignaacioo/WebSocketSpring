package me.SeniorRest.SchoolProject.WebSocket.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.SeniorRest.SchoolProject.WebSocket.Controllers.BalanceController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VseGPTService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BalanceController balance;
    private final String API_URL = "https://api.vsegpt.ru/v1/chat/completions";
    @Value("${api.api-key}")
    private String API_KEY;
    public String processMessage(String key, List<Map<String, Object>> chatHistory) {
        try {
            // Формирование тела запроса
            Map<String, Object> requestBody = Map.of(
                    "model", "openai/gpt-4o-mini",
                    "providers", List.of("openai"),
                    "messages", chatHistory
            );

            // Сериализация в JSON
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // Создание HTTP-клиента и отправка запроса
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Проверка ответа
            if (response.statusCode() == 200) {
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);

                // Извлечение токенов
                Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                int promptTokens = (int) usage.getOrDefault("prompt_tokens", 0);
                int completionTokens = (int) usage.getOrDefault("completion_tokens", 0);

                // Обновление баланса
                balance.updateBalance(key, promptTokens, completionTokens);

                // Извлекаем текст ответа
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choiceMessage = (Map<String, Object>) choices.get(0).get("message");
                    return (String) choiceMessage.get("content");
                } else {
                    throw new IOException("Некорректный ответ API: отсутствуют 'choices'.");
                }

            } else {
                System.err.println("Ошибка API: " + response.statusCode() + " - " + response.body());
                return "Произошла ошибка при запросе к API.";
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Произошла ошибка при запросе к API.";
        }
    }
}

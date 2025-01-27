package me.SeniorRest.SchoolProject.WebSocket.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class BalanceController {

    @Value("${history.path}")
    protected String BALANCE_FOLDER;
    private static final double PROMPT_TOKEN_COST = 0.00002;
    private static final double COMPLETION_TOKEN_COST = 0.00008;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Инициализация файла баланса
    public void initializeBalance(String key) {
        Path balanceFilePath = Path.of(BALANCE_FOLDER, key, "balance.json");
        File balanceFile = balanceFilePath.toFile();

        if (!balanceFile.exists()) {
            try {
                Files.createDirectories(balanceFilePath.getParent());
                Files.writeString(balanceFilePath, "{\"balance\": 0}");
            } catch (IOException e) {
                System.err.println("Ошибка при создании файла баланса: " + e.getMessage());
            }
        }
    }

    // Получение текущего баланса
    public double getBalance(String key) {
        Path balanceFilePath = Path.of(BALANCE_FOLDER, key, "balance.json");
        try {
            String json = Files.readString(balanceFilePath);
            return objectMapper.readTree(json).get("balance").asDouble();
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла баланса: " + e.getMessage());
            return 0;
        }
    }

    // Обновление баланса
    public void updateBalance(String key, int promptTokens, int completionTokens) {
        double cost = (promptTokens * PROMPT_TOKEN_COST) + (completionTokens * COMPLETION_TOKEN_COST);
        double currentBalance = getBalance(key);
        double newBal = currentBalance - cost;

        Path balanceFilePath = Path.of(BALANCE_FOLDER, key, "balance.json");
        try {
            Files.writeString(balanceFilePath, "{\"balance\": " + newBal + "}");
        } catch (IOException e) {
            System.err.println("Ошибка обновления файла баланса: " + e.getMessage());
        }
    }
}

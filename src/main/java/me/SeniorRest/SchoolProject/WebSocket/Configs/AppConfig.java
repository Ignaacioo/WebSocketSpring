package me.SeniorRest.SchoolProject.WebSocket.Configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Value("${history.max-size:6}")
    private int maxChatHistorySize;

    @Value("${images.path:images}")
    private String uploadPath;

    @Value("${history.path:histories}")
    private String chatHistoryPath;

    @Value("${image.base-url:http://localhost:8080/images/}")
    private String imageBaseUrl;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
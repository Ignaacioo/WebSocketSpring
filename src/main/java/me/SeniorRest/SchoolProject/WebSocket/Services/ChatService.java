package me.SeniorRest.SchoolProject.WebSocket.Services;

import org.springframework.stereotype.Service;

//Ответ на вопрос, отправка на чатгпт и возвращение ответа
@Service
public class ChatService {
    public String answerMessage(String message) {
        return message;
    }
}

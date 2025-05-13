package juego.arsw.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import juego.arsw.model.User;
import juego.arsw.service.UserService;

@Controller
public class UserWebSocketController {

    @Autowired
    private UserService userService;

    @MessageMapping("/game")
    @SendTo("/topic/game")
    public User processPlayer(User playerData) {
        return userService.processUser(playerData);
    }
}
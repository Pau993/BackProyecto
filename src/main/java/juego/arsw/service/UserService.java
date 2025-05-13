package juego.arsw.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import juego.arsw.model.User;

@Service
public class UserService {
    private final List<User> users = new ArrayList<>();

    public User createUser(User user) {
        users.add(user);
        return user;
    }

    public Optional<User> getUserById(String id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public User updateUser(String id, User updatedUser) {
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.getId().equals(id)) {
                updatedUser.setId(id);
                users.set(i, updatedUser);
                return updatedUser;
            }
        }
        return null;
    }

    public boolean deleteUser(String id) {
        return users.removeIf(user -> user.getId().equals(id));
    }

    public User processUser(User user) {
        Optional<User> existingUser = getUserById(user.getId());
        
        if (existingUser.isPresent()) {
            return updateUser(user.getId(), user);
        } else {
            return createUser(user);
        }
    }

    public void updateUserPosition(String id, double x, double y, String direction) {
        getUserById(id).ifPresent(user -> {
            user.setX(x);
            user.setY(y);
            user.setDirection(direction);
        });
    }

    public void updateUserName(String id, String name) {
        getUserById(id).ifPresent(user -> user.setName(name));
    }
}
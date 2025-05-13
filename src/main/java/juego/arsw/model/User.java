package juego.arsw.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User{
    private String id;
    private String name;
    private double x;
    private double y;
    private String direction;
    private String hasPerson;

    public User(String id) {
        this.id = id;
        this.x = 0.0;
        this.y = 0.0;
        this.direction = "right";
        this.name = "Player-" + id;
    }

    public User(String id, double x, double y, String direction, boolean hasPerson) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.name = "Player-" + id;
    }

    public String getHasPerson() {
        return hasPerson;
    }
    
    public void setHasPerson(String hasPerson) {
        this.hasPerson = hasPerson;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getDirection() {
        return this.direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

}
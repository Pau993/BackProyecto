package juego.arsw.model;

public class EntityPerson {

    private String id;
    private int x;
    private int y;
    private String spriteFile;

    public EntityPerson() {
    }

    public EntityPerson(String id, int x, int y, String spriteFile) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.spriteFile = spriteFile;
    }

    public String getSpriteFile() {
        return this.spriteFile;
    }

    public void setSpriteFile(String spriteFile) {
        this.spriteFile = spriteFile;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
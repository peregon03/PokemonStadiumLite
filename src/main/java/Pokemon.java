import javax.swing.ImageIcon;
import java.util.List;

public class Pokemon
{
    private String nombre;
    private List<String> tipos;
    private int hp;
    private int ataque;
    private int defensa;
    private int velocidad;
    private int hpActual;
    private String spriteUrl;
    private ImageIcon sprite;

    public Pokemon(String nombre, List<String> tipos, int hp, int ataque, int defensa, int velocidad, String spriteUrl)
    {
        this.nombre = nombre;
        this.tipos = tipos;
        this.hp = hp;
        this.ataque = ataque;
        this.defensa = defensa;
        this.velocidad = velocidad;
        this.hpActual = hp;
        this.spriteUrl = spriteUrl;
        this.sprite = null;
    }

    public String getNombre()
    {
        return nombre;
    }

    public List<String> getTipos()
    {
        return tipos;
    }

    public int getHp()
    {
        return hp;
    }

    public int getAtaque()
    {
        return ataque;
    }

    public int getDefensa()
    {
        return defensa;
    }

    public int getVelocidad()
    {
        return velocidad;
    }

    public int getHpActual()
    {
        return hpActual;
    }

    public String getSpriteUrl()
    {
        return spriteUrl;
    }

    public ImageIcon getSprite()
    {
        return sprite;
    }

    //El HP no puede ser negativo
    public void setHpActual(int hpActual)
    {
        this.hpActual = Math.max(0, hpActual);
    }

    public void setSprite(ImageIcon sprite)
    {
        this.sprite = sprite;
    }

    //Devuelve los tipos como texto separado por coma
    public String getTiposTexto()
    {
        return String.join(", ", tipos);
    }
}

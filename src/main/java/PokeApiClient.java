import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.URL;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import org.json.JSONObject;
import org.json.JSONArray;

public class PokeApiClient
{
    //URL base de la PokeAPI
    private static final String BASE_URL = "https://pokeapi.co/api/v2/pokemon/";

    //Obtiene la lista completa de nombres de todos los pokemon disponibles en la API
    //Se usa para el autocompletado del campo de busqueda
    public static List<String> obtenerTodosLosNombres() throws Exception
    {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "?limit=10000&offset=0"))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new Exception("No se pudo obtener la lista de nombres");

        JSONObject json    = new JSONObject(response.body());
        JSONArray  results = json.getJSONArray("results");

        List<String> nombres = new ArrayList<>();
        for (int i = 0; i < results.length(); i++)
            nombres.add(results.getJSONObject(i).getString("name"));

        return nombres;
    }

    //Consulta y parsea un pokemon por nombre o id numerico
    public static Pokemon consultarPokemon(String nombreOId) throws Exception
    {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + nombreOId.toLowerCase().trim()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new Exception("Pokemon no encontrado: " + nombreOId);

        JSONObject json = new JSONObject(response.body());

        //Extraer nombre
        String nombre = json.getString("name");

        //Extraer tipos del pokemon
        List<String> tipos = new ArrayList<>();
        JSONArray tiposArray = json.getJSONArray("types");
        for (int i = 0; i < tiposArray.length(); i++)
        {
            JSONObject tipoObj = tiposArray.getJSONObject(i);
            tipos.add(tipoObj.getJSONObject("type").getString("name"));
        }

        //Extraer stats base: hp, attack, defense, speed
        int hp = 0, ataque = 0, defensa = 0, velocidad = 0;
        JSONArray statsArray = json.getJSONArray("stats");
        for (int i = 0; i < statsArray.length(); i++)
        {
            JSONObject statObj = statsArray.getJSONObject(i);
            String nombreStat = statObj.getJSONObject("stat").getString("name");
            int valor = statObj.getInt("base_stat");

            if (nombreStat.equals("hp"))       hp = valor;
            else if (nombreStat.equals("attack"))  ataque = valor;
            else if (nombreStat.equals("defense")) defensa = valor;
            else if (nombreStat.equals("speed"))   velocidad = valor;
        }

        //Intentar obtener sprite animado (GIF de Gen V Black/White)
        //Si no existe, usar el sprite estatico frontal como fallback
        String spriteUrl = null;
        try
        {
            String animado = json.getJSONObject("sprites")
                .getJSONObject("versions")
                .getJSONObject("generation-v")
                .getJSONObject("black-white")
                .getJSONObject("animated")
                .optString("front_default", null);

            if (animado != null && !animado.equals("null") && !animado.isEmpty())
                spriteUrl = animado;
        }
        catch (Exception ignored) {}

        if (spriteUrl == null)
            spriteUrl = json.getJSONObject("sprites").optString("front_default", null);

        Pokemon pokemon = new Pokemon(nombre, tipos, hp, ataque, defensa, velocidad, spriteUrl);

        //Descargar la imagen del sprite si existe
        if (spriteUrl != null && !spriteUrl.equals("null") && !spriteUrl.isEmpty())
        {
            try
            {
                URL url = new URL(spriteUrl);
                InputStream is = url.openStream();
                byte[] bytes = is.readAllBytes();
                is.close();
                pokemon.setSprite(new ImageIcon(bytes));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return pokemon;
    }
}

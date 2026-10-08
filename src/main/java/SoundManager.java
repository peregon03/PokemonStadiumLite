import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

//Gestiona la reproduccion de sonidos: cries MP3 reales de cada pokemon
//y sonidos sintetizados para eventos del combate.
//Los cries se descargan de Pokemon Showdown, que los distribuye como MP3.
//La libreria mp3spi registra automaticamente el decodificador en javax.sound.sampled.
public class SoundManager
{
    private static final float SAMPLE_RATE = 44100f;

    //Reproduce el cry (grito) oficial del pokemon descargando el MP3 desde Pokemon Showdown
    public static void playCry(String nombrePokemon)
    {
        String urlCry = "https://play.pokemonshowdown.com/audio/cries/"
            + normalizarNombre(nombrePokemon) + ".mp3";

        Thread hilo = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // Descargar los bytes del MP3 con HttpClient (mas robusto que URL.openStream con HTTPS)
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlCry))
                        .header("User-Agent", "Mozilla/5.0")
                        .build();
                    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                    if (response.statusCode() != 200) return;

                    byte[] mp3Bytes = response.body();
                    if (mp3Bytes == null || mp3Bytes.length == 0) return;

                    // Decodificar el MP3 a PCM usando mp3spi via ByteArrayInputStream
                    AudioInputStream rawStream = AudioSystem.getAudioInputStream(
                        new ByteArrayInputStream(mp3Bytes)
                    );

                    AudioFormat formatoMp3 = rawStream.getFormat();
                    AudioFormat formatoPcm = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        formatoMp3.getSampleRate(),
                        16,
                        formatoMp3.getChannels(),
                        formatoMp3.getChannels() * 2,
                        formatoMp3.getSampleRate(),
                        false
                    );
                    AudioInputStream pcmStream = AudioSystem.getAudioInputStream(formatoPcm, rawStream);

                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, formatoPcm);
                    SourceDataLine linea = (SourceDataLine) AudioSystem.getLine(info);
                    linea.open(formatoPcm);
                    linea.start();

                    byte[] buffer = new byte[4096];
                    int leidos;
                    while ((leidos = pcmStream.read(buffer, 0, buffer.length)) != -1)
                        linea.write(buffer, 0, leidos);

                    linea.drain();
                    linea.close();
                    pcmStream.close();
                }
                catch (Exception e)
                {
                    // Si el cry no esta disponible o falla la red, se ignora silenciosamente
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    //Convierte el nombre de la API al formato que usa Pokemon Showdown en su URL
    //Ejemplo: "nidoran-f" -> "nidoranf", "mr-mime" -> "mrmime", "ho-oh" -> "hooh"
    private static String normalizarNombre(String nombre)
    {
        return nombre.toLowerCase()
            .replace("-", "")
            .replace(".", "")
            .replace("'", "")
            .replace("\u2019", "") // comilla tipografica
            .replace(" ", "");
    }

    //Fanfarria de victoria (sintetizada, suena cuando se anuncia el ganador)
    public static void playVictoria()
    {
        reproducir(new int[]{523, 659, 784, 659, 1047}, new int[]{140, 140, 140, 70, 420});
    }

    //Melodia de inicio de combate (sintetizada)
    public static void playInicioCombate()
    {
        reproducir(new int[]{262, 294, 330, 349, 392}, new int[]{95, 95, 95, 95, 280});
    }

    // ---- Utilidades de sintesis ----

    private static void reproducir(int[] frecuencias, int[] duraciones)
    {
        Thread hilo = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < frecuencias.length; i++)
                    reproducirNota(frecuencias[i], duraciones[i]);
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void reproducirNota(int frecuencia, int duracionMs)
    {
        try
        {
            int muestras    = (int)(SAMPLE_RATE * duracionMs / 1000.0);
            int fadeSamples = (int)(SAMPLE_RATE * 0.018);
            byte[] buffer   = new byte[muestras * 2];

            for (int i = 0; i < muestras; i++)
            {
                double angulo     = 2.0 * Math.PI * i * frecuencia / SAMPLE_RATE;
                double envolvente = Math.min(1.0, Math.min(i / (double) fadeSamples,
                                                           (muestras - i) / (double) fadeSamples));
                short valor = (short)(Math.sin(angulo) * 7500 * envolvente);
                buffer[2 * i]     = (byte)(valor & 0xFF);
                buffer[2 * i + 1] = (byte)((valor >> 8) & 0xFF);
            }

            AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info  = new DataLine.Info(SourceDataLine.class, formato);
            SourceDataLine linea = (SourceDataLine) AudioSystem.getLine(info);
            linea.open(formato);
            linea.start();
            linea.write(buffer, 0, buffer.length);
            linea.drain();
            linea.close();
        }
        catch (Exception e)
        {
            // ignorar errores de audio
        }
    }
}

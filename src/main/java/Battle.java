import java.util.Random;

public class Battle
{
    private Pokemon pokemonUno;
    private Pokemon pokemonDos;
    private BattleListener listener;
    private Random random;

    public Battle(Pokemon pokemonUno, Pokemon pokemonDos, BattleListener listener)
    {
        this.pokemonUno = pokemonUno;
        this.pokemonDos = pokemonDos;
        this.listener = listener;
        this.random = new Random();
    }

    //Inicia el combate en un hilo separado para no bloquear la UI
    public void iniciarCombate()
    {
        Thread hiloCombate = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                ejecutarCombate();
            }
        });
        hiloCombate.setDaemon(true);
        hiloCombate.start();
    }

    private void ejecutarCombate()
    {
        //Determinar quien ataca primero segun velocidad
        Pokemon atacante;
        Pokemon defensor;

        if (pokemonUno.getVelocidad() > pokemonDos.getVelocidad())
        {
            atacante = pokemonUno;
            defensor = pokemonDos;
        }
        else if (pokemonDos.getVelocidad() > pokemonUno.getVelocidad())
        {
            atacante = pokemonDos;
            defensor = pokemonUno;
        }
        else
        {
            //Empate en velocidad: orden aleatorio
            if (random.nextBoolean())
            {
                atacante = pokemonUno;
                defensor = pokemonDos;
            }
            else
            {
                atacante = pokemonDos;
                defensor = pokemonUno;
            }
        }

        //Loop de combate hasta que un pokemon llegue a 0 HP
        while (pokemonUno.getHpActual() > 0 && pokemonDos.getHpActual() > 0)
        {
            //Calcular efectividad de tipo usando el primer tipo de cada pokemon
            double modificador = calcularEfectividad(
                atacante.getTipos().get(0),
                defensor.getTipos().get(0)
            );

            //Critico: 10% de probabilidad, multiplica el daño por 1.5
            boolean critico = random.nextInt(10) == 0;

            //Formula de daño: damage = (ATK * rand - DEF * rand), minimo 1
            double dañoBase = (atacante.getAtaque() * random.nextDouble())
                            - (defensor.getDefensa() * random.nextDouble());
            int daño = (int) Math.max(1, dañoBase);

            if (critico)
                daño = (int)(daño * 1.5);

            daño = (int)(daño * modificador);
            daño = Math.max(1, daño);

            //Aplicar daño al defensor
            defensor.setHpActual(defensor.getHpActual() - daño);

            //Notificar a la UI sobre el turno y el cambio de HP
            listener.onTurn(atacante.getNombre(), defensor.getNombre(), daño, critico, modificador);
            listener.onHpChanged(defensor.getNombre(), defensor.getHpActual());

            if (defensor.getHpActual() <= 0)
                break;

            //Pausa entre turnos para que el combate se vea paso a paso
            try
            {
                Thread.sleep(1200);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            //Intercambiar roles: el defensor pasa a atacar
            Pokemon temp = atacante;
            atacante = defensor;
            defensor = temp;
        }

        //Determinar y notificar al ganador
        String ganador;
        if (pokemonUno.getHpActual() <= 0)
            ganador = pokemonDos.getNombre();
        else
            ganador = pokemonUno.getNombre();

        listener.onBattleEnded(ganador);
    }

    //Calcula el modificador de tipo: agua > fuego > planta > agua (x1.3 / x0.7 / x1.0)
    private double calcularEfectividad(String tipoAtacante, String tipoDefensor)
    {
        if (tipoAtacante.equals("water") && tipoDefensor.equals("fire"))
            return 1.3;
        else if (tipoAtacante.equals("fire") && tipoDefensor.equals("grass"))
            return 1.3;
        else if (tipoAtacante.equals("grass") && tipoDefensor.equals("water"))
            return 1.3;
        else if (tipoAtacante.equals("fire") && tipoDefensor.equals("water"))
            return 0.7;
        else if (tipoAtacante.equals("grass") && tipoDefensor.equals("fire"))
            return 0.7;
        else if (tipoAtacante.equals("water") && tipoDefensor.equals("grass"))
            return 0.7;
        else
            return 1.0;
    }
}

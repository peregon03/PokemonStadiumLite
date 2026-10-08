//Interfaz para desacoplar la logica del combate de la interfaz grafica
public interface BattleListener
{
    //Se llama cada vez que un pokemon ataca al otro
    void onTurn(String attacker, String defender, int damage, boolean critical, double modifier);

    //Se llama cuando el HP de un pokemon cambia
    void onHpChanged(String pokemon, int hpActual);

    //Se llama cuando el combate termina con un ganador
    void onBattleEnded(String winner);
}

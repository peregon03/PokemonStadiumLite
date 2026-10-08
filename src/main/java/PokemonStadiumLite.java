import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class PokemonStadiumLite extends JFrame
{
    // ---- Paleta de colores ----
    private static final Color C_FONDO    = new Color(8, 8, 25);
    private static final Color C_PANEL    = new Color(15, 15, 45, 215);
    private static final Color C_BORDE    = new Color(255, 203, 5);
    private static final Color C_ACENTO   = new Color(59, 76, 202);
    private static final Color C_ROJO     = new Color(204, 0, 0);
    private static final Color C_TEXTO    = Color.WHITE;
    private static final Color C_STAT_HP  = new Color(78, 210, 90);
    private static final Color C_STAT_ATK = new Color(255, 90, 90);
    private static final Color C_STAT_DEF = new Color(80, 150, 255);
    private static final Color C_STAT_VEL = new Color(255, 210, 60);
    private static final int   STAT_MAX   = 255;

    // ---- Sprites en la arena (dibujados directamente en paintComponent) ----
    private Image  imagenSprite1 = null, imagenSprite2 = null;
    private JPanel panelBattleArena;
    private int     xBase1, yBase1, xBase2, yBase2;  // posiciones base de cada sprite
    private int     xAtk1 = 0, xAtk2 = 0;            // desplazamiento horizontal de ataque
    private int     yBob1 = 0, yBob2 = 0;            // desplazamiento vertical de flotacion

    // ---- Animacion ----
    private int   animTick = 0;
    private Timer animTimer;
    private Timer timerAtaque;
    private int   pasoAtaque = 0;
    private int   ladoAtacanteActual = 0;

    // ---- Info en tarjetas ----
    private JLabel etiquetaNombre1, etiquetaNombre2;
    private JPanel panelTipos1, panelTipos2;

    // ---- Barras de stats (0=HP, 1=ATK, 2=DEF, 3=VEL) ----
    private JProgressBar[] barrasStats1 = new JProgressBar[4];
    private JProgressBar[] barrasStats2 = new JProgressBar[4];
    private JLabel[]       valoresStats1 = new JLabel[4];
    private JLabel[]       valoresStats2 = new JLabel[4];

    // ---- Controles de carga ----
    private JTextField campoNombre1, campoNombre2;
    private JButton    botonCargar1, botonAleatorio1;
    private JButton    botonCargar2, botonAleatorio2;

    // ---- Combate ----
    private JButton    botonPelear;
    private JTextArea  areaLog;
    private Pokemon    pokemon1, pokemon2;
    private Random     random;

    // ---- Autocompletado ----
    private List<String> nombresPokemon = new ArrayList<>();

    public PokemonStadiumLite()
    {
        super("Pokemon Stadium Lite");
        random = new Random();
        construirUI();
        iniciarAnimacion();
        cargarNombresPokemon();
    }

    // =========================================================================
    // UI
    // =========================================================================

    private void construirUI()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 760);
        setLocationRelativeTo(null);

        // Fondo oscuro de toda la ventana
        PanelFondo panelFondo = new PanelFondo();
        panelFondo.setLayout(new BorderLayout(0, 0));
        setContentPane(panelFondo);

        // ---- Tarjetas compactas laterales ----
        panelFondo.add(construirTarjeta(1), BorderLayout.WEST);
        panelFondo.add(construirTarjeta(2), BorderLayout.EAST);

        // ---- Arena central con sprites ----
        panelBattleArena = new PanelArena();
        panelBattleArena.setLayout(null); // posicionamiento absoluto para sprites

        // Recalcular posiciones base cuando cambia el tamanio del panel
        panelBattleArena.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e) { recalcularPosicionesBase(); }
        });

        panelFondo.add(panelBattleArena, BorderLayout.CENTER);

        // ---- Panel sur: botones + log ----
        botonPelear = new JButton("  \u2694  FIGHT!  ");
        botonPelear.setEnabled(false);
        estilizarBoton(botonPelear, C_ROJO);
        botonPelear.setFont(new Font("Arial", Font.BOLD, 17));
        botonPelear.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) { iniciarCombate(); }
        });

        JButton botonReiniciar = new JButton("Reiniciar");
        estilizarBoton(botonReiniciar, new Color(35, 35, 75));
        botonReiniciar.setFont(new Font("Arial", Font.PLAIN, 12));
        botonReiniciar.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) { reiniciarUI(); }
        });

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 5));
        panelBotones.setOpaque(false);
        panelBotones.add(botonPelear);
        panelBotones.add(botonReiniciar);

        areaLog = new JTextArea(6, 60);
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        areaLog.setBackground(new Color(10, 10, 30));
        areaLog.setForeground(new Color(200, 220, 255));
        areaLog.setCaretColor(C_BORDE);

        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_BORDE, 1),
            "  Registro de batalla",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 11), C_BORDE));
        scrollLog.getViewport().setBackground(new Color(10, 10, 30));
        scrollLog.setOpaque(false);

        JPanel panelSur = new JPanel(new BorderLayout(0, 3));
        panelSur.setOpaque(false);
        panelSur.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        panelSur.add(panelBotones, BorderLayout.NORTH);
        panelSur.add(scrollLog,   BorderLayout.CENTER);

        panelFondo.add(panelSur, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Tarjeta compacta lateral: solo info del pokemon, sin sprite
    private JPanel construirTarjeta(int lado)
    {
        JPanel panel = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(C_BORDE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(195, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Titulo del jugador
        JLabel lblJugador = new JLabel("JUGADOR " + lado, SwingConstants.CENTER);
        lblJugador.setFont(new Font("Arial", Font.BOLD, 11));
        lblJugador.setForeground(C_BORDE);
        lblJugador.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Separador decorativo
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 203, 5, 60));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Nombre del pokemon
        JLabel etiquetaNombre = new JLabel("---", SwingConstants.CENTER);
        etiquetaNombre.setFont(new Font("Arial", Font.BOLD, 16));
        etiquetaNombre.setForeground(C_TEXTO);
        etiquetaNombre.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Tipos
        JPanel panelTipos = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        panelTipos.setOpaque(false);
        panelTipos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Stats
        JPanel panelStats = new JPanel();
        panelStats.setLayout(new BoxLayout(panelStats, BoxLayout.Y_AXIS));
        panelStats.setOpaque(false);
        panelStats.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        panelStats.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar[] barras = lado == 1 ? barrasStats1 : barrasStats2;
        JLabel[]        vals  = lado == 1 ? valoresStats1 : valoresStats2;
        String[] nombres = {"HP ", "ATK", "DEF", "VEL"};
        Color[]  colores = {C_STAT_HP, C_STAT_ATK, C_STAT_DEF, C_STAT_VEL};

        for (int i = 0; i < 4; i++)
        {
            JProgressBar barra = new JProgressBar(0, STAT_MAX);
            barra.setValue(0);
            barra.setStringPainted(false);
            barra.setForeground(colores[i]);
            barra.setBackground(new Color(28, 28, 58));
            barra.setBorderPainted(false);
            barra.setPreferredSize(new Dimension(90, 9));
            barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9));

            JLabel lblValor = new JLabel("---");
            lblValor.setForeground(C_TEXTO);
            lblValor.setFont(new Font("Arial", Font.PLAIN, 10));
            lblValor.setPreferredSize(new Dimension(30, 13));
            lblValor.setHorizontalAlignment(SwingConstants.RIGHT);

            barras[i] = barra;
            vals[i]   = lblValor;

            JPanel fila = new JPanel(new GridBagLayout());
            fila.setOpaque(false);
            fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            GridBagConstraints gbc = new GridBagConstraints();

            JLabel lblNom = new JLabel(nombres[i]);
            lblNom.setForeground(colores[i]);
            lblNom.setFont(new Font("Arial", Font.BOLD, 10));
            lblNom.setPreferredSize(new Dimension(30, 13));

            gbc.gridx = 0; gbc.weightx = 0; gbc.insets = new Insets(1, 2, 1, 4);
            fila.add(lblNom, gbc);
            gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            fila.add(barra, gbc);
            gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(1, 4, 1, 2);
            fila.add(lblValor, gbc);

            panelStats.add(fila);
        }

        // Controles de carga
        final int ladoFinal = lado;
        JTextField campoNombre = new JTextField(10);
        campoNombre.setBackground(new Color(20, 20, 55));
        campoNombre.setForeground(C_TEXTO);
        campoNombre.setCaretColor(C_BORDE);
        campoNombre.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDE, 1),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)));

        JButton botonCargar    = new JButton("Load");
        JButton botonAleatorio = new JButton("Rand");
        estilizarBoton(botonCargar, C_ACENTO);
        estilizarBoton(botonAleatorio, new Color(55, 90, 40));
        botonCargar.setFont(new Font("Arial", Font.BOLD, 11));
        botonAleatorio.setFont(new Font("Arial", Font.BOLD, 11));

        // Dos filas: campo de texto arriba, Load+Rand abajo
        JPanel filaBotones = new JPanel(new GridLayout(1, 2, 4, 0));
        filaBotones.setOpaque(false);
        filaBotones.add(botonCargar);
        filaBotones.add(botonAleatorio);

        JPanel panelCarga = new JPanel(new BorderLayout(0, 4));
        panelCarga.setOpaque(false);
        panelCarga.add(campoNombre, BorderLayout.NORTH);
        panelCarga.add(filaBotones, BorderLayout.CENTER);
        panelCarga.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        // Guardar referencias
        if (lado == 1)
        {
            etiquetaNombre1 = etiquetaNombre;
            panelTipos1     = panelTipos;
            campoNombre1    = campoNombre;
            botonCargar1    = botonCargar;
            botonAleatorio1 = botonAleatorio;
        }
        else
        {
            etiquetaNombre2 = etiquetaNombre;
            panelTipos2     = panelTipos;
            campoNombre2    = campoNombre;
            botonCargar2    = botonCargar;
            botonAleatorio2 = botonAleatorio;
        }

        botonCargar.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String n = campoNombre.getText().trim();
                if (!n.isEmpty()) cargarPokemon(ladoFinal, n);
            }
        });
        botonAleatorio.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                cargarPokemon(ladoFinal, String.valueOf(random.nextInt(1025) + 1));
            }
        });
        campoNombre.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String n = campoNombre.getText().trim();
                if (!n.isEmpty()) cargarPokemon(ladoFinal, n);
            }
        });

        panel.add(lblJugador);
        panel.add(Box.createVerticalStrut(4));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(8));
        panel.add(etiquetaNombre);
        panel.add(Box.createVerticalStrut(4));
        panel.add(panelTipos);
        panel.add(Box.createVerticalStrut(10));
        panel.add(panelStats);
        panel.add(Box.createVerticalGlue());
        panel.add(panelCarga);

        configurarAutocompletado(campoNombre, ladoFinal);
        return panel;
    }

    // =========================================================================
    // Animacion
    // =========================================================================

    // Recalcula las posiciones base de los sprites segun el tamanio actual de la arena
    private void recalcularPosicionesBase()
    {
        int w = panelBattleArena.getWidth();
        int h = panelBattleArena.getHeight();
        if (w == 0 || h == 0) return;

        // La plataforma oval esta centrada a ~72% de altura, los sprites justo encima
        int platY  = (int)(h * 0.72);
        int baseY  = platY - 170; // sprites "parados" sobre la plataforma

        xBase1 = w / 4 - 100;
        yBase1 = baseY;
        xBase2 = 3 * w / 4 - 100;
        yBase2 = baseY;

        actualizarPosicionesSprites();
    }

    // Solicita repintar la arena; los sprites se dibujan en paintComponent con las posiciones actuales
    private void actualizarPosicionesSprites()
    {
        if (panelBattleArena != null) panelBattleArena.repaint();
    }

    // Timer de 50ms: mantiene el efecto de flotacion sinusoidal en ambos sprites
    private void iniciarAnimacion()
    {
        animTimer = new Timer(50, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                animTick++;
                yBob1 = (int)(7 * Math.sin(animTick * 0.14));
                yBob2 = (int)(7 * Math.sin(animTick * 0.14 + Math.PI));
                actualizarPosicionesSprites();
            }
        });
        animTimer.start();
    }

    // Animacion de inicio de combate: ambos sprites se lanzan al centro y retroceden
    private void animarInicioCombate()
    {
        detenerAnimacionAtaque();
        pasoAtaque = 0;

        timerAtaque = new Timer(18, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                pasoAtaque++;
                int w    = panelBattleArena != null ? panelBattleArena.getWidth() : 650;
                int dist = Math.max(60, w / 7);

                if (pasoAtaque <= 10)
                {
                    // Ambos se acercan al centro
                    xAtk1 =  dist * pasoAtaque / 10;
                    xAtk2 = -dist * pasoAtaque / 10;
                }
                else if (pasoAtaque <= 14) { /* pausa dramatica */ }
                else if (pasoAtaque <= 24)
                {
                    // Retroceden a sus posiciones
                    int r = pasoAtaque - 14;
                    xAtk1 =  dist * (10 - r) / 10;
                    xAtk2 = -dist * (10 - r) / 10;
                }
                else
                {
                    xAtk1 = 0; xAtk2 = 0;
                    timerAtaque.stop();
                }
                actualizarPosicionesSprites();
            }
        });
        timerAtaque.start();
    }

    // Animacion de ataque: el atacante se lanza hacia el defensor y retrocede
    private void animarAtaque(int lado)
    {
        detenerAnimacionAtaque();
        pasoAtaque         = 0;
        ladoAtacanteActual = lado;
        int dir = lado == 1 ? 1 : -1; // lado 1 ataca a la derecha, lado 2 a la izquierda

        timerAtaque = new Timer(16, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                pasoAtaque++;
                int w    = panelBattleArena != null ? panelBattleArena.getWidth() : 650;
                int dist = Math.max(80, w / 5);

                if (pasoAtaque <= 10)
                {
                    // Avance rapido hacia el rival
                    int off = dist * pasoAtaque / 10;
                    if (lado == 1) xAtk1 =  off;
                    else           xAtk2 = -off;
                }
                else if (pasoAtaque <= 13) { /* impacto: pausa breve */ }
                else if (pasoAtaque <= 22)
                {
                    // Retroceso
                    int r   = pasoAtaque - 13;
                    int off = dist * (10 - r) / 10;
                    if (lado == 1) xAtk1 =  off;
                    else           xAtk2 = -off;
                }
                else
                {
                    xAtk1 = 0; xAtk2 = 0;
                    timerAtaque.stop();
                }
                actualizarPosicionesSprites();
            }
        });
        timerAtaque.start();
    }

    private void detenerAnimacionAtaque()
    {
        if (timerAtaque != null && timerAtaque.isRunning())
        {
            timerAtaque.stop();
            xAtk1 = 0; xAtk2 = 0;
        }
    }

    // =========================================================================
    // Logica de carga y combate
    // =========================================================================

    private void cargarNombresPokemon()
    {
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>()
        {
            @Override
            protected List<String> doInBackground() throws Exception
            {
                return PokeApiClient.obtenerTodosLosNombres();
            }
            @Override
            protected void done()
            {
                try { nombresPokemon = get(); }
                catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void cargarPokemon(int lado, String nombreOId)
    {
        if (lado == 1) { botonCargar1.setEnabled(false); botonAleatorio1.setEnabled(false); }
        else           { botonCargar2.setEnabled(false); botonAleatorio2.setEnabled(false); }

        SwingWorker<Pokemon, Void> worker = new SwingWorker<Pokemon, Void>()
        {
            @Override
            protected Pokemon doInBackground() throws Exception
            {
                return PokeApiClient.consultarPokemon(nombreOId);
            }

            @Override
            protected void done()
            {
                if (lado == 1) { botonCargar1.setEnabled(true); botonAleatorio1.setEnabled(true); }
                else           { botonCargar2.setEnabled(true); botonAleatorio2.setEnabled(true); }

                try
                {
                    Pokemon pokemon = get();
                    mostrarPokemon(lado, pokemon);
                    SoundManager.playCry(pokemon.getNombre());
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(PokemonStadiumLite.this,
                        "Pokemon no encontrado: \"" + nombreOId + "\"\nVerifica el nombre o ID.",
                        "Error de carga", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void mostrarPokemon(int lado, Pokemon pokemon)
    {
        if (lado == 1) { pokemon1 = pokemon; actualizarPanelVisual(pokemon, 1); }
        else           { pokemon2 = pokemon; actualizarPanelVisual(pokemon, 2); }

        if (pokemon1 != null && pokemon2 != null)
            botonPelear.setEnabled(true);
    }

    private void actualizarPanelVisual(Pokemon pokemon, int lado)
    {
        JLabel         nombre = lado == 1 ? etiquetaNombre1 : etiquetaNombre2;
        JPanel         tipos  = lado == 1 ? panelTipos1     : panelTipos2;
        JProgressBar[] barras = lado == 1 ? barrasStats1    : barrasStats2;
        JLabel[]       vals   = lado == 1 ? valoresStats1   : valoresStats2;

        // Guardar imagen para dibujarla en PanelArena.paintComponent (sin JLabel intermedio)
        Image img = pokemon.getSprite() != null ? pokemon.getSprite().getImage() : null;
        if (lado == 1) imagenSprite1 = img;
        else           imagenSprite2 = img;
        panelBattleArena.repaint();

        nombre.setText(pokemon.getNombre().toUpperCase());

        tipos.removeAll();
        for (String tipo : pokemon.getTipos())
        {
            JLabel badge = new JLabel(" " + tipo.toUpperCase() + " ");
            badge.setOpaque(true);
            badge.setBackground(colorDeTipo(tipo));
            badge.setForeground(Color.WHITE);
            badge.setFont(new Font("Arial", Font.BOLD, 9));
            badge.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            tipos.add(badge);
        }
        tipos.revalidate();
        tipos.repaint();

        int[] statVals = {pokemon.getHp(), pokemon.getAtaque(), pokemon.getDefensa(), pokemon.getVelocidad()};
        int[] statMaxs = {pokemon.getHp(), STAT_MAX, STAT_MAX, STAT_MAX};
        for (int i = 0; i < 4; i++)
        {
            barras[i].setMaximum(statMaxs[i]);
            barras[i].setValue(statVals[i]);
            vals[i].setText(String.valueOf(statVals[i]));
        }
        barras[0].setForeground(C_STAT_HP);
    }

    private void iniciarCombate()
    {
        pokemon1.setHpActual(pokemon1.getHp());
        pokemon2.setHpActual(pokemon2.getHp());

        barrasStats1[0].setMaximum(pokemon1.getHp());
        barrasStats1[0].setValue(pokemon1.getHp());
        valoresStats1[0].setText(String.valueOf(pokemon1.getHp()));
        barrasStats1[0].setForeground(C_STAT_HP);

        barrasStats2[0].setMaximum(pokemon2.getHp());
        barrasStats2[0].setValue(pokemon2.getHp());
        valoresStats2[0].setText(String.valueOf(pokemon2.getHp()));
        barrasStats2[0].setForeground(C_STAT_HP);

        areaLog.setText("");
        botonPelear.setEnabled(false);
        botonCargar1.setEnabled(false); botonAleatorio1.setEnabled(false);
        botonCargar2.setEnabled(false); botonAleatorio2.setEnabled(false);

        agregarLog("\u2550\u2550\u2550 COMBATE: " + pokemon1.getNombre().toUpperCase()
            + " vs " + pokemon2.getNombre().toUpperCase() + " \u2550\u2550\u2550");

        SoundManager.playInicioCombate();
        animarInicioCombate(); // ambos sprites se lanzan al centro al comenzar

        BattleListener bl = new BattleListener()
        {
            @Override
            public void onTurn(String attacker, String defender, int damage, boolean critical, double modifier)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        SoundManager.playCry(attacker);
                        int ladoAtacante = attacker.equals(pokemon1.getNombre()) ? 1 : 2;
                        animarAtaque(ladoAtacante);

                        String extras = "";
                        if (critical)        extras += " \u26a1[CRITICO!]";
                        if (modifier > 1.0)  extras += " [Super efectivo!]";
                        else if (modifier < 1.0) extras += " [Poco efectivo]";

                        agregarLog("\u25b6 " + attacker.toUpperCase() + " \u2192 " + defender.toUpperCase()
                            + "   da\u00f1o: " + damage + extras);
                    }
                });
            }

            @Override
            public void onHpChanged(String pokemonNombre, int hpActual)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run() { actualizarBarraHp(pokemonNombre, hpActual); }
                });
            }

            @Override
            public void onBattleEnded(String winner)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        detenerAnimacionAtaque();
                        SoundManager.playVictoria();
                        agregarLog("\u2605 " + winner.toUpperCase() + " GANO EL COMBATE! \u2605");
                        botonPelear.setEnabled(true);
                        botonCargar1.setEnabled(true); botonAleatorio1.setEnabled(true);
                        botonCargar2.setEnabled(true); botonAleatorio2.setEnabled(true);

                        String loser = winner.equals(pokemon1.getNombre())
                            ? pokemon2.getNombre() : pokemon1.getNombre();
                        mostrarDialogoGanador(winner, loser);
                    }
                });
            }
        };

        new Battle(pokemon1, pokemon2, bl).iniciarCombate();
    }

    private void actualizarBarraHp(String nombrePokemon, int hpActual)
    {
        if (pokemon1 != null && pokemon1.getNombre().equals(nombrePokemon))
        {
            barrasStats1[0].setValue(hpActual);
            valoresStats1[0].setText(String.valueOf(hpActual));
            barrasStats1[0].setForeground(colorSegunHp(hpActual, pokemon1.getHp()));
            agregarLog("  " + pokemon1.getNombre().toUpperCase() + " HP: " + hpActual);
        }
        else if (pokemon2 != null && pokemon2.getNombre().equals(nombrePokemon))
        {
            barrasStats2[0].setValue(hpActual);
            valoresStats2[0].setText(String.valueOf(hpActual));
            barrasStats2[0].setForeground(colorSegunHp(hpActual, pokemon2.getHp()));
            agregarLog("  " + pokemon2.getNombre().toUpperCase() + " HP: " + hpActual);
        }
    }

    // =========================================================================
    // Autocompletado
    // =========================================================================

    private void configurarAutocompletado(JTextField campo, int lado)
    {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        JList<String> lista = new JList<>(modelo);
        lista.setBackground(new Color(15, 15, 45));
        lista.setForeground(C_TEXTO);
        lista.setSelectionBackground(C_BORDE);
        lista.setSelectionForeground(new Color(10, 10, 30));
        lista.setFont(new Font("Arial", Font.PLAIN, 12));
        lista.setFixedCellHeight(26);
        lista.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JScrollPane scroll = new JScrollPane(lista,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDE, 1));
        scroll.getViewport().setBackground(new Color(15, 15, 45));

        JWindow ventana = new JWindow(this);
        ventana.setFocusableWindowState(false); // no robar foco al campo de texto
        ventana.setContentPane(scroll);

        campo.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e)  { actualizar(); }
            @Override public void removeUpdate(DocumentEvent e)  { actualizar(); }
            @Override public void changedUpdate(DocumentEvent e) {}

            private void actualizar()
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String texto = campo.getText().trim().toLowerCase();
                        modelo.clear();

                        if (texto.length() < 2 || nombresPokemon.isEmpty())
                        {
                            ventana.setVisible(false);
                            return;
                        }

                        List<String> empiezan = nombresPokemon.stream()
                            .filter(n -> n.startsWith(texto)).limit(7)
                            .collect(Collectors.toList());
                        List<String> contienen = nombresPokemon.stream()
                            .filter(n -> !n.startsWith(texto) && n.contains(texto)).limit(4)
                            .collect(Collectors.toList());

                        for (String n : empiezan)  modelo.addElement(n);
                        for (String n : contienen) modelo.addElement(n);

                        if (modelo.isEmpty()) { ventana.setVisible(false); return; }

                        try
                        {
                            Point pos = campo.getLocationOnScreen();
                            ventana.setBounds(pos.x, pos.y + campo.getHeight(),
                                220, Math.min(modelo.size() * 26 + 4, 220));
                            ventana.setVisible(true);
                        }
                        catch (Exception ignored) {}
                    }
                });
            }
        });

        lista.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                int index = lista.locationToIndex(e.getPoint());
                if (index >= 0)
                {
                    lista.setSelectedIndex(index);
                    String sel = lista.getSelectedValue();
                    if (sel != null) { campo.setText(sel); ventana.setVisible(false); cargarPokemon(lado, sel); }
                }
            }
        });

        // Con setFocusableWindowState(false) el JWindow nunca roba foco,
        // por lo que focusLost solo se dispara cuando el usuario hace clic en otro lugar de verdad
        campo.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                Timer t = new Timer(150, ev -> ventana.setVisible(false));
                t.setRepeats(false); t.start();
            }
        });
    }

    // =========================================================================
    // Dialogo de ganador
    // =========================================================================

    private void mostrarDialogoGanador(String winner, String loser)
    {
        JDialog dialogo = new JDialog(this, "Resultado del combate", true);
        dialogo.setUndecorated(true);
        dialogo.setSize(400, 260);
        dialogo.setLocationRelativeTo(this);

        JPanel contenido = new JPanel()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(5, 5, 20), 0, h, new Color(22, 18, 55)));
                g2.fillRoundRect(0, 0, w, h, 20, 20);
                g2.setColor(C_BORDE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 20, 20);
                g2.setColor(new Color(255, 203, 5, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(20, 48, w - 20, 48);
                g2.dispose();
            }
        };
        contenido.setLayout(new BorderLayout());
        contenido.setOpaque(false);

        JLabel lblTitulo = new JLabel("  Resultado del combate", SwingConstants.LEFT);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 12));
        lblTitulo.setForeground(C_BORDE);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel panelResultado = new JPanel();
        panelResultado.setLayout(new BoxLayout(panelResultado, BoxLayout.Y_AXIS));
        panelResultado.setOpaque(false);
        panelResultado.setBorder(BorderFactory.createEmptyBorder(18, 20, 10, 20));

        JLabel lblEtiqueta = new JLabel("\u2605  GANADOR  \u2605", SwingConstants.CENTER);
        lblEtiqueta.setFont(new Font("Arial", Font.BOLD, 13));
        lblEtiqueta.setForeground(new Color(255, 203, 5, 160));
        lblEtiqueta.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblGanador = new JLabel(winner.toUpperCase(), SwingConstants.CENTER);
        lblGanador.setFont(new Font("Arial", Font.BOLD, 32));
        lblGanador.setForeground(C_BORDE);
        lblGanador.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSep = new JLabel("\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014", SwingConstants.CENTER);
        lblSep.setForeground(new Color(100, 100, 140));
        lblSep.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblPerdedor = new JLabel(loser.toUpperCase() + " fue derrotado", SwingConstants.CENTER);
        lblPerdedor.setFont(new Font("Arial", Font.PLAIN, 13));
        lblPerdedor.setForeground(new Color(160, 160, 190));
        lblPerdedor.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelResultado.add(lblEtiqueta);
        panelResultado.add(Box.createVerticalStrut(6));
        panelResultado.add(lblGanador);
        panelResultado.add(Box.createVerticalStrut(8));
        panelResultado.add(lblSep);
        panelResultado.add(Box.createVerticalStrut(8));
        panelResultado.add(lblPerdedor);

        JButton btnCerrar = new JButton("Cerrar");
        estilizarBoton(btnCerrar, C_ACENTO);
        btnCerrar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCerrar.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) { dialogo.dispose(); }
        });

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        panelBoton.setOpaque(false);
        panelBoton.add(btnCerrar);

        contenido.add(lblTitulo, BorderLayout.NORTH);
        contenido.add(panelResultado, BorderLayout.CENTER);
        contenido.add(panelBoton, BorderLayout.SOUTH);

        dialogo.setContentPane(contenido);
        dialogo.setVisible(true);
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private void reiniciarUI()
    {
        pokemon1 = null; pokemon2 = null;
        detenerAnimacionAtaque();

        etiquetaNombre1.setText("---"); etiquetaNombre2.setText("---");
        panelTipos1.removeAll(); panelTipos1.revalidate(); panelTipos1.repaint();
        panelTipos2.removeAll(); panelTipos2.revalidate(); panelTipos2.repaint();
        campoNombre1.setText(""); campoNombre2.setText("");

        for (int i = 0; i < 4; i++)
        {
            barrasStats1[i].setValue(0); valoresStats1[i].setText("---");
            barrasStats2[i].setValue(0); valoresStats2[i].setText("---");
        }
        barrasStats1[0].setForeground(C_STAT_HP);
        barrasStats2[0].setForeground(C_STAT_HP);

        imagenSprite1 = null;
        imagenSprite2 = null;
        panelBattleArena.repaint();

        areaLog.setText("");
        botonPelear.setEnabled(false);
    }

    private Color colorSegunHp(int hpActual, int hpMax)
    {
        double pct = (double) hpActual / hpMax;
        if (pct > 0.50) return C_STAT_HP;
        if (pct > 0.25) return new Color(255, 165, 0);
        return new Color(220, 20, 20);
    }

    private void agregarLog(String mensaje)
    {
        areaLog.append(mensaje + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    private void estilizarBoton(JButton btn, Color colorFondo)
    {
        btn.setBackground(colorFondo);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
    }

    private static Color colorDeTipo(String tipo)
    {
        switch (tipo.toLowerCase())
        {
            case "fire":     return new Color(200, 65, 25);
            case "water":    return new Color(35, 105, 215);
            case "grass":    return new Color(55, 155, 55);
            case "electric": return new Color(195, 160, 0);
            case "psychic":  return new Color(205, 35, 115);
            case "ice":      return new Color(75, 185, 215);
            case "dragon":   return new Color(75, 45, 185);
            case "dark":     return new Color(75, 50, 35);
            case "fairy":    return new Color(205, 95, 155);
            case "fighting": return new Color(165, 40, 18);
            case "flying":   return new Color(105, 135, 205);
            case "poison":   return new Color(135, 50, 135);
            case "ground":   return new Color(185, 150, 45);
            case "rock":     return new Color(155, 140, 45);
            case "bug":      return new Color(105, 160, 18);
            case "ghost":    return new Color(80, 60, 135);
            case "steel":    return new Color(135, 140, 155);
            case "normal":   return new Color(125, 125, 105);
            default:         return new Color(75, 75, 75);
        }
    }

    // =========================================================================
    // Inner classes: paneles con pintura personalizada
    // =========================================================================

    // Fondo oscuro degradado de toda la ventana
    private class PanelFondo extends JPanel
    {
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(5, 5, 20), 0, h, new Color(22, 18, 55)));
            g2.fillRect(0, 0, w, h);
            // Luz cenital sutil
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(w / 2f, 0), h * 0.8f,
                new float[]{0f, 1f},
                new Color[]{new Color(80, 105, 225, 45), new Color(0, 0, 0, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    // Panel central de la arena: dibuja la plataforma oval y contiene los sprites
    private class PanelArena extends JPanel
    {
        // Cuando un GIF animado actualiza un frame, Java por defecto solo repinta la region
        // que cambio (repaint parcial), dejando artefactos del frame anterior visibles como
        // un rectangulo. Forzar repaint completo elimina ese recuadro titiante.
        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h)
        {
            if ((infoflags & (FRAMEBITS | ALLBITS)) != 0)
            {
                repaint();
                return (infoflags & ALLBITS) == 0;
            }
            return super.imageUpdate(img, infoflags, x, y, w, h);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // Fondo oscuro propio del panel para evitar el recuadro gris por defecto
            g2.setPaint(new GradientPaint(0, 0, new Color(5, 5, 20), 0, h, new Color(22, 18, 55)));
            g2.fillRect(0, 0, w, h);


            // Plataforma oval
            int pw = (int)(w * 0.90);
            int ph = (int)(h * 0.22);
            int px = (w - pw) / 2;
            int py = (int)(h * 0.72);

            // Sombra
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(px + 8, py + 8, pw, ph);

            // Base con degradado
            g2.setPaint(new GradientPaint(0, py, new Color(72, 55, 26), 0, py + ph, new Color(42, 30, 11)));
            g2.fillOval(px, py, pw, ph);

            // Cuadricula sobre la plataforma
            Shape clipAntes = g2.getClip();
            g2.setClip(new Ellipse2D.Float(px, py, pw, ph));
            g2.setColor(new Color(100, 78, 36, 90));
            g2.setStroke(new BasicStroke(1f));
            for (int x = px; x < px + pw; x += 22) g2.drawLine(x, py, x, py + ph);
            for (int y = py; y < py + ph; y += 22) g2.drawLine(px, y, px + pw, y);
            g2.setClip(clipAntes);

            // Linea divisoria central
            g2.setColor(new Color(255, 203, 5, 160));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(w / 2, py + 5, w / 2, py + ph - 5);

            // Circulo central
            int cr = 15;
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(w / 2 - cr, py + ph / 2 - cr, cr * 2, cr * 2);

            // Borde dorado exterior
            g2.setColor(new Color(255, 203, 5, 200));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(px, py, pw, ph);

            // Borde dorado interior
            g2.setColor(new Color(255, 203, 5, 80));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(px + 15, py + 6, pw - 30, ph - 12);

            // Brillo radial
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(w / 2f, py + ph / 2f), pw / 2.0f,
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(255, 225, 105, 28), new Color(255, 200, 80, 8), new Color(0, 0, 0, 0)}));
            g2.fillOval(px - 20, py - 10, pw + 40, ph + 20);

            // ---- Sprites dibujados directamente (sin JLabel, sin recuadros) ----
            // Al pasar 'this' como ImageObserver, los GIFs animados se actualizan solos
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            dibujarSprite(g2, imagenSprite1, xBase1 + xAtk1, yBase1 + yBob1);
            dibujarSprite(g2, imagenSprite2, xBase2 + xAtk2, yBase2 + yBob2);

            g2.dispose();
        }

        // Escala la imagen respetando su proporcion y la centra en el area de 200x200.
        // Se escala primero a un BufferedImage TYPE_INT_ARGB (fondo 100% transparente)
        // para que la interpolacion bilineal no mezcle pixeles del borde del sprite
        // con los colores de la plataforma que se dibujaron antes en este mismo paintComponent.
        private void dibujarSprite(Graphics2D g2, Image img, int x, int y)
        {
            if (img == null) return;
            // img.getWidth(this) registra PanelArena como ImageObserver:
            // cuando el GIF avanza un frame, imageUpdate() -> repaint() -> paintComponent() de nuevo
            int iw = img.getWidth(this);
            int ih = img.getHeight(this);
            if (iw <= 0 || ih <= 0) return;

            double escala = Math.min(200.0 / iw, 200.0 / ih);
            int dw = (int)(iw * escala);
            int dh = (int)(ih * escala);
            int dx = x + (200 - dw) / 2;
            int dy = y + (200 - dh) / 2;

            // Buffer ARGB limpio: todos los pixeles empiezan en (0,0,0,0) = transparente total.
            // La interpolacion bilineal mezcla borde del sprite con transparente, no con el fondo.
            BufferedImage buf = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = buf.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bg.drawImage(img, 0, 0, dw, dh, null); // null: dibuja el frame actual del GIF
            bg.dispose();

            g2.drawImage(buf, dx, dy, null);
        }
    }

    // =========================================================================

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run() { new PokemonStadiumLite(); }
        });
    }
}

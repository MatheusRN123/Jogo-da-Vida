package moore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class JogoDaVida {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Jogo da Vida - Moore");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            // Painel da grade (30 colunas, 20 linhas, células de 20px)
            PainelVida painel = new PainelVida(30, 20, 20); 
            frame.add(painel, BorderLayout.CENTER);

            // Botões e controles
            JToggleButton botaoIniciarPausar = new JToggleButton("Iniciar");
            JButton botaoPasso = new JButton("Passo");
            JButton botaoLimpar = new JButton("Limpar");
            JButton botaoAleatorio = new JButton("Aleatório");
            JSlider controleVelocidade = new JSlider(50, 500, 200); // de 50ms até 500ms
            controleVelocidade.setInverted(true);
            controleVelocidade.setToolTipText("Velocidade (ms)");

            // Timer para rodar automaticamente
            Timer temporizador = new Timer(controleVelocidade.getValue(), e -> painel.proximoPasso());

            // Alterar velocidade do timer com o slider
            controleVelocidade.addChangeListener(e -> temporizador.setDelay(controleVelocidade.getValue()));

            // Iniciar / Pausar execução automática
            botaoIniciarPausar.addActionListener(e -> {
                if (botaoIniciarPausar.isSelected()) {
                    botaoIniciarPausar.setText("Pausar");
                    temporizador.start();
                } else {
                    botaoIniciarPausar.setText("Iniciar");
                    temporizador.stop();
                }
            });

            // Executar apenas um passo
            botaoPasso.addActionListener(e -> {
                if (!temporizador.isRunning()) painel.proximoPasso();
            });

            // Limpar grade
            botaoLimpar.addActionListener(e -> {
                painel.limpar();
                painel.repaint();
            });

            // Preencher aleatoriamente
            botaoAleatorio.addActionListener(e -> {
                painel.aleatorizar();
                painel.repaint();
            });

            // Painel inferior com os controles
            JPanel controles = new JPanel();
            controles.add(botaoIniciarPausar);
            controles.add(botaoPasso);
            controles.add(botaoLimpar);
            controles.add(botaoAleatorio);
            controles.add(new JLabel("Velocidade:"));
            controles.add(controleVelocidade);

            frame.add(controles, BorderLayout.SOUTH);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

// Classe responsável pela lógica e desenho do jogo
class PainelVida extends JPanel {
    private boolean[][] grade;   // grade principal
    private boolean[][] buffer;  // buffer para próxima geração
    private final int colunas, linhas;
    private final int tamanhoCelula;

    // Regras do autômato (até 8 vizinhos)
    private final boolean[] nascer = new boolean[9];
    private final boolean[] sobreviver = new boolean[9];

    public PainelVida(int colunas, int linhas, int tamanhoCelula) {
        this.colunas = colunas;
        this.linhas = linhas;
        this.tamanhoCelula = tamanhoCelula;

        setPreferredSize(new Dimension(colunas * tamanhoCelula, linhas * tamanhoCelula));
        setBackground(Color.WHITE);

        grade = new boolean[linhas][colunas];
        buffer = new boolean[linhas][colunas];

        // Regras padrão: nascer com 3 ou 6 vizinhos, sobreviver com 2 ou 3
        nascer[3] = true;
        nascer[6] = true;
        sobreviver[2] = true;
        sobreviver[3] = true;

        // Clique do mouse ativa/desativa célula
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int c = e.getX() / tamanhoCelula;
                int l = e.getY() / tamanhoCelula;
                if (estaDentroGrade(l, c)) {
                    grade[l][c] = !grade[l][c];
                    repaint();
                }
            }
        });

        // Barra de espaço = próximo passo
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    proximoPasso();
                }
            }
        });

        // Inicializa aleatoriamente
        aleatorizar();
    }

    // Verifica se célula está dentro da grade
    private boolean estaDentroGrade(int l, int c) {
        return l >= 0 && l < linhas && c >= 0 && c < colunas;
    }

    // Calcula próxima geração
    public void proximoPasso() {
        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                int vizinhos = contarVizinhosMoore(l, c);
                if (grade[l][c]) {
                    buffer[l][c] = sobreviver[vizinhos];
                } else {
                    buffer[l][c] = nascer[vizinhos];
                }
            }
        }

        // Troca referências (swap)
        boolean[][] tmp = grade;
        grade = buffer;
        buffer = tmp;

        repaint();
    }

    // Conta vizinhos de Moore (8 vizinhos possíveis)
    private int contarVizinhosMoore(int l, int c) {
        int count = 0;
        for (int dl = -1; dl <= 1; dl++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dl == 0 && dc == 0) continue; // ignora a própria célula
                int nl = l + dl;
                int nc = c + dc;
                if (nl >= 0 && nl < linhas && nc >= 0 && nc < colunas) {
                    if (grade[nl][nc]) count++;
                }
            }
        }
        return count;
    }

    // Limpar grade
    public void limpar() {
        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                grade[l][c] = false;
            }
        }
    }

    // Preencher aleatoriamente
    public void aleatorizar() {
        Random rnd = new Random();
        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                grade[l][c] = rnd.nextDouble() < 0.18; // ~18% de chance de estar viva
            }
        }
    }

    // Desenhar células na tela
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                if (grade[l][c]) {
                    g.setColor(Color.BLACK);
                    g.fillRect(c * tamanhoCelula, l * tamanhoCelula, tamanhoCelula, tamanhoCelula);
                }
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(c * tamanhoCelula, l * tamanhoCelula, tamanhoCelula, tamanhoCelula);
            }
        }
    }
}

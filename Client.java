package progetto_demi_67;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private Socket socket;
    private PrintStream out;
    private BufferedReader in;

    private JTextArea areaRisposte;
    private JTextField campoImporto;
    private JButton btnSaldo, btnDeposita, btnPreleva, btnEsci;

    // Aggiunto un parametro booleano per sapere se è una registrazione
    public Client(String ip, int port, String nome, String pin, boolean isNuovoAccount) {
        setTitle(isNuovoAccount ? "Client Bancomat - Nuovo Account" : "Client Bancomat");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        areaRisposte = new JTextArea();
        areaRisposte.setEditable(false);
        areaRisposte.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(areaRisposte);
        add(scrollPane, BorderLayout.CENTER);

        JPanel pannelloComandi = new JPanel();
        btnSaldo = new JButton("Saldo");
        btnDeposita = new JButton("Deposita");
        btnPreleva = new JButton("Preleva");
        btnEsci = new JButton("Esci");
        campoImporto = new JTextField(8);

        pannelloComandi.add(btnSaldo);
        pannelloComandi.add(new JLabel("Importo €:"));
        pannelloComandi.add(campoImporto);
        pannelloComandi.add(btnDeposita);
        pannelloComandi.add(btnPreleva);
        pannelloComandi.add(btnEsci);
        add(pannelloComandi, BorderLayout.SOUTH);

        btnSaldo.addActionListener(e -> inviaComandoAlServer("SALDO"));
        btnDeposita.addActionListener(e -> {
            String importo = campoImporto.getText().trim();
            if (!importo.isEmpty()) inviaComandoAlServer("DEPOSITA " + importo);
        });
        btnPreleva.addActionListener(e -> {
            String importo = campoImporto.getText().trim();
            if (!importo.isEmpty()) inviaComandoAlServer("PRELEVA " + importo);
        });
        btnEsci.addActionListener(e -> {
            inviaComandoAlServer("ESCI");
            System.exit(0);
        });

        // Passiamo anche l'intenzione (isNuovoAccount)
        avviaConnessione(ip, port, nome, pin, isNuovoAccount);
    }

    private void avviaConnessione(String ip, int port, String nome, String pin, boolean isNuovoAccount) {
        try {
            socket = new Socket(ip, port);
            out = new PrintStream(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 1. Il server chiede "Inserisci il NOME o NUOVO"
            scriviSuSchermo(in.readLine());

            if (isNuovoAccount) {
                // Comunichiamo al server che vogliamo registrarci
                out.println("NUOVO");
                
                // Il server chiede il nuovo nome
                scriviSuSchermo(in.readLine());
                out.println(nome);
                
                // Il server chiede il nuovo PIN
                scriviSuSchermo(in.readLine());
                out.println(pin);
            } else {
                // Accesso normale
                out.println(nome);
                
                // Il server chiede il PIN
                scriviSuSchermo(in.readLine());
                out.println(pin);
            }

            // Avvio del Thread per l'ascolto continuo
            new Thread(() -> {
                try {
                    String rispostaServer;
                    while ((rispostaServer = in.readLine()) != null) {
                        scriviSuSchermo(rispostaServer);
                    }
                } catch (IOException ex) {
                    scriviSuSchermo("--- DISCONNESSO DAL SERVER ---");
                    disabilitaBottoni();
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Impossibile connettersi: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void inviaComandoAlServer(String comando) {
        if (out != null) {
            scriviSuSchermo("\nTu > " + comando);
            out.println(comando);
            campoImporto.setText("");
        }
    }

    private void scriviSuSchermo(String messaggio) {
        SwingUtilities.invokeLater(() -> {
            areaRisposte.append(messaggio + "\n");
            areaRisposte.setCaretPosition(areaRisposte.getDocument().getLength());
        });
    }

    private void disabilitaBottoni() {
        btnSaldo.setEnabled(false);
        btnDeposita.setEnabled(false);
        btnPreleva.setEnabled(false);
        campoImporto.setEnabled(false);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        // --- MENU INIZIALE ---
        String[] opzioni = {"Accedi", "Registra Nuovo Account"};
        int scelta = JOptionPane.showOptionDialog(null, "Cosa vuoi fare?", "Benvenuto nella Banca",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opzioni, opzioni[0]);

        if (scelta == JOptionPane.CLOSED_OPTION) return;

        boolean isNuovoAccount = (scelta == 1);

        String ip = JOptionPane.showInputDialog("Inserisci IP Server:", "127.0.0.1");
        if (ip == null || ip.trim().isEmpty()) return;

        String portStr = JOptionPane.showInputDialog("Inserisci Porta:", "8080");
        if (portStr == null || portStr.trim().isEmpty()) return;

        String nome = JOptionPane.showInputDialog(isNuovoAccount ? "Scegli un NOME per il nuovo account:" : "Inserisci il tuo NOME:");
        if (nome == null || nome.trim().isEmpty()) return;

        JPasswordField pinField = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, pinField, isNuovoAccount ? "Scegli un PIN (es. 12345):" : "Inserisci il tuo PIN:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (okCxl != JOptionPane.OK_OPTION) return;
        String pin = new String(pinField.getPassword());
        
        try {
            int port = Integer.parseInt(portStr);
            SwingUtilities.invokeLater(() -> {
                Client client = new Client(ip, port, nome, pin, isNuovoAccount);
                client.setVisible(true);
            });
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Porta non valida.");
        }
    }
}


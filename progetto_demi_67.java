package progetto_demi_67;
import java.net.*;
import java.io.*;
import java.net.*;
import java.io.*;

public class progetto_demi_67 {

    // Nome del file che funge da database
    private static final String NOME_FILE = "database.txt";
    public static String creaIban(String file) {
    	String nuovoIban = "IT" + (int)(Math.random() * 90000) + 10000;
        boolean giusto=true;
        try (BufferedReader readerFile = new BufferedReader(new FileReader(NOME_FILE))) {
            String linea;
            while ((linea = readerFile.readLine()) != null) {
                String[] dati = linea.split(",");
                if (dati.length == 4) {
                    String iban = dati[2].trim();
                    if(nuovoIban.equals(iban)) {giusto=false; break;} 	
                }
            }
            if(giusto)
            	return nuovoIban;
            else 
            	return creaIban(file);
        } catch (IOException | NumberFormatException e) {
        	return ("Errore lettura database: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader tastiera = new BufferedReader(input);
        ServerSocket connessione = null;
        Socket socket = null;
        int i = 1;

        System.out.println("Inserisci la porta di ascolto:");

        try {
            String port = tastiera.readLine();
            System.out.println("In attesa di connessioni su port " + port + "...");
            connessione = new ServerSocket(Integer.parseInt(port));
        } catch (IOException ex) {
            System.out.println("Errore nell'avvio del server: " + ex);
            System.exit(0);
        }
        while (true) {
            try {
                socket = connessione.accept();
                System.out.println("Connessione da " + socket.getInetAddress().toString());

                BufferedReader inDalClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream outVersoClient = new PrintStream(socket.getOutputStream());

                outVersoClient.println("");
                String nomeRicevuto = inDalClient.readLine();

                ContoDati conto = null;

                // --- GESTIONE NUOVO ACCOUNT ---
                if ("NUOVO".equalsIgnoreCase(nomeRicevuto)) {
                    outVersoClient.println("");
                    String nuovoNome = inDalClient.readLine();

                    outVersoClient.println("");
                    String nuovoPin = inDalClient.readLine();

                    // Genera un IBAN finto casuale
                    String nuovoIban=creaIban(NOME_FILE);
                    
                    // Salva sul file database.txt
                    try (FileWriter fw = new FileWriter(NOME_FILE, true);
                         BufferedWriter bw = new BufferedWriter(fw)) {
                        bw.write(nuovoNome + "," + nuovoPin + "," + nuovoIban + ",0.0");
                        bw.newLine();
                    }

                    conto = new ContoDati(nuovoNome, nuovoPin, nuovoIban, 0.0);
                    System.out.println("Nuovo account creato per " + nuovoNome);
                } 
                // --- GESTIONE LOGIN NORMALE ---
                else {
                    outVersoClient.println("");
                    String pinRicevuto = inDalClient.readLine();
                    conto = eseguiLogin(nomeRicevuto, pinRicevuto, NOME_FILE);
                }

                // --- AVVIO DEL THREAD SE L'UTENTE E' VALIDO ---
                if (conto != null) {
                    outVersoClient.println("Accesso effettuato! Benvenuto " + conto.nome + ". IBAN: " + conto.iban);
                    Figlio f = new Figlio(socket, i, inDalClient, outVersoClient, conto);
                    f.start();
                    i++;
                } else {
                    outVersoClient.println("Credenziali errate. Connessione chiusa.");
                    socket.close();
                }

            } catch (IOException ex) {
                System.out.println("Errore: " + ex);
            }
        }
        }
    

    /**
     * Legge il file e restituisce un oggetto ContoDati se le credenziali sono corrette
     */
    private static ContoDati eseguiLogin(String nomeUtente, String pinUtente, String file) {
        if (nomeUtente == null || pinUtente == null) return null;

        try (BufferedReader readerFile = new BufferedReader(new FileReader(file))) {
            String linea;
            while ((linea = readerFile.readLine()) != null) {
                String[] dati = linea.split(",");
                if (dati.length == 4) {
                    String nomeSalvato = dati[0].trim();
                    String pinSalvato = dati[1].trim();
                    String iban = dati[2].trim();
                    double saldo = Double.parseDouble(dati[3].trim());

                    if (nomeSalvato.equalsIgnoreCase(nomeUtente.trim()) && pinSalvato.equals(pinUtente.trim())) {
                        return new ContoDati(nomeSalvato, pinSalvato, iban, saldo);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Errore lettura database: " + e.getMessage());
        }
        return null;
    }

    /**
     * Aggiorna il file di testo. 
     * È 'synchronized' per evitare che due thread scrivano sul file contemporaneamente.
     */
    public static synchronized void aggiornaDatabase(ContoDati contoAggiornato) {
        File fileOriginale = new File(NOME_FILE);
        File fileTemp = new File("temp.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(fileOriginale));
             BufferedWriter bw = new BufferedWriter(new FileWriter(fileTemp))) {
             
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] dati = linea.split(",");
                if (dati.length == 4 && dati[0].trim().equalsIgnoreCase(contoAggiornato.nome)) {
                    // Riscrive la riga con il NUOVO saldo
                    bw.write(contoAggiornato.nome + "," + contoAggiornato.pin + "," + 
                             contoAggiornato.iban + "," + contoAggiornato.saldo);
                } else {
                    // Ricopia le righe degli altri utenti identiche a prima
                    bw.write(linea);
                }
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Errore nell'aggiornamento del file: " + e.getMessage());
        }
        
        // Sostituisce il vecchio file con quello nuovo aggiornato
        if (fileOriginale.delete()) {
            fileTemp.renameTo(fileOriginale);
        }
    }
}

// ---------------------------------------------------------
// CLASSI DI SUPPORTO (Devono stare fuori dal main!)
// ---------------------------------------------------------

// Classe di supporto per trasportare i dati del conto
class ContoDati {
    String nome;
    String pin;
    String iban;
    double saldo;

    public ContoDati(String nome, String pin, String iban, double saldo) {
        this.nome = nome;
        this.pin = pin;
        this.iban = iban;
        this.saldo = saldo;
    }
}


class Figlio extends Thread {
    Socket socket;
    int i;
    String ip_mittente;
    BufferedReader datin;
    PrintStream out;
    ContoDati conto; 
    boolean connesso = true;

    // Costruttore con i 5 parametri corretti
    public Figlio(Socket socket, int i, BufferedReader datin, PrintStream out, ContoDati conto) {
        this.socket = socket;
        this.i = i;
        this.datin = datin;
        this.out = out;
        this.conto = conto;
        this.ip_mittente = socket.getInetAddress().toString();
    }

    public void run() {
        out.println("Comandi disponibili: SALDO, DEPOSITA <importo>, PRELEVA <importo>, ESCI");

        while (connesso) {
            try {
                String arrivata = datin.readLine();
                if (arrivata != null) {
                    System.out.println(conto.nome + " (" + ip_mittente + ") > " + arrivata);
                    gestisciComando(arrivata);
                } else {
                    connesso = false;
                }
            } catch (IOException e) {
                System.out.println("Disconnesso: " + ip_mittente);
                connesso = false;
            }
        }
        
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Errore nella chiusura: " + e);
        }
    }

    private void gestisciComando(String comando) {
        String[] parti = comando.toUpperCase().split(" ");
        String azione = parti[0];

        try {
            switch (azione) {
                case "SALDO":
                    out.println("Il tuo saldo attuale è: " + conto.saldo + "€");
                    break;
                case "DEPOSITA":
                    double importoDep = Double.parseDouble(parti[1]);
                    conto.saldo += importoDep;
                    progetto_demi_67.aggiornaDatabase(conto);
                    out.println("Depositati " + importoDep + "€. Nuovo saldo: " + conto.saldo + "€");
                    break;
                case "PRELEVA":
                    double importoPrel = Double.parseDouble(parti[1]);
                    if (conto.saldo >= importoPrel) {
                        conto.saldo -= importoPrel;
                        progetto_demi_67.aggiornaDatabase(conto);
                        out.println("Prelevati " + importoPrel + "€. Nuovo saldo: " + conto.saldo + "€");
                    } else {
                        out.println("Errore: Fondi insufficienti.");
                    }
                    break;
                case "ESCI":
                    out.println("Arrivederci!");
                    connesso = false;
                    break;
                default:
                    out.println("Comando non riconosciuto.");
            }
        } catch (Exception e) {
            out.println("Errore di sintassi. Esempio corretto: DEPOSITA 50");
        }
    }
}

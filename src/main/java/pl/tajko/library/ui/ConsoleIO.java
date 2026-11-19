package pl.tajko.library.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Cienka warstwa nad konsolą: wczytywanie i wypisywanie tekstu.
 * <p>
 * Menu nie korzystają bezpośrednio z {@code System.in} i {@code System.out}, tylko z tej klasy.
 * Dzięki temu w testach można podać "sztuczne" wejście (bajty z tekstu) i sprawdzić wszystko,
 * co program wypisał.
 * <p>
 * Teksty zachęty (prompt) powinny same kończyć się spacją, np. {@code "Wybór: "}.
 */
public class ConsoleIO {

    /** Czytnik linii wejścia. */
    private final BufferedReader reader;

    /** Strumień wyjścia. */
    private final PrintStream out;

    /**
     * Tworzy obiekt czytający wejście w kodowaniu UTF-8.
     *
     * @param in  strumień wejściowy, nie może być {@code null}
     * @param out strumień wyjściowy, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś strumień jest {@code null}
     */
    public ConsoleIO(InputStream in, PrintStream out) {
        this(in, out, StandardCharsets.UTF_8);
    }

    /**
     * Tworzy obiekt czytający wejście w podanym kodowaniu.
     *
     * @param in           strumień wejściowy, nie może być {@code null}
     * @param out          strumień wyjściowy, nie może być {@code null}
     * @param inputCharset kodowanie znaków wejścia, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś argument jest {@code null}
     */
    public ConsoleIO(InputStream in, PrintStream out, Charset inputCharset) {
        if (in == null || out == null || inputCharset == null) {
            throw new IllegalArgumentException("Wejście, wyjście i kodowanie nie mogą być puste");
        }
        this.reader = new BufferedReader(new InputStreamReader(in, inputCharset));
        this.out = out;
    }

    /**
     * Wypisuje tekst i przechodzi do nowej linii.
     *
     * @param text tekst do wypisania
     */
    public void println(String text) {
        out.println(text);
    }

    /**
     * Wypisuje pustą linię.
     */
    public void println() {
        out.println();
    }

    /**
     * Wypisuje tekst sformatowany jak w {@link String#format(String, Object...)}.
     *
     * @param format wzorzec formatowania
     * @param args   argumenty wzorca
     */
    public void printf(String format, Object... args) {
        out.printf(format, args);
    }

    /**
     * Wypisuje komunikat o błędzie w jednej linii, z przedrostkiem "Błąd: ".
     *
     * @param message treść komunikatu
     */
    public void printError(String message) {
        out.println("Błąd: " + message);
    }

    /**
     * Wypisuje zachętę i wczytuje jedną linię. Zwrócony tekst ma obcięte spacje z obu stron
     * i może być pusty (gdy użytkownik wcisnął tylko Enter).
     *
     * @param prompt tekst zachęty (bez nowej linii)
     * @return wczytana linia bez spacji na brzegach
     * @throws InputClosedException jeśli wejście się skończyło
     * @throws UncheckedIOException jeśli odczyt się nie powiódł
     */
    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Nie można odczytać danych z konsoli", e);
        }
    }

    /**
     * Wczytuje liczbę całkowitą z podanego zakresu. Przy niepoprawnym wpisie wypisuje błąd
     * i pyta ponownie.
     *
     * @param prompt tekst zachęty
     * @param min    najmniejsza dozwolona wartość (włącznie)
     * @param max    największa dozwolona wartość (włącznie)
     * @return liczba z zakresu {@code min..max}
     * @throws IllegalArgumentException jeśli {@code min} jest większe od {@code max}
     * @throws InputClosedException     jeśli wejście się skończyło
     */
    public int readInt(String prompt, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Dolna granica zakresu jest większa od górnej");
        }
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // to nie jest liczba, więc przechodzimy do komunikatu poniżej
            }
            printError("Podaj liczbę całkowitą od " + min + " do " + max + ".");
        }
    }

    /**
     * Zadaje pytanie tak/nie i powtarza je do skutku. Akceptuje "t", "tak", "n", "nie"
     * (wielkość liter nie ma znaczenia).
     *
     * @param question treść pytania (bez znaku zapytania na końcu nie jest wymagane)
     * @return {@code true} dla odpowiedzi twierdzącej, {@code false} dla przeczącej
     * @throws InputClosedException jeśli wejście się skończyło
     */
    public boolean confirm(String question) {
        while (true) {
            String answer = readLine(question + " (t/n): ").toLowerCase(Locale.ROOT);
            if (answer.equals("t") || answer.equals("tak")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("nie")) {
                return false;
            }
            printError("Wpisz t (tak) albo n (nie).");
        }
    }
}
package pl.tajko.library.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Pomocnik testów menu: konsola czytająca z gotowego skryptu odpowiedzi i zbierająca wszystko, co wypisze.
 */
final class TestConsole {

    /** Bufor na wszystko, co wypisała konsola. */
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /** Konsola do przekazania testowanemu menu. */
    private final ConsoleIO io;

    /**
     * Tworzy konsolę z gotowym skryptem odpowiedzi.
     *
     * @param script odpowiedzi użytkownika, każda zakończona znakiem nowej linii
     */
    TestConsole(String script) {
        this.io = new ConsoleIO(new ByteArrayInputStream(script.getBytes(UTF_8)), new PrintStream(buffer, true, UTF_8));
    }

    /**
     * Zwraca konsolę do przekazania testowanemu menu.
     *
     * @return konsola
     */
    ConsoleIO io() {
        return io;
    }

    /**
     * Zwraca wszystko, co dotąd wypisała konsola.
     *
     * @return wypisany tekst
     */
    String output() {
        return buffer.toString(UTF_8);
    }

    /**
     * Zlicza wystąpienia fragmentu w wypisanym tekście.
     *
     * @param fragment szukany fragment
     * @return liczba nienakładających się wystąpień
     */
    int count(String fragment) {
        String text = output();
        int count = 0;
        int index = text.indexOf(fragment);
        while (index >= 0) {
            count++;
            index = text.indexOf(fragment, index + fragment.length());
        }
        return count;
    }
}
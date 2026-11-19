package pl.tajko.library.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy jednostkowe klasy {@link ConsoleIO}.
 * <p>
 * Wejście to bajty z tekstu, a wyjście zbiera się do bufora, więc test widzi dokładnie to,
 * co zobaczyłby użytkownik.
 */
@DisplayName("ConsoleIO")
class ConsoleIOTest {

    /** Bufor, do którego trafia wszystko, co wypisze testowana konsola. */
    private ByteArrayOutputStream output;

    /**
     * Tworzy konsolę czytającą z podanego tekstu.
     *
     * @param input tekst, który "wpisze" użytkownik (linie rozdzielone znakiem \n)
     * @return konsola do testu
     */
    private ConsoleIO io(String input) {
        output = new ByteArrayOutputStream();
        return new ConsoleIO(new ByteArrayInputStream(input.getBytes(UTF_8)), new PrintStream(output, true, UTF_8));
    }

    /**
     * Zwraca wszystko, co dotąd wypisała konsola.
     *
     * @return wypisany tekst
     */
    private String printed() {
        return output.toString(UTF_8);
    }

    /**
     * Zlicza komunikaty o błędach w wypisanym tekście.
     *
     * @return liczba wystąpień przedrostka "Błąd:"
     */
    private int errorCount() {
        return printed().split("Błąd:", -1).length - 1;
    }

    /**
     * Konstruktor powinien odrzucać puste argumenty.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste argumenty")
    void shouldRejectNullArguments() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        PrintStream out = new PrintStream(new ByteArrayOutputStream());
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new ConsoleIO(null, out)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ConsoleIO(in, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new ConsoleIO(in, out, null))
        );
    }

    /**
     * Testy wczytywania linii.
     */
    @Nested
    @DisplayName("readLine")
    class ReadLineTests {

        /**
         * Zachęta powinna być wypisana, a odpowiedź pozbawiona spacji na brzegach.
         */
        @Test
        @DisplayName("wypisuje zachętę i zwraca linię bez spacji na brzegach")
        void shouldPrintPromptAndTrim() {
            ConsoleIO io = io("  Jan Kowalski  \n");

            String line = io.readLine("Imię: ");

            assertAll(
                    () -> assertEquals("Jan Kowalski", line),
                    () -> assertEquals("Imię: ", printed())
            );
        }

        /**
         * Polskie znaki powinny przejść bez zmian.
         */
        @Test
        @DisplayName("zachowuje polskie znaki")
        void shouldKeepPolishCharacters() {
            assertEquals("Żółć", io("Żółć\n").readLine("> "));
        }

        /**
         * Sam Enter powinien dać pusty tekst.
         */
        @Test
        @DisplayName("zwraca pusty tekst dla pustej linii")
        void shouldReturnEmptyStringForEmptyLine() {
            assertEquals("", io("\n").readLine("> "));
        }

        /**
         * Kolejne wywołania powinny czytać kolejne linie.
         */
        @Test
        @DisplayName("czyta kolejne linie po kolei")
        void shouldReadLinesInOrder() {
            ConsoleIO io = io("pierwsza\ndruga\n");

            assertAll(
                    () -> assertEquals("pierwsza", io.readLine("> ")),
                    () -> assertEquals("druga", io.readLine("> "))
            );
        }

        /**
         * Gdy dane się skończą, powinien polecieć wyjątek zamiast pętli w nieskończoność.
         */
        @Test
        @DisplayName("zgłasza InputClosedException na końcu wejścia")
        void shouldFailAtEndOfInput() {
            ConsoleIO io = io("");

            assertThrows(InputClosedException.class, () -> io.readLine("> "));
        }
    }

    /**
     * Testy wczytywania liczb.
     */
    @Nested
    @DisplayName("readInt")
    class ReadIntTests {

        /**
         * Poprawna liczba z zakresu powinna zostać zwrócona od razu.
         */
        @Test
        @DisplayName("zwraca liczbę z zakresu")
        void shouldReadNumberInRange() {
            ConsoleIO io = io("3\n");

            assertAll(
                    () -> assertEquals(3, io.readInt("Wybór: ", 0, 5)),
                    () -> assertEquals(0, errorCount())
            );
        }

        /**
         * Granice zakresu są dozwolone.
         *
         * @param value wartość na granicy zakresu
         */
        @ParameterizedTest(name = "akceptuje granicę: [{0}]")
        @ValueSource(strings = {"0", "5"})
        void shouldAcceptBoundaries(String value) {
            assertEquals(Integer.parseInt(value), io(value + "\n").readInt("Wybór: ", 0, 5));
        }

        /**
         * Niepoprawne wpisy powinny dawać błąd i ponowne pytanie, aż do poprawnego.
         */
        @Test
        @DisplayName("powtarza pytanie po tekście, liczbie spoza zakresu i pustej linii")
        void shouldAskAgainAfterInvalidInput() {
            ConsoleIO io = io("abc\n9\n-1\n\n2\n");

            int result = io.readInt("Wybór: ", 0, 5);

            assertAll(
                    () -> assertEquals(2, result),
                    () -> assertEquals(4, errorCount()),
                    () -> assertTrue(printed().contains("od 0 do 5"))
            );
        }

        /**
         * Odwrócony zakres to błąd programisty.
         */
        @Test
        @DisplayName("odrzuca odwrócony zakres")
        void shouldRejectInvertedRange() {
            ConsoleIO io = io("1\n");

            assertThrows(IllegalArgumentException.class, () -> io.readInt("Wybór: ", 5, 0));
        }

        /**
         * Koniec wejścia w trakcie pytania powinien przerwać pętlę.
         */
        @Test
        @DisplayName("zgłasza InputClosedException, gdy wejście się skończy")
        void shouldFailAtEndOfInput() {
            ConsoleIO io = io("abc\n");

            assertThrows(InputClosedException.class, () -> io.readInt("Wybór: ", 0, 5));
        }
    }

    /**
     * Testy pytań tak/nie.
     */
    @Nested
    @DisplayName("confirm")
    class ConfirmTests {

        /**
         * Odpowiedzi twierdzące.
         *
         * @param answer wpisana odpowiedź
         */
        @ParameterizedTest(name = "[{0}] oznacza tak")
        @ValueSource(strings = {"t", "T", "tak", "TAK", " tak "})
        void shouldAcceptYes(String answer) {
            assertTrue(io(answer + "\n").confirm("Usunąć?"));
        }

        /**
         * Odpowiedzi przeczące.
         *
         * @param answer wpisana odpowiedź
         */
        @ParameterizedTest(name = "[{0}] oznacza nie")
        @ValueSource(strings = {"n", "N", "nie", "NIE"})
        void shouldAcceptNo(String answer) {
            assertFalse(io(answer + "\n").confirm("Usunąć?"));
        }

        /**
         * Inne odpowiedzi powinny wywołać błąd i ponowne pytanie.
         */
        @Test
        @DisplayName("powtarza pytanie po niezrozumiałej odpowiedzi")
        void shouldAskAgainAfterInvalidAnswer() {
            ConsoleIO io = io("może\n\ntak\n");

            boolean result = io.confirm("Usunąć?");

            assertAll(
                    () -> assertTrue(result),
                    () -> assertEquals(2, errorCount()),
                    () -> assertTrue(printed().startsWith("Usunąć? (t/n): "))
            );
        }
    }

    /**
     * Testy wypisywania.
     */
    @Nested
    @DisplayName("wypisywanie")
    class PrintTests {

        /**
         * Metody wypisujące powinny dawać oczekiwany tekst.
         */
        @Test
        @DisplayName("println, printf i printError wypisują oczekiwany tekst")
        void shouldPrintText() {
            ConsoleIO io = io("");

            io.println("Linia");
            io.println();
            io.printf("%d. %s%n", 1, "Czysty kod");
            io.printError("coś poszło nie tak");

            String nl = System.lineSeparator();
            assertEquals("Linia" + nl + nl + "1. Czysty kod" + nl + "Błąd: coś poszło nie tak" + nl, printed());
        }
    }

    /**
     * Testy wczytywania z wartością domyślną.
     */
    @Nested
    @DisplayName("wartości domyślne")
    class DefaultValueTests {

        /**
         * Sam Enter powinien zwrócić dotychczasową wartość, a zachęta pokazywać ją w nawiasie.
         */
        @Test
        @DisplayName("readLineOrDefault: Enter zostawia wartość domyślną")
        void shouldKeepDefaultText() {
            ConsoleIO io = io("\n");

            assertAll(
                    () -> assertEquals("Stary tytuł", io.readLineOrDefault("Tytuł", "Stary tytuł")),
                    () -> assertEquals("Tytuł [Stary tytuł]: ", printed())
            );
        }

        /**
         * Wpisany tekst powinien zastąpić wartość domyślną.
         */
        @Test
        @DisplayName("readLineOrDefault: wpisany tekst zastępuje wartość domyślną")
        void shouldReplaceDefaultText() {
            assertEquals("Nowy", io("  Nowy  \n").readLineOrDefault("Tytuł", "Stary"));
        }

        /**
         * Sam Enter powinien zwrócić dotychczasową liczbę.
         */
        @Test
        @DisplayName("readIntOrDefault: Enter zostawia wartość domyślną")
        void shouldKeepDefaultNumber() {
            assertEquals(2008, io("\n").readIntOrDefault("Rok", 1450, 2026, 2008));
        }

        /**
         * Wpisana poprawna liczba powinna zastąpić wartość domyślną.
         */
        @Test
        @DisplayName("readIntOrDefault: wpisana liczba zastępuje wartość domyślną")
        void shouldReplaceDefaultNumber() {
            assertEquals(1999, io("1999\n").readIntOrDefault("Rok", 1450, 2026, 2008));
        }

        /**
         * Niepoprawne wpisy powinny dać błąd i ponowne pytanie.
         */
        @Test
        @DisplayName("readIntOrDefault: powtarza pytanie po niepoprawnym wpisie")
        void shouldAskAgainAfterInvalidNumber() {
            ConsoleIO io = io("abc\n3000\n1999\n");

            int result = io.readIntOrDefault("Rok", 1450, 2026, 2008);

            assertAll(
                    () -> assertEquals(1999, result),
                    () -> assertEquals(2, errorCount())
            );
        }

        /**
         * Odwrócony zakres to błąd programisty.
         */
        @Test
        @DisplayName("readIntOrDefault: odrzuca odwrócony zakres")
        void shouldRejectInvertedRange() {
            ConsoleIO io = io("\n");

            assertThrows(IllegalArgumentException.class, () -> io.readIntOrDefault("Rok", 10, 1, 5));
        }
    }
}
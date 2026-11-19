package pl.tajko.library.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.tajko.library.LibraryContext;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy menu głównego.
 */
@DisplayName("MainMenu")
class MainMenuTest {

    /** Stały zegar używany w testach. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Folder tymczasowy na pliki danych. */
    @TempDir
    Path tempDir;

    /** Kontekst aplikacji używany w teście. */
    private LibraryContext context;

    /**
     * Tworzy świeży kontekst z pustymi danymi przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        context = new LibraryContext(tempDir, CLOCK);
    }

    /**
     * Uruchamia menu główne z gotowym skryptem odpowiedzi.
     *
     * @param script odpowiedzi użytkownika
     * @return konsola testowa z wypisanym tekstem
     */
    private TestConsole run(String script) {
        TestConsole console = new TestConsole(script);
        new MainMenu(console.io(),
                new BookMenu(console.io(), context.bookService()),
                new MemberMenu(console.io(), context.memberService())).run();
        return console;
    }

    /**
     * Wybór 0 powinien pożegnać użytkownika i zakończyć menu.
     */
    @Test
    @DisplayName("wyjście kończy program z pożegnaniem")
    void shouldExit() {
        assertTrue(run("0\n").output().contains("Do zobaczenia!"));
    }

    /**
     * Wybór 1 powinien otworzyć menu katalogu, a powrót wrócić do menu głównego.
     */
    @Test
    @DisplayName("otwiera menu katalogu książek i wraca")
    void shouldOpenBookMenu() {
        TestConsole console = run("1\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("--- Katalog książek ---")),
                () -> assertEquals(2, console.count("=== Libris: menu główne ==="))
        );
    }

    /**
     * Wybór 2 powinien otworzyć menu czytelników.
     */
    @Test
    @DisplayName("otwiera menu czytelników i wraca")
    void shouldOpenMemberMenu() {
        TestConsole console = run("2\n0\n0\n");

        assertTrue(console.output().contains("--- Czytelnicy ---"));
    }

    /**
     * Niepoprawny wybór powinien dać komunikat i ponowne pytanie.
     */
    @Test
    @DisplayName("niepoprawny wybór daje komunikat i menu działa dalej")
    void shouldRejectInvalidChoice() {
        TestConsole console = run("9\nabc\n0\n");

        assertAll(
                () -> assertEquals(2, console.count("Błąd:")),
                () -> assertTrue(console.output().contains("Do zobaczenia!"))
        );
    }

    /**
     * Koniec danych wejściowych powinien przerwać menu wyjątkiem, a nie pętlą bez końca.
     */
    @Test
    @DisplayName("koniec wejścia przerywa menu")
    void shouldStopAtEndOfInput() {
        assertThrows(InputClosedException.class, () -> run(""));
    }

    /**
     * Menu powinno odrzucać puste zależności.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste zależności")
    void shouldRejectNullDependencies() {
        TestConsole console = new TestConsole("");
        BookMenu bookMenu = new BookMenu(console.io(), context.bookService());
        MemberMenu memberMenu = new MemberMenu(console.io(), context.memberService());

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new MainMenu(null, bookMenu, memberMenu)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MainMenu(console.io(), null, memberMenu)),
                () -> assertThrows(IllegalArgumentException.class, () -> new MainMenu(console.io(), bookMenu, null))
        );
    }
}
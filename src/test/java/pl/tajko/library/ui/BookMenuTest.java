package pl.tajko.library.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.tajko.library.LibraryContext;
import pl.tajko.library.model.Genre;
import pl.tajko.library.model.Member;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy menu katalogu książek na prawdziwych serwisach i plikach w folderze tymczasowym.
 * Każdy test "wpisuje" gotowy skrypt odpowiedzi, a następnie sprawdza wypisany tekst.
 */
@DisplayName("BookMenu")
class BookMenuTest {

    /** Stały zegar używany w testach. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Poprawny numer ISBN-13 pierwszej książki. */
    private static final String ISBN_CLEAN_CODE = "9780132350884";

    /** Skrypt dodania "Czysty kod" w 2 egzemplarzach: wybór 3, tytuł, autor, rok, ISBN, gatunek 1, kopie 2. */
    private static final String ADD_CLEAN_CODE =
            "3\nCzysty kod\nRobert C. Martin\n2008\n" + ISBN_CLEAN_CODE + "\n1\n2\n";

    /** Skrypt dodania "Algorytmy" w 1 egzemplarzu. */
    private static final String ADD_ALGORITHMS =
            "3\nAlgorytmy\nAutor Drugi\n2001\n9780306406157\n1\n1\n";

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
     * Uruchamia menu z gotowym skryptem odpowiedzi.
     *
     * @param script odpowiedzi użytkownika
     * @return konsola testowa z wypisanym tekstem
     */
    private TestConsole run(String script) {
        TestConsole console = new TestConsole(script);
        new BookMenu(console.io(), context.bookService()).run();
        return console;
    }

    /**
     * Pusty katalog powinien dać komunikat zamiast pustej tabeli.
     */
    @Test
    @DisplayName("pokazuje komunikat dla pustego katalogu")
    void shouldShowMessageForEmptyCatalog() {
        TestConsole console = run("1\n0\n");

        assertTrue(console.output().contains("Katalog jest pusty."));
    }

    /**
     * Dodana książka powinna pojawić się w katalogu z liczbą wolnych i wszystkich egzemplarzy.
     */
    @Test
    @DisplayName("dodaje książkę i pokazuje ją w katalogu")
    void shouldAddBookAndShowItInCatalog() {
        TestConsole console = run(ADD_CLEAN_CODE + "1\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Dodano 2 egz. książki \"Czysty kod\".")),
                () -> assertTrue(console.output().contains("Robert C. Martin")),
                () -> assertTrue(console.output().contains(ISBN_CLEAN_CODE)),
                () -> assertTrue(console.output().contains("2/2"))
        );
    }

    /**
     * Wybór pozycji z katalogu powinien pokazać egzemplarze ze stanem.
     */
    @Test
    @DisplayName("po wyborze pozycji pokazuje egzemplarze ze stanem")
    void shouldShowCopiesAfterSelectingTitle() {
        TestConsole console = run(ADD_CLEAN_CODE + "1\n1\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Egzemplarze:")),
                () -> assertEquals(2, console.count("wolny"))
        );
    }

    /**
     * Niepoprawny ISBN powinien dać komunikat błędu, nie zatrzymać menu i niczego nie dodać.
     */
    @Test
    @DisplayName("niepoprawny ISBN daje komunikat i niczego nie dodaje")
    void shouldReportInvalidIsbn() {
        TestConsole console = run("3\nCzysty kod\nRobert C. Martin\n2008\n123\n1\n1\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertTrue(console.output().contains("Katalog jest pusty."))
        );
    }

    /**
     * Dodanie egzemplarzy powinno zwiększyć ich liczbę w katalogu.
     */
    @Test
    @DisplayName("dodaje kolejne egzemplarze istniejącej książki")
    void shouldAddCopies() {
        TestConsole console = run(ADD_CLEAN_CODE + "4\n1\n3\n1\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Dodano 3 egz. książki \"Czysty kod\".")),
                () -> assertTrue(console.output().contains("5/5"))
        );
    }

    /**
     * Edycja z samym Enterem w polach powinna zmienić tylko to, co wpisano.
     */
    @Test
    @DisplayName("edytuje tytuł, a pozostałe pola zostawia bez zmian")
    void shouldEditOnlyTitle() {
        TestConsole console = run(ADD_CLEAN_CODE + "5\n1\nNowy tytuł\n\n\n\n1\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Zaktualizowano dane 2 egz.")),
                () -> assertTrue(console.output().contains("Nowy tytuł")),
                () -> assertTrue(console.output().contains("Robert C. Martin")),
                () -> assertTrue(console.output().contains("2008"))
        );
    }

    /**
     * Po potwierdzeniu egzemplarz powinien zostać usunięty, a ostatni egzemplarz usuwa książkę z katalogu.
     */
    @Test
    @DisplayName("usuwa egzemplarze po potwierdzeniu, aż katalog będzie pusty")
    void shouldDeleteCopies() {
        TestConsole console = run(ADD_CLEAN_CODE + "6\n1\n1\nt\n6\n1\n1\nt\n1\n0\n");

        assertAll(
                () -> assertEquals(2, console.count("Usunięto egzemplarz.")),
                () -> assertTrue(console.output().contains("Katalog jest pusty."))
        );
    }

    /**
     * Odpowiedź "nie" na pytanie o potwierdzenie powinna zostawić egzemplarz.
     */
    @Test
    @DisplayName("anulowanie usuwania zostawia egzemplarze")
    void shouldCancelDeletion() {
        TestConsole console = run(ADD_CLEAN_CODE + "6\n1\n1\nn\n1\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Anulowano.")),
                () -> assertFalse(console.output().contains("Usunięto egzemplarz.")),
                () -> assertTrue(console.output().contains("2/2"))
        );
    }

    /**
     * Wypożyczony egzemplarz nie może zostać usunięty, a katalog pokazuje brak wolnych egzemplarzy.
     */
    @Test
    @DisplayName("nie pozwala usunąć wypożyczonego egzemplarza")
    void shouldNotDeleteBorrowedCopy() {
        context.bookService().addBook("Czysty kod", "Robert C. Martin", 2008, ISBN_CLEAN_CODE, Genre.NOVEL, 1);
        Member member = context.memberService().addMember("Jan", "Kowalski", "jan@example.com");
        context.loanService().borrow(member.getId(), ISBN_CLEAN_CODE);

        TestConsole console = run("6\n1\n1\nt\n1\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertFalse(console.output().contains("Usunięto egzemplarz.")),
                () -> assertTrue(console.output().contains("0/1"))
        );
    }

    /**
     * Wyszukiwanie powinno pokazać tylko pasujące książki.
     */
    @Test
    @DisplayName("wyszukuje książki po fragmencie tytułu")
    void shouldSearchByTitle() {
        TestConsole console = run(ADD_CLEAN_CODE + ADD_ALGORITHMS + "2\nkod\n0\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Robert C. Martin")),
                () -> assertFalse(console.output().contains("Autor Drugi"))
        );
    }

    /**
     * Brak wyników powinien dać komunikat.
     */
    @Test
    @DisplayName("informuje, gdy wyszukiwanie nic nie znalazło")
    void shouldReportNothingFound() {
        TestConsole console = run(ADD_CLEAN_CODE + "2\nzzz\n0\n");

        assertTrue(console.output().contains("Nic nie znaleziono."));
    }

    /**
     * Pusta fraza powinna dać komunikat błędu, a menu ma działać dalej.
     */
    @Test
    @DisplayName("pusta fraza wyszukiwania daje komunikat błędu")
    void shouldReportBlankQuery() {
        TestConsole console = run("2\n \n0\n");

        assertTrue(console.output().contains("Błąd:"));
    }

    /**
     * Menu powinno odrzucać puste zależności.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste zależności")
    void shouldRejectNullDependencies() {
        TestConsole console = new TestConsole("");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new BookMenu(null, context.bookService())),
                () -> assertThrows(IllegalArgumentException.class, () -> new BookMenu(console.io(), null))
        );
    }
}
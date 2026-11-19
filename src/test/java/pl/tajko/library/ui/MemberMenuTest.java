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
 * Testy menu czytelników na prawdziwych serwisach i plikach w folderze tymczasowym.
 */
@DisplayName("MemberMenu")
class MemberMenuTest {

    /** Stały zegar używany w testach. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Skrypt dodania Jana Kowalskiego: wybór 3, imię, nazwisko, e-mail. */
    private static final String ADD_JAN = "3\nJan\nKowalski\njan@example.com\n";

    /** Skrypt dodania Anny Nowak. */
    private static final String ADD_ANNA = "3\nAnna\nNowak\nanna@example.com\n";

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
        new MemberMenu(console.io(), context.memberService()).run();
        return console;
    }

    /**
     * Brak czytelników powinien dać komunikat.
     */
    @Test
    @DisplayName("pokazuje komunikat, gdy nie ma czytelników")
    void shouldShowMessageForNoMembers() {
        assertTrue(run("1\n0\n").output().contains("Brak czytelników."));
    }

    /**
     * Dodany czytelnik powinien pojawić się na liście.
     */
    @Test
    @DisplayName("dodaje czytelnika i pokazuje go na liście")
    void shouldAddMemberAndListIt() {
        TestConsole console = run(ADD_JAN + "1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Dodano czytelnika Jan Kowalski.")),
                () -> assertTrue(console.output().contains("jan@example.com")),
                () -> assertEquals(1, context.memberService().getMembers().size())
        );
    }

    /**
     * Zajęty adres e-mail powinien dać komunikat błędu i nie dodać drugiego czytelnika.
     */
    @Test
    @DisplayName("odrzuca zajęty adres e-mail")
    void shouldRejectDuplicateEmail() {
        TestConsole console = run(ADD_JAN + "3\nAnna\nNowak\nJAN@example.com\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertFalse(console.output().contains("Anna Nowak")),
                () -> assertEquals(1, context.memberService().getMembers().size())
        );
    }

    /**
     * Niepoprawny adres e-mail powinien dać komunikat błędu.
     */
    @Test
    @DisplayName("odrzuca niepoprawny adres e-mail")
    void shouldRejectInvalidEmail() {
        TestConsole console = run("3\nJan\nKowalski\nabc\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertTrue(context.memberService().getMembers().isEmpty())
        );
    }

    /**
     * Edycja z samym Enterem w polach powinna zmienić tylko to, co wpisano.
     */
    @Test
    @DisplayName("edytuje nazwisko, a pozostałe pola zostawia bez zmian")
    void shouldEditOnlyLastName() {
        TestConsole console = run(ADD_JAN + "4\n1\n\nZieliński\n\n1\n0\n");

        Member member = context.memberService().getMembers().stream()
                .findFirst()
                .map(summary -> context.memberService().getMember(summary.id()))
                .orElseThrow();
        assertAll(
                () -> assertTrue(console.output().contains("Zaktualizowano dane czytelnika Jan Zieliński.")),
                () -> assertEquals("Jan", member.getFirstName()),
                () -> assertEquals("Zieliński", member.getLastName()),
                () -> assertEquals("jan@example.com", member.getEmail())
        );
    }

    /**
     * Po potwierdzeniu czytelnik powinien zostać usunięty.
     */
    @Test
    @DisplayName("usuwa czytelnika po potwierdzeniu")
    void shouldDeleteMember() {
        TestConsole console = run(ADD_JAN + "5\n1\nt\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Usunięto czytelnika Jan Kowalski.")),
                () -> assertTrue(console.output().contains("Brak czytelników.")),
                () -> assertTrue(context.memberService().getMembers().isEmpty())
        );
    }

    /**
     * Odpowiedź "nie" powinna zostawić czytelnika.
     */
    @Test
    @DisplayName("anulowanie usuwania zostawia czytelnika")
    void shouldCancelDeletion() {
        TestConsole console = run(ADD_JAN + "5\n1\nn\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Anulowano.")),
                () -> assertEquals(1, context.memberService().getMembers().size())
        );
    }

    /**
     * Czytelnik z niezwróconą książką nie może zostać usunięty.
     */
    @Test
    @DisplayName("nie pozwala usunąć czytelnika z niezwróconą książką")
    void shouldNotDeleteMemberWithActiveLoan() {
        context.bookService().addBook("Czysty kod", "Robert C. Martin", 2008, "9780132350884", Genre.NOVEL, 1);
        Member member = context.memberService().addMember("Jan", "Kowalski", "jan@example.com");
        context.loanService().borrow(member.getId(), "9780132350884");

        TestConsole console = run("5\n1\nt\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertEquals(1, context.memberService().getMembers().size())
        );
    }

    /**
     * Wyszukiwanie powinno pokazać tylko pasujących czytelników.
     */
    @Test
    @DisplayName("wyszukuje czytelników po nazwisku")
    void shouldSearchMembers() {
        TestConsole console = run(ADD_JAN + ADD_ANNA + "2\nnowak\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("anna@example.com")),
                () -> assertFalse(console.output().contains("jan@example.com"))
        );
    }

    /**
     * Brak wyników powinien dać komunikat.
     */
    @Test
    @DisplayName("informuje, gdy wyszukiwanie nic nie znalazło")
    void shouldReportNothingFound() {
        TestConsole console = run(ADD_JAN + "2\nzzz\n0\n");

        assertTrue(console.output().contains("Nic nie znaleziono."));
    }

    /**
     * Menu powinno odrzucać puste zależności.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste zależności")
    void shouldRejectNullDependencies() {
        TestConsole console = new TestConsole("");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MemberMenu(null, context.memberService())),
                () -> assertThrows(IllegalArgumentException.class, () -> new MemberMenu(console.io(), null))
        );
    }
}
package pl.tajko.library.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.tajko.library.LibraryContext;
import pl.tajko.library.model.Book;
import pl.tajko.library.model.Genre;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy menu wypożyczeń na prawdziwych serwisach i plikach w folderze tymczasowym.
 * Dzisiejsza data w testach to 2026-04-15, więc termin zwrotu wypożyczenia z dzisiaj to 2026-05-15.
 */
@DisplayName("LoanMenu")
class LoanMenuTest {

    /** Stały zegar używany w testach. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Poprawny numer ISBN-13 pierwszej książki. */
    private static final String ISBN_A = "9780132350884";

    /** Poprawny numer ISBN-13 drugiej książki. */
    private static final String ISBN_B = "9780306406157";

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
        new LoanMenu(console.io(), context.loanService(), context.memberService(), context.bookService()).run();
        return console;
    }

    /**
     * Dodaje czytelnika Jana Kowalskiego (na liście posortowanej po nazwisku jest przed Anną Nowak).
     *
     * @return dodany czytelnik
     */
    private Member addJan() {
        return context.memberService().addMember("Jan", "Kowalski", "jan@example.com");
    }

    /**
     * Dodaje czytelniczkę Annę Nowak.
     *
     * @return dodana czytelniczka
     */
    private Member addAnna() {
        return context.memberService().addMember("Anna", "Nowak", "anna@example.com");
    }

    /**
     * Dodaje książkę w podanej liczbie egzemplarzy.
     *
     * @param title  tytuł
     * @param isbn   poprawny numer ISBN
     * @param copies liczba egzemplarzy
     */
    private void addBook(String title, String isbn, int copies) {
        context.bookService().addBook(title, "Autor Testowy", 2008, isbn, Genre.NOVEL, copies);
    }

    /**
     * Zapisuje wypożyczenie z datą z przeszłości (serwis zawsze wypożycza "od dzisiaj").
     *
     * @param isbn     książka, której pierwszy egzemplarz jest wypożyczany
     * @param member   czytelnik
     * @param loanDate data wypożyczenia
     * @param returned data zwrotu lub {@code null}, jeśli książka nie została zwrócona
     */
    private void saveOldLoan(String isbn, Member member, LocalDate loanDate, LocalDate returned) {
        Book copy = context.bookService().getCopies(isbn).get(0);
        Loan loan = new Loan(copy.getId(), member.getId(), loanDate);
        if (returned != null) {
            loan.markAsReturned(returned);
        }
        context.loanRepository().save(loan);
    }

    /**
     * Poprawne wypożyczenie powinno zapisać wypożyczenie i podać termin zwrotu.
     */
    @Test
    @DisplayName("wypożycza książkę czytelnikowi i podaje termin zwrotu")
    void shouldBorrowBook() {
        addJan();
        addBook("Czysty kod", ISBN_A, 1);

        TestConsole console = run("1\n1\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains(
                        "Wypożyczono \"Czysty kod\" czytelnikowi Jan Kowalski. Termin zwrotu: 2026-05-15.")),
                () -> assertEquals(1, context.loanService().getActiveLoans().size())
        );
    }

    /**
     * Bez czytelników wypożyczanie powinno dać komunikat.
     */
    @Test
    @DisplayName("wypożyczanie bez czytelników daje komunikat")
    void shouldReportNoMembers() {
        addBook("Czysty kod", ISBN_A, 1);

        assertTrue(run("1\n0\n").output().contains("Brak czytelników."));
    }

    /**
     * Bez książek wypożyczanie powinno dać komunikat po wyborze czytelnika.
     */
    @Test
    @DisplayName("wypożyczanie przy pustym katalogu daje komunikat")
    void shouldReportEmptyCatalog() {
        addJan();

        assertTrue(run("1\n1\n0\n").output().contains("Katalog jest pusty."));
    }

    /**
     * Anulowanie na etapie wyboru czytelnika nie powinno niczego wypożyczyć.
     */
    @Test
    @DisplayName("anulowanie przy wyborze czytelnika niczego nie wypożycza")
    void shouldCancelBorrowing() {
        addJan();
        addBook("Czysty kod", ISBN_A, 1);

        run("1\n0\n0\n");

        assertTrue(context.loanService().getActiveLoans().isEmpty());
    }

    /**
     * Gdy wszystkie egzemplarze są wypożyczone, drugi czytelnik dostaje komunikat błędu.
     */
    @Test
    @DisplayName("odrzuca wypożyczenie, gdy brak wolnych egzemplarzy")
    void shouldRejectWhenNoFreeCopy() {
        Member jan = addJan();
        addAnna();
        addBook("Czysty kod", ISBN_A, 1);
        context.loanService().borrow(jan.getId(), ISBN_A);

        TestConsole console = run("1\n2\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertEquals(1, context.loanService().getActiveLoans().size())
        );
    }

    /**
     * Ten sam czytelnik nie może wypożyczyć drugiego egzemplarza tej samej książki.
     */
    @Test
    @DisplayName("odrzuca drugi egzemplarz tej samej książki dla tego samego czytelnika")
    void shouldRejectSecondCopyForSameMember() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 2);
        context.loanService().borrow(jan.getId(), ISBN_A);

        TestConsole console = run("1\n1\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertEquals(1, context.loanService().getActiveLoans().size())
        );
    }

    /**
     * Czytelnik z książką po terminie nie może wypożyczać kolejnych.
     */
    @Test
    @DisplayName("odrzuca wypożyczenie czytelnikowi z książką po terminie")
    void shouldRejectMemberWithOverdueLoan() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 1);
        addBook("Algorytmy", ISBN_B, 1);
        saveOldLoan(ISBN_A, jan, LocalDate.of(2026, 3, 1), null);

        TestConsole console = run("1\n1\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Błąd:")),
                () -> assertTrue(console.output().contains("po terminie")),
                () -> assertEquals(1, context.loanService().getActiveLoans().size())
        );
    }

    /**
     * Poprawny zwrot powinien oznaczyć wypożyczenie jako zwrócone.
     */
    @Test
    @DisplayName("zwraca książkę wybraną z listy wypożyczeń czytelnika")
    void shouldReturnBook() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 1);
        context.loanService().borrow(jan.getId(), ISBN_A);

        TestConsole console = run("2\n1\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Zwrócono \"Czysty kod\".")),
                () -> assertTrue(context.loanService().getActiveLoans().isEmpty())
        );
    }

    /**
     * Zwrot książki po terminie powinien zostać odnotowany w komunikacie.
     */
    @Test
    @DisplayName("zwrot po terminie jest odnotowany w komunikacie")
    void shouldMentionOverdueReturn() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 1);
        saveOldLoan(ISBN_A, jan, LocalDate.of(2026, 3, 1), null);

        TestConsole console = run("2\n1\n1\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("zwrot po terminie")),
                () -> assertTrue(context.loanService().getActiveLoans().isEmpty())
        );
    }

    /**
     * Gdy nikt nie ma książek, zwrot powinien dać komunikat.
     */
    @Test
    @DisplayName("zwrot, gdy nikt nic nie wypożyczył, daje komunikat")
    void shouldReportNoBorrowers() {
        addJan();

        assertTrue(run("2\n0\n").output().contains("Nikt nie ma wypożyczonych książek."));
    }

    /**
     * Anulowanie na etapie wyboru wypożyczenia nie powinno niczego zwrócić.
     */
    @Test
    @DisplayName("anulowanie przy wyborze wypożyczenia niczego nie zwraca")
    void shouldCancelReturn() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 1);
        context.loanService().borrow(jan.getId(), ISBN_A);

        run("2\n1\n0\n0\n");

        assertEquals(1, context.loanService().getActiveLoans().size());
    }

    /**
     * Lista aktywnych powinna pokazywać tytuł, czytelnika i status "w terminie".
     */
    @Test
    @DisplayName("pokazuje aktywne wypożyczenia ze statusem")
    void shouldShowActiveLoans() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 1);
        context.loanService().borrow(jan.getId(), ISBN_A);

        TestConsole console = run("3\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("Czysty kod")),
                () -> assertTrue(console.output().contains("Jan Kowalski")),
                () -> assertTrue(console.output().contains("w terminie"))
        );
    }

    /**
     * Brak aktywnych wypożyczeń powinien dać komunikat.
     */
    @Test
    @DisplayName("informuje o braku aktywnych wypożyczeń")
    void shouldReportNoActiveLoans() {
        assertTrue(run("3\n0\n").output().contains("Brak aktywnych wypożyczeń."));
    }

    /**
     * Lista po terminie powinna pokazywać status "PO TERMINIE".
     */
    @Test
    @DisplayName("pokazuje wypożyczenia po terminie")
    void shouldShowOverdueLoans() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 1);
        saveOldLoan(ISBN_A, jan, LocalDate.of(2026, 3, 1), null);

        TestConsole console = run("4\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("PO TERMINIE")),
                () -> assertFalse(console.output().contains("w terminie"))
        );
    }

    /**
     * Brak wypożyczeń po terminie powinien dać komunikat.
     */
    @Test
    @DisplayName("informuje o braku wypożyczeń po terminie")
    void shouldReportNoOverdueLoans() {
        assertTrue(run("4\n0\n").output().contains("Brak wypożyczeń po terminie."));
    }

    /**
     * Historia powinna zawierać także zwrócone wypożyczenia z datą zwrotu.
     */
    @Test
    @DisplayName("historia zawiera zwrócone wypożyczenia z datą zwrotu")
    void shouldShowHistoryWithReturnedLoans() {
        Member jan = addJan();
        addBook("Czysty kod", ISBN_A, 1);
        addBook("Algorytmy", ISBN_B, 1);
        saveOldLoan(ISBN_A, jan, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10));
        context.loanService().borrow(jan.getId(), ISBN_B);

        TestConsole console = run("5\n0\n");

        assertAll(
                () -> assertTrue(console.output().contains("zwrócona 2026-02-10")),
                () -> assertTrue(console.output().contains("w terminie"))
        );
    }

    /**
     * Pusta historia powinna dać komunikat.
     */
    @Test
    @DisplayName("informuje o pustej historii")
    void shouldReportEmptyHistory() {
        assertTrue(run("5\n0\n").output().contains("Brak wypożyczeń w historii."));
    }

    /**
     * Menu powinno odrzucać puste zależności.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste zależności")
    void shouldRejectNullDependencies() {
        TestConsole console = new TestConsole("");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new LoanMenu(null,
                        context.loanService(), context.memberService(), context.bookService())),
                () -> assertThrows(IllegalArgumentException.class, () -> new LoanMenu(console.io(),
                        null, context.memberService(), context.bookService())),
                () -> assertThrows(IllegalArgumentException.class, () -> new LoanMenu(console.io(),
                        context.loanService(), null, context.bookService())),
                () -> assertThrows(IllegalArgumentException.class, () -> new LoanMenu(console.io(),
                        context.loanService(), context.memberService(), null))
        );
    }
}
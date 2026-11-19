package pl.tajko.library.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.tajko.library.LibraryContext;
import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.model.Member;
import pl.tajko.library.service.BookSummary;
import pl.tajko.library.service.MemberSummary;

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
 * Testy jednostkowe klasy {@link DemoDataLoader}, na prawdziwych serwisach i plikach w folderze tymczasowym.
 */
@DisplayName("DemoDataLoader")
class DemoDataLoaderTest {

    /** Stały zegar używany w testach. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Folder tymczasowy na pliki danych. */
    @TempDir
    Path tempDir;

    /** Kontekst aplikacji używany w teście. */
    private LibraryContext context;

    /** Testowany loader. */
    private DemoDataLoader loader;

    /**
     * Tworzy świeży kontekst z pustymi danymi i loader przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        context = new LibraryContext(tempDir, CLOCK);
        loader = new DemoDataLoader(context.bookService(), context.memberService(), context.loanService(),
                context.loanRepository(), CLOCK);
    }

    /**
     * Konstruktor powinien odrzucać puste zależności.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste zależności")
    void shouldRejectNullDependencies() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new DemoDataLoader(null,
                        context.memberService(), context.loanService(), context.loanRepository(), CLOCK)),
                () -> assertThrows(IllegalArgumentException.class, () -> new DemoDataLoader(
                        context.bookService(), null, context.loanService(), context.loanRepository(), CLOCK)),
                () -> assertThrows(IllegalArgumentException.class, () -> new DemoDataLoader(
                        context.bookService(), context.memberService(), null, context.loanRepository(), CLOCK)),
                () -> assertThrows(IllegalArgumentException.class, () -> new DemoDataLoader(
                        context.bookService(), context.memberService(), context.loanService(), null, CLOCK)),
                () -> assertThrows(IllegalArgumentException.class, () -> new DemoDataLoader(
                        context.bookService(), context.memberService(), context.loanService(),
                        context.loanRepository(), null))
        );
    }

    /**
     * Pusta biblioteka nie ma danych, a po załadowaniu ma.
     */
    @Test
    @DisplayName("hasData: pusta biblioteka nie ma danych, po załadowaniu ma")
    void shouldReportWhetherDataExists() {
        assertFalse(loader.hasData());

        loader.load();

        assertTrue(loader.hasData());
    }

    /**
     * Podsumowanie i faktyczna zawartość biblioteki powinny się zgadzać.
     */
    @Test
    @DisplayName("ładuje tytuły, czytelników i wypożyczenia")
    void shouldLoadDemoData() {
        DemoDataLoader.DemoSummary summary = loader.load();

        int totalCopies = context.bookService().getCatalog().stream().mapToInt(BookSummary::totalCopies).sum();
        assertAll(
                () -> assertEquals(12, context.bookService().getCatalog().size()),
                () -> assertEquals(totalCopies, summary.copies()),
                () -> assertEquals(12, summary.titles()),
                () -> assertEquals(5, summary.members()),
                () -> assertEquals(6, summary.loans()),
                () -> assertEquals(5, context.memberService().getMembers().size()),
                () -> assertEquals(6, context.loanService().getHistory().size()),
                () -> assertEquals(5, context.loanService().getActiveLoans().size())
        );
    }

    /**
     * Dokładnie jedno wypożyczenie jest po terminie, a jeden z czytelników ma dwie książki.
     */
    @Test
    @DisplayName("zawiera jedno wypożyczenie po terminie i czytelnika z dwiema książkami")
    void shouldContainOverdueLoanAndBusyMember() {
        loader.load();

        MemberSummary busiest = context.memberService().getMembers().stream()
                .filter(member -> member.activeLoans() == 2)
                .findFirst()
                .orElseThrow();
        assertAll(
                () -> assertEquals(1, context.loanService().getOverdueLoans().size()),
                () -> assertEquals("Jan Kowalski", busiest.fullName())
        );
    }

    /**
     * Czytelnik z wypożyczeniem po terminie nie powinien móc wypożyczyć kolejnej książki.
     */
    @Test
    @DisplayName("czytelnik z wypożyczeniem po terminie nie może wypożyczać")
    void shouldBlockMemberWithOverdueLoan() {
        loader.load();

        Member overdueMember = context.memberService().getMembers().stream()
                .filter(MemberSummary::hasOverdueLoans)
                .map(summary -> context.memberService().getMember(summary.id()))
                .findFirst()
                .orElseThrow();
        String isbn = context.bookService().getCatalog().get(0).isbn();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> context.loanService().borrow(overdueMember.getId(), isbn));
        assertTrue(exception.getMessage().contains("po terminie"));
    }

    /**
     * Drugie załadowanie powinno zostać odrzucone i niczego nie zdublować.
     */
    @Test
    @DisplayName("odrzuca ponowne załadowanie danych")
    void shouldRejectSecondLoad() {
        loader.load();

        assertAll(
                () -> assertThrows(BusinessRuleException.class, () -> loader.load()),
                () -> assertEquals(12, context.bookService().getCatalog().size()),
                () -> assertEquals(5, context.memberService().getMembers().size())
        );
    }

    /**
     * Istniejące dane (nawet jeden czytelnik) powinny blokować załadowanie i niczego nie dopisać.
     */
    @Test
    @DisplayName("odrzuca załadowanie, gdy w bibliotece są już jakieś dane")
    void shouldRejectLoadWhenDataExists() {
        context.memberService().addMember("Jan", "Kowalski", "jan@example.com");

        assertAll(
                () -> assertThrows(BusinessRuleException.class, () -> loader.load()),
                () -> assertTrue(context.bookService().getCatalog().isEmpty()),
                () -> assertEquals(1, context.memberService().getMembers().size())
        );
    }
}
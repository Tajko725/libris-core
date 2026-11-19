package pl.tajko.library.demo;

import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.model.Book;
import pl.tajko.library.model.Genre;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;
import pl.tajko.library.repository.Repository;
import pl.tajko.library.service.BookService;
import pl.tajko.library.service.LoanService;
import pl.tajko.library.service.MemberService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Ładuje do pustej biblioteki dane przykładowe, żeby łatwo pokazać działanie programu.
 * <p>
 * Dane to 12 tytułów w 1 do 3 egzemplarzach, 5 czytelników i 6 wypożyczeń: 4 aktywne w terminie,
 * 1 aktywne po terminie (czytelnik z takim wypożyczeniem nie może brać kolejnych książek)
 * oraz 1 już zwrócone. Numery ISBN mają poprawne sumy kontrolne, ale nie odpowiadają prawdziwym wydaniom.
 * <p>
 * Wypożyczenia z datą z przeszłości są zapisywane bezpośrednio w repozytorium, bo
 * {@link LoanService} zawsze wypożycza "od dzisiaj".
 */
public class DemoDataLoader {

    /**
     * Podsumowanie załadowanych danych.
     *
     * @param titles  liczba tytułów w katalogu
     * @param copies  liczba wszystkich egzemplarzy
     * @param members liczba czytelników
     * @param loans   liczba wypożyczeń w historii (aktywnych i zwróconych)
     */
    public record DemoSummary(int titles, int copies, int members, int loans) {
    }

    /**
     * Opis książki do załadowania.
     *
     * @param title  tytuł
     * @param author autor
     * @param year   rok wydania
     * @param isbn   numer ISBN-13 z poprawną sumą kontrolną
     * @param genre  gatunek
     * @param copies liczba egzemplarzy
     */
    private record BookSeed(String title, String author, int year, String isbn, Genre genre, int copies) {
    }

    /** Liczba dni, od których trwa wypożyczenie po terminie (termin zwrotu to 30 dni). */
    private static final int OVERDUE_LOAN_AGE_DAYS = 40;

    private static final BookSeed PAN_TADEUSZ =
            new BookSeed("Pan Tadeusz", "Adam Mickiewicz", 1834, "9788370010003", Genre.OTHER, 3);
    private static final BookSeed LALKA =
            new BookSeed("Lalka", "Bolesław Prus", 1890, "9788370010379", Genre.NOVEL, 2);
    private static final BookSeed FERDYDURKE =
            new BookSeed("Ferdydurke", "Witold Gombrowicz", 1937, "9788370010744", Genre.NOVEL, 1);
    private static final BookSeed SOLARIS =
            new BookSeed("Solaris", "Stanisław Lem", 1961, "9788370011116", Genre.SCIENCE_FICTION, 2);
    private static final BookSeed WIEDZMIN =
            new BookSeed("Wiedźmin: Ostatnie życzenie", "Andrzej Sapkowski", 1993, "9788370011482",
                    Genre.FANTASY, 3);
    private static final BookSeed QUO_VADIS =
            new BookSeed("Quo vadis", "Henryk Sienkiewicz", 1896, "9788370011857", Genre.HISTORY, 2);
    private static final BookSeed AKADEMIA =
            new BookSeed("Akademia Pana Kleksa", "Jan Brzechwa", 1946, "9788370012229", Genre.CHILDREN, 2);
    private static final BookSeed HAWKING =
            new BookSeed("Krótka historia czasu", "Stephen Hawking", 1988, "9788370012595", Genre.SCIENCE, 1);
    private static final BookSeed ZBRODNIA =
            new BookSeed("Zbrodnia i kara", "Fiodor Dostojewski", 1866, "9788370012960", Genre.CRIME, 2);
    private static final BookSeed CHLOPI =
            new BookSeed("Chłopi", "Władysław Reymont", 1904, "9788370013332", Genre.NOVEL, 1);
    private static final BookSeed KAMIENIE =
            new BookSeed("Kamienie na szaniec", "Aleksander Kamiński", 1943, "9788370013707", Genre.HISTORY, 2);
    private static final BookSeed PANI_CURIE =
            new BookSeed("Pani Curie", "Ève Curie", 1937, "9788370014070", Genre.BIOGRAPHY, 1);

    /** Wszystkie książki ładowane jako dane przykładowe. */
    private static final List<BookSeed> BOOKS = List.of(PAN_TADEUSZ, LALKA, FERDYDURKE, SOLARIS, WIEDZMIN,
            QUO_VADIS, AKADEMIA, HAWKING, ZBRODNIA, CHLOPI, KAMIENIE, PANI_CURIE);

    /** Serwis książek. */
    private final BookService bookService;

    /** Serwis czytelników. */
    private final MemberService memberService;

    /** Serwis wypożyczeń. */
    private final LoanService loanService;

    /** Repozytorium wypożyczeń, używane do zapisu wypożyczeń z datą z przeszłości. */
    private final Repository<Loan> loanRepository;

    /** Zegar, z którego pochodzi dzisiejsza data. */
    private final Clock clock;

    /**
     * Tworzy loader korzystający z podanych serwisów.
     *
     * @param bookService    serwis książek, nie może być {@code null}
     * @param memberService  serwis czytelników, nie może być {@code null}
     * @param loanService    serwis wypożyczeń, nie może być {@code null}
     * @param loanRepository repozytorium wypożyczeń, nie może być {@code null}
     * @param clock          zegar, nie może być {@code null}
     * @throws IllegalArgumentException jeśli którykolwiek argument jest {@code null}
     */
    public DemoDataLoader(BookService bookService, MemberService memberService, LoanService loanService,
                          Repository<Loan> loanRepository, Clock clock) {
        if (bookService == null || memberService == null || loanService == null
                || loanRepository == null || clock == null) {
            throw new IllegalArgumentException("Serwisy, repozytorium i zegar nie mogą być puste");
        }
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    /**
     * Sprawdza, czy w bibliotece są już jakiekolwiek dane (książki, czytelnicy lub wypożyczenia).
     *
     * @return {@code true}, jeśli biblioteka nie jest pusta
     */
    public boolean hasData() {
        return !bookService.getCatalog().isEmpty()
                || !memberService.getMembers().isEmpty()
                || !loanService.getHistory().isEmpty();
    }

    /**
     * Ładuje dane przykładowe. Działa tylko, gdy biblioteka jest pusta, żeby niczego nie nadpisać.
     *
     * @return podsumowanie załadowanych danych
     * @throws BusinessRuleException jeśli w bibliotece są już jakiekolwiek dane
     */
    public DemoSummary load() {
        if (hasData()) {
            throw new BusinessRuleException("Dane przykładowe można załadować tylko do pustej biblioteki");
        }
        LocalDate today = LocalDate.now(clock);

        int copies = 0;
        for (BookSeed seed : BOOKS) {
            copies += bookService.addBook(seed.title(), seed.author(), seed.year(), seed.isbn(),
                    seed.genre(), seed.copies()).size();
        }

        Member anna = memberService.addMember("Anna", "Nowak", "anna.nowak@example.com");
        Member jan = memberService.addMember("Jan", "Kowalski", "jan.kowalski@example.com");
        Member piotr = memberService.addMember("Piotr", "Zieliński", "piotr.zielinski@example.com");
        Member katarzyna = memberService.addMember("Katarzyna", "Wiśniewska", "katarzyna.wisniewska@example.com");
        Member lukasz = memberService.addMember("Łukasz", "Wójcik", "lukasz.wojcik@example.com");

        loanService.borrow(jan.getId(), SOLARIS.isbn());
        loanService.borrow(jan.getId(), LALKA.isbn());
        loanService.borrow(anna.getId(), WIEDZMIN.isbn());
        loanService.borrow(piotr.getId(), PAN_TADEUSZ.isbn());

        // wypożyczenie po terminie (czytelnik nie może już wypożyczać kolejnych książek)
        saveOldLoan(CHLOPI, lukasz, today.minusDays(OVERDUE_LOAN_AGE_DAYS), null);
        // wypożyczenie już zwrócone, widoczne tylko w historii
        saveOldLoan(FERDYDURKE, katarzyna, today.minusDays(60), today.minusDays(45));

        return new DemoSummary(BOOKS.size(), copies, memberService.getMembers().size(),
                loanService.getHistory().size());
    }

    /**
     * Zapisuje wypożyczenie z datą z przeszłości pierwszego egzemplarza podanej książki.
     *
     * @param seed       książka do wypożyczenia
     * @param member     czytelnik
     * @param loanDate   data wypożyczenia
     * @param returnDate data zwrotu lub {@code null}, jeśli książka nie została zwrócona
     */
    private void saveOldLoan(BookSeed seed, Member member, LocalDate loanDate, LocalDate returnDate) {
        Book copy = bookService.getCopies(seed.isbn()).get(0);
        Loan loan = new Loan(copy.getId(), member.getId(), loanDate);
        if (returnDate != null) {
            loan.markAsReturned(returnDate);
        }
        loanRepository.save(loan);
    }
}
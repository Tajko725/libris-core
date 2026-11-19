package pl.tajko.library;

import pl.tajko.library.model.Book;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;
import pl.tajko.library.repository.JsonRepository;
import pl.tajko.library.repository.Repository;
import pl.tajko.library.service.BookService;
import pl.tajko.library.service.LoanService;
import pl.tajko.library.service.MemberService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Składa aplikację w całość: tworzy repozytoria zapisujące do plików JSON i serwisy, które z nich korzystają.
 * <p>
 * Repozytorium wypożyczeń jest jedno i współdzielone przez wszystkie serwisy, żeby każdy widział
 * te same dane. Takie miejsce składania zależności nazywa się czasem "composition root".
 */
public class LibraryContext {

    /** Nazwa pliku z książkami. */
    static final String BOOKS_FILE = "books.json";

    /** Nazwa pliku z czytelnikami. */
    static final String MEMBERS_FILE = "members.json";

    /** Nazwa pliku z wypożyczeniami. */
    static final String LOANS_FILE = "loans.json";

    /** Folder z plikami danych. */
    private final Path dataDir;

    /** Serwis książek. */
    private final BookService bookService;

    /** Serwis czytelników. */
    private final MemberService memberService;

    /** Serwis wypożyczeń. */
    private final LoanService loanService;

    /** Repozytorium czytelników, udostępniane wyłącznie danym przykładowym. */
    private final Repository<Member> memberRepository;

    /** Repozytorium wypożyczeń, udostępniane wyłącznie danym przykładowym. */
    private final Repository<Loan> loanRepository;

    /**
     * Tworzy kontekst i podłącza pliki z podanego folderu. Brakujący folder zostanie utworzony.
     *
     * @param dataDir folder na pliki danych, nie może być {@code null}
     * @param clock   zegar używany przez serwisy, nie może być {@code null}
     * @throws IllegalArgumentException jeśli którykolwiek argument jest {@code null}
     * @throws UncheckedIOException     jeśli nie da się utworzyć folderu z danymi
     */
    public LibraryContext(Path dataDir, Clock clock) {
        if (dataDir == null || clock == null) {
            throw new IllegalArgumentException("Folder danych i zegar nie mogą być puste");
        }
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Nie można utworzyć folderu z danymi: " + dataDir, e);
        }
        this.dataDir = dataDir;

        Repository<Book> bookRepository =
                new JsonRepository<>(dataDir.resolve(BOOKS_FILE), Book.class, Book::getId);
        this.memberRepository = new JsonRepository<>(dataDir.resolve(MEMBERS_FILE), Member.class, Member::getId);
        this.loanRepository = new JsonRepository<>(dataDir.resolve(LOANS_FILE), Loan.class, Loan::getId);

        this.bookService = new BookService(bookRepository, loanRepository);
        this.memberService = new MemberService(memberRepository, loanRepository, clock);
        this.loanService = new LoanService(loanRepository, bookRepository, memberRepository, clock);
    }

    /**
     * Zwraca folder z plikami danych.
     *
     * @return ścieżka do folderu danych
     */
    public Path dataDir() {
        return dataDir;
    }

    /**
     * Zwraca serwis książek.
     *
     * @return serwis książek
     */
    public BookService bookService() {
        return bookService;
    }

    /**
     * Zwraca serwis czytelników.
     *
     * @return serwis czytelników
     */
    public MemberService memberService() {
        return memberService;
    }

    /**
     * Zwraca serwis wypożyczeń.
     *
     * @return serwis wypożyczeń
     */
    public LoanService loanService() {
        return loanService;
    }

    /**
     * Zwraca repozytorium wypożyczeń.
     * <p>
     * Normalnie używa się {@link LoanService}. Repozytorium jest udostępnione tylko po to, aby dane przykładowe
     * mogły zapisać wypożyczenia z datą z przeszłości (serwis zawsze wypożycza "od dzisiaj").
     *
     * @return repozytorium wypożyczeń
     */
    public Repository<Loan> loanRepository() {
        return loanRepository;
    }

    /**
     * Zwraca repozytorium czytelników.
     * <p>
     * Normalnie używa się {@link MemberService}. Repozytorium jest udostępnione tylko po to, aby dane przykładowe
     * mogły zapisać czytelników z datą rejestracji z przeszłości (serwis zawsze rejestruje "od dzisiaj").
     *
     * @return repozytorium czytelników
     */
    public Repository<Member> memberRepository() {
        return memberRepository;
    }
}
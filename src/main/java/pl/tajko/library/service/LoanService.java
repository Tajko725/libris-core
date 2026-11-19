package pl.tajko.library.service;

import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.exception.EntityNotFoundException;
import pl.tajko.library.model.Book;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;
import pl.tajko.library.repository.Repository;
import pl.tajko.library.validation.IsbnValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serwis obsługujący wypożyczenia i zwroty książek.
 * <p>
 * Czytelnik wypożycza książkę po numerze ISBN, a serwis sam wybiera wolny egzemplarz.
 * Obowiązują zasady:
 * <ul>
 *   <li>czytelnik z książką po terminie zwrotu nie może wypożyczać kolejnych,</li>
 *   <li>czytelnik może mieć co najwyżej {@value #MAX_ACTIVE_LOANS} aktywnych wypożyczeń,</li>
 *   <li>czytelnik nie może mieć dwóch egzemplarzy tej samej książki (tego samego ISBN) naraz,</li>
 *   <li>wypożyczyć można tylko wolny egzemplarz.</li>
 * </ul>
 * Zegar ({@link Clock}) jest wstrzykiwany, aby w testach można było użyć stałej daty.
 */
public class LoanService {

    /** Maksymalna liczba aktywnych wypożyczeń jednego czytelnika. */
    public static final int MAX_ACTIVE_LOANS = 5;

    /** Tytuł wyświetlany, gdy egzemplarz został usunięty z biblioteki. */
    static final String UNKNOWN_BOOK_TITLE = "(usunięta książka)";

    /** Imię i nazwisko wyświetlane, gdy czytelnik został usunięty z biblioteki. */
    static final String UNKNOWN_MEMBER_NAME = "(usunięty czytelnik)";

    /** Tekst wyświetlany zamiast numeru ISBN usuniętego egzemplarza. */
    static final String UNKNOWN_ISBN = "-";

    /** Repozytorium wypożyczeń. */
    private final Repository<Loan> loanRepository;

    /** Repozytorium egzemplarzy książek. */
    private final Repository<Book> bookRepository;

    /** Repozytorium czytelników. */
    private final Repository<Member> memberRepository;

    /** Zegar, z którego serwis odczytuje dzisiejszą datę. */
    private final Clock clock;

    /**
     * Tworzy serwis korzystający z podanych zależności.
     *
     * @param loanRepository   repozytorium wypożyczeń, nie może być {@code null}
     * @param bookRepository   repozytorium książek, nie może być {@code null}
     * @param memberRepository repozytorium czytelników, nie może być {@code null}
     * @param clock            zegar, nie może być {@code null}
     * @throws IllegalArgumentException jeśli którakolwiek zależność jest {@code null}
     */
    public LoanService(Repository<Loan> loanRepository, Repository<Book> bookRepository,
                       Repository<Member> memberRepository, Clock clock) {
        if (loanRepository == null || bookRepository == null || memberRepository == null || clock == null) {
            throw new IllegalArgumentException("Repozytoria i zegar nie mogą być puste");
        }
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    /**
     * Wypożycza czytelnikowi wolny egzemplarz książki o podanym numerze ISBN.
     * Data wypożyczenia to dzisiejsza data z zegara serwisu.
     *
     * @param memberId identyfikator czytelnika
     * @param isbn     numer ISBN książki (może zawierać myślniki i spacje)
     * @return zapisane wypożyczenie
     * @throws IllegalArgumentException jeśli identyfikator lub ISBN jest pusty
     * @throws EntityNotFoundException  jeśli czytelnik lub książka o takim ISBN nie istnieje
     * @throws BusinessRuleException    jeśli czytelnik ma książkę po terminie, osiągnął limit,
     *                                  ma już egzemplarz tej książki albo brak wolnych egzemplarzy
     */
    public Loan borrow(UUID memberId, String isbn) {
        if (memberId == null) {
            throw new IllegalArgumentException("Identyfikator czytelnika nie może być pusty");
        }
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("Numer ISBN nie może być pusty");
        }
        Member member = requireMember(memberId);
        String normalizedIsbn = IsbnValidator.normalize(isbn);
        List<Book> copies = bookRepository.findAll().stream()
                .filter(book -> book.getIsbn().equals(normalizedIsbn))
                .toList();
        if (copies.isEmpty()) {
            throw new EntityNotFoundException("Nie znaleziono książki o numerze ISBN " + normalizedIsbn);
        }

        LocalDate today = LocalDate.now(clock);
        List<Loan> activeLoans = loanRepository.findAll().stream().filter(Loan::isActive).toList();
        List<Loan> memberLoans = activeLoans.stream()
                .filter(loan -> loan.getMemberId().equals(memberId))
                .toList();

        if (memberLoans.stream().anyMatch(loan -> loan.isOverdue(today))) {
            throw new BusinessRuleException(
                    "Czytelnik " + member.getFullName() + " ma książkę po terminie zwrotu i nie może wypożyczać");
        }
        if (memberLoans.size() >= MAX_ACTIVE_LOANS) {
            throw new BusinessRuleException(
                    "Czytelnik " + member.getFullName() + " osiągnął limit " + MAX_ACTIVE_LOANS
                            + " aktywnych wypożyczeń");
        }
        Set<UUID> copyIds = copies.stream().map(Book::getId).collect(Collectors.toSet());
        if (memberLoans.stream().anyMatch(loan -> copyIds.contains(loan.getBookId()))) {
            throw new BusinessRuleException(
                    "Czytelnik " + member.getFullName() + " ma już egzemplarz tej książki");
        }

        Set<UUID> borrowedIds = activeLoans.stream().map(Loan::getBookId).collect(Collectors.toSet());
        Book freeCopy = copies.stream()
                .filter(copy -> !borrowedIds.contains(copy.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Brak wolnych egzemplarzy książki o numerze ISBN " + normalizedIsbn));

        Loan loan = new Loan(freeCopy.getId(), memberId, today);
        loanRepository.save(loan);
        return loan;
    }

    /**
     * Rejestruje zwrot książki z dzisiejszą datą z zegara serwisu.
     *
     * @param loanId identyfikator wypożyczenia
     * @return zaktualizowane wypożyczenie
     * @throws IllegalArgumentException jeśli identyfikator jest pusty
     * @throws EntityNotFoundException  jeśli wypożyczenie nie istnieje
     * @throws BusinessRuleException    jeśli książka została już zwrócona
     */
    public Loan returnLoan(UUID loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("Identyfikator wypożyczenia nie może być pusty");
        }
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono wypożyczenia o identyfikatorze " + loanId));
        if (!loan.isActive()) {
            throw new BusinessRuleException("Ta książka została już zwrócona");
        }
        loan.markAsReturned(LocalDate.now(clock));
        loanRepository.save(loan);
        return loan;
    }

    /**
     * Zwraca wszystkie aktywne wypożyczenia, od najwcześniejszego terminu zwrotu.
     *
     * @return lista wypożyczeń, pusta jeśli nic nie jest wypożyczone
     */
    public List<LoanSummary> getActiveLoans() {
        return summarize(loanRepository.findAll().stream().filter(Loan::isActive).toList(), ACTIVE_ORDER);
    }

    /**
     * Zwraca aktywne wypożyczenia po terminie zwrotu, od najwcześniejszego terminu.
     *
     * @return lista przeterminowanych wypożyczeń, pusta jeśli nie ma takich
     */
    public List<LoanSummary> getOverdueLoans() {
        LocalDate today = LocalDate.now(clock);
        return summarize(loanRepository.findAll().stream()
                .filter(loan -> loan.isOverdue(today))
                .toList(), ACTIVE_ORDER);
    }

    /**
     * Zwraca aktywne wypożyczenia wskazanego czytelnika, od najwcześniejszego terminu zwrotu.
     *
     * @param memberId identyfikator czytelnika
     * @return lista wypożyczeń czytelnika, pusta jeśli nic nie ma wypożyczone
     * @throws EntityNotFoundException jeśli czytelnik nie istnieje
     */
    public List<LoanSummary> getMemberLoans(UUID memberId) {
        requireMember(memberId);
        return summarize(loanRepository.findAll().stream()
                .filter(Loan::isActive)
                .filter(loan -> loan.getMemberId().equals(memberId))
                .toList(), ACTIVE_ORDER);
    }

    /**
     * Zwraca pełną historię wypożyczeń (aktywnych i zwróconych), od najnowszego wypożyczenia.
     *
     * @return lista wszystkich wypożyczeń, pusta jeśli nic jeszcze nie wypożyczono
     */
    public List<LoanSummary> getHistory() {
        return summarize(loanRepository.findAll(), HISTORY_ORDER);
    }

    /** Kolejność aktywnych wypożyczeń: najpierw te z najwcześniejszym terminem zwrotu. */
    private static final Comparator<LoanSummary> ACTIVE_ORDER = Comparator
            .comparing(LoanSummary::dueDate)
            .thenComparing(LoanSummary::bookTitle);

    /** Kolejność historii: najpierw najnowsze wypożyczenia. */
    private static final Comparator<LoanSummary> HISTORY_ORDER = Comparator
            .comparing(LoanSummary::loanDate, Comparator.reverseOrder())
            .thenComparing(LoanSummary::bookTitle);

    /**
     * Zamienia wypożyczenia na podsumowania z tytułami i nazwiskami oraz sortuje je.
     * Książki i czytelnicy są wczytywani jednorazowo i szukani w mapach.
     *
     * @param loans lista wypożyczeń do przetworzenia
     * @param order wymagana kolejność wyniku
     * @return posortowana lista podsumowań
     */
    private List<LoanSummary> summarize(List<Loan> loans, Comparator<LoanSummary> order) {
        LocalDate today = LocalDate.now(clock);
        Map<UUID, Book> booksById = bookRepository.findAll().stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        Map<UUID, Member> membersById = memberRepository.findAll().stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        return loans.stream()
                .map(loan -> toSummary(loan, booksById.get(loan.getBookId()),
                        membersById.get(loan.getMemberId()), today))
                .sorted(order)
                .toList();
    }

    /**
     * Buduje podsumowanie jednego wypożyczenia.
     *
     * @param loan   wypożyczenie
     * @param book   wypożyczony egzemplarz lub {@code null}, jeśli został usunięty
     * @param member czytelnik lub {@code null}, jeśli został usunięty
     * @param today  dzisiejsza data użyta do sprawdzenia przeterminowania
     * @return podsumowanie wypożyczenia
     */
    private static LoanSummary toSummary(Loan loan, Book book, Member member, LocalDate today) {
        return new LoanSummary(
                loan.getId(),
                loan.getBookId(),
                book == null ? UNKNOWN_ISBN : book.getIsbn(),
                book == null ? UNKNOWN_BOOK_TITLE : book.getTitle(),
                loan.getMemberId(),
                member == null ? UNKNOWN_MEMBER_NAME : member.getFullName(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.isOverdue(today));
    }

    /**
     * Zwraca czytelnika o podanym identyfikatorze lub zgłasza błąd, jeśli nie istnieje.
     *
     * @param memberId identyfikator czytelnika
     * @return znaleziony czytelnik
     * @throws EntityNotFoundException jeśli czytelnik nie istnieje
     */
    private Member requireMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono czytelnika o identyfikatorze " + memberId));
    }
}
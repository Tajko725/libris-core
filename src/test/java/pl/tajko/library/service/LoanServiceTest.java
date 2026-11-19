package pl.tajko.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.exception.EntityNotFoundException;
import pl.tajko.library.model.Book;
import pl.tajko.library.model.Genre;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;
import pl.tajko.library.repository.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe klasy {@link LoanService}.
 * <p>
 * Repozytoria są zastąpione atrapami Mockito, a zegar ma stałą datę (2026-04-15).
 * Ustawienia atrap ({@code when}) są w testach lub w metodach pomocniczych wołanych tylko tam,
 * gdzie są potrzebne, bo ścisłe stuby Mockito zgłaszają błąd przy nieużytym ustawieniu.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService")
class LoanServiceTest {

    /** Dzisiejsza data w testach. */
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 15);

    /** Stały zegar ustawiony na {@link #TODAY} (w strefie Europe/Warsaw). */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Poprawny numer ISBN-13 używany w testach (pierwsza książka). */
    private static final String ISBN_A = "9780132350884";

    /** Poprawny numer ISBN-13 używany w testach (druga książka). */
    private static final String ISBN_B = "9780306406157";

    /** Atrapa repozytorium wypożyczeń. */
    @Mock
    private Repository<Loan> loanRepository;

    /** Atrapa repozytorium książek. */
    @Mock
    private Repository<Book> bookRepository;

    /** Atrapa repozytorium czytelników. */
    @Mock
    private Repository<Member> memberRepository;

    /** Testowany serwis. */
    private LoanService service;

    /**
     * Tworzy serwis z atrapami i stałym zegarem przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        service = new LoanService(loanRepository, bookRepository, memberRepository, CLOCK);
    }

    /**
     * Konstruktor powinien odrzucać puste zależności.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste repozytoria i zegar")
    void shouldRejectNullDependencies() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new LoanService(null, bookRepository, memberRepository, CLOCK)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new LoanService(loanRepository, null, memberRepository, CLOCK)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new LoanService(loanRepository, bookRepository, null, CLOCK)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new LoanService(loanRepository, bookRepository, memberRepository, null))
        );
    }

    /**
     * Testy wypożyczania.
     */
    @Nested
    @DisplayName("borrow")
    class BorrowTests {

        /**
         * Ustawia w atrapach czytelnika i listę egzemplarzy. Wywoływana tylko tam, gdzie serwis
         * naprawdę je odczyta.
         *
         * @param member czytelnik
         * @param books  egzemplarze w bibliotece
         */
        private void stubMemberAndBooks(Member member, Book... books) {
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(bookRepository.findAll()).thenReturn(List.of(books));
        }

        /**
         * Wolny egzemplarz powinien zostać wypożyczony na 30 dni od dzisiaj.
         */
        @Test
        @DisplayName("zapisuje wypożyczenie wolnego egzemplarza z terminem zwrotu po 30 dniach")
        void shouldBorrowFreeCopy() {
            Member member = member("Jan", "Kowalski");
            Book copy = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, copy);

            Loan result = service.borrow(member.getId(), ISBN_A);

            ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
            verify(loanRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals(result, captor.getValue()),
                    () -> assertEquals(copy.getId(), result.getBookId()),
                    () -> assertEquals(member.getId(), result.getMemberId()),
                    () -> assertEquals(TODAY, result.getLoanDate()),
                    () -> assertEquals(TODAY.plusDays(30), result.getDueDate()),
                    () -> assertTrue(result.isActive())
            );
        }

        /**
         * ISBN z myślnikami powinien być rozpoznany tak samo jak zapis bez nich.
         */
        @Test
        @DisplayName("rozpoznaje ISBN zapisany z myślnikami")
        void shouldAcceptIsbnWithHyphens() {
            Member member = member("Jan", "Kowalski");
            Book copy = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, copy);

            Loan result = service.borrow(member.getId(), "978-0-13-235088-4");

            assertEquals(copy.getId(), result.getBookId());
        }

        /**
         * Gdy pierwszy egzemplarz jest wypożyczony przez kogoś innego, serwis wybiera drugi.
         */
        @Test
        @DisplayName("wybiera wolny egzemplarz, gdy inny jest już wypożyczony")
        void shouldPickFreeCopy() {
            Member member = member("Jan", "Kowalski");
            Member other = member("Anna", "Nowak");
            Book first = book("Czysty kod", ISBN_A);
            Book second = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, first, second);
            when(loanRepository.findAll()).thenReturn(
                    List.of(activeLoan(first.getId(), other.getId(), LocalDate.of(2026, 4, 10))));

            Loan result = service.borrow(member.getId(), ISBN_A);

            assertEquals(second.getId(), result.getBookId());
        }

        /**
         * Zwrócone wypożyczenie nie powinno blokować egzemplarza.
         */
        @Test
        @DisplayName("zwrócony egzemplarz jest znowu dostępny")
        void shouldTreatReturnedCopyAsFree() {
            Member member = member("Jan", "Kowalski");
            Member other = member("Anna", "Nowak");
            Book copy = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, copy);
            when(loanRepository.findAll()).thenReturn(List.of(returnedLoan(copy.getId(), other.getId())));

            Loan result = service.borrow(member.getId(), ISBN_A);

            assertEquals(copy.getId(), result.getBookId());
        }

        /**
         * Gdy wszystkie egzemplarze są wypożyczone, wypożyczenie powinno zostać odrzucone.
         */
        @Test
        @DisplayName("odrzuca wypożyczenie, gdy brak wolnych egzemplarzy")
        void shouldRejectWhenNoFreeCopy() {
            Member member = member("Jan", "Kowalski");
            Member other = member("Anna", "Nowak");
            Book copy = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, copy);
            when(loanRepository.findAll()).thenReturn(
                    List.of(activeLoan(copy.getId(), other.getId(), LocalDate.of(2026, 4, 10))));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class,
                            () -> service.borrow(member.getId(), ISBN_A)),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Czytelnik nie może mieć dwóch egzemplarzy tej samej książki, nawet jeśli inny jest wolny.
         */
        @Test
        @DisplayName("odrzuca drugi egzemplarz tej samej książki dla tego samego czytelnika")
        void shouldRejectSecondCopyOfSameBook() {
            Member member = member("Jan", "Kowalski");
            Book first = book("Czysty kod", ISBN_A);
            Book second = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, first, second);
            when(loanRepository.findAll()).thenReturn(
                    List.of(activeLoan(first.getId(), member.getId(), LocalDate.of(2026, 4, 10))));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class,
                            () -> service.borrow(member.getId(), ISBN_A)),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Po osiągnięciu limitu aktywnych wypożyczeń kolejne powinno zostać odrzucone.
         */
        @Test
        @DisplayName("odrzuca wypożyczenie po osiągnięciu limitu aktywnych wypożyczeń")
        void shouldRejectWhenLimitReached() {
            Member member = member("Jan", "Kowalski");
            Book copy = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, copy);
            List<Loan> loans = IntStream.range(0, LoanService.MAX_ACTIVE_LOANS)
                    .mapToObj(i -> activeLoan(UUID.randomUUID(), member.getId(), LocalDate.of(2026, 4, 10)))
                    .toList();
            when(loanRepository.findAll()).thenReturn(loans);

            assertAll(
                    () -> assertThrows(BusinessRuleException.class,
                            () -> service.borrow(member.getId(), ISBN_A)),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Czytelnik z książką po terminie zwrotu nie może wypożyczać kolejnych.
         */
        @Test
        @DisplayName("odrzuca wypożyczenie, gdy czytelnik ma książkę po terminie")
        void shouldRejectWhenMemberHasOverdueLoan() {
            Member member = member("Jan", "Kowalski");
            Book copy = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, copy);
            // termin zwrotu minął 2026-03-31
            when(loanRepository.findAll()).thenReturn(
                    List.of(activeLoan(UUID.randomUUID(), member.getId(), LocalDate.of(2026, 3, 1))));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class,
                            () -> service.borrow(member.getId(), ISBN_A)),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Przeterminowane wypożyczenie innego czytelnika nie powinno blokować tego czytelnika.
         */
        @Test
        @DisplayName("przeterminowane wypożyczenie innej osoby nie blokuje czytelnika")
        void shouldIgnoreOverdueLoanOfAnotherMember() {
            Member member = member("Jan", "Kowalski");
            Member other = member("Anna", "Nowak");
            Book copy = book("Czysty kod", ISBN_A);
            stubMemberAndBooks(member, copy);
            when(loanRepository.findAll()).thenReturn(
                    List.of(activeLoan(UUID.randomUUID(), other.getId(), LocalDate.of(2026, 3, 1))));

            Loan result = service.borrow(member.getId(), ISBN_A);

            assertEquals(copy.getId(), result.getBookId());
        }

        /**
         * Nieistniejący czytelnik powinien zgłosić błąd, zanim serwis zajrzy do książek.
         */
        @Test
        @DisplayName("zgłasza błąd dla nieistniejącego czytelnika")
        void shouldFailForUnknownMember() {
            UUID id = UUID.randomUUID();
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(EntityNotFoundException.class, () -> service.borrow(id, ISBN_A)),
                    () -> verifyNoInteractions(bookRepository),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Nieistniejący ISBN powinien zgłosić błąd "nie znaleziono".
         */
        @Test
        @DisplayName("zgłasza błąd dla nieznanego numeru ISBN")
        void shouldFailForUnknownIsbn() {
            Member member = member("Jan", "Kowalski");
            stubMemberAndBooks(member, book("Czysty kod", ISBN_A));

            assertAll(
                    () -> assertThrows(EntityNotFoundException.class,
                            () -> service.borrow(member.getId(), ISBN_B)),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Pusty ISBN powinien być odrzucony przed jakąkolwiek operacją na repozytoriach.
         *
         * @param isbn niepoprawny numer ISBN
         */
        @ParameterizedTest(name = "odrzuca ISBN: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void shouldRejectBlankIsbn(String isbn) {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> service.borrow(UUID.randomUUID(), isbn)),
                    () -> verifyNoInteractions(memberRepository, bookRepository, loanRepository)
            );
        }

        /**
         * Pusty identyfikator czytelnika powinien być odrzucony przed jakąkolwiek operacją na repozytoriach.
         */
        @Test
        @DisplayName("odrzuca pusty identyfikator czytelnika")
        void shouldRejectNullMemberId() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> service.borrow(null, ISBN_A)),
                    () -> verifyNoInteractions(memberRepository, bookRepository, loanRepository)
            );
        }
    }

    /**
     * Testy zwrotów.
     */
    @Nested
    @DisplayName("returnLoan")
    class ReturnLoanTests {

        /**
         * Aktywne wypożyczenie powinno dostać dzisiejszą datę zwrotu i zostać zapisane.
         */
        @Test
        @DisplayName("zapisuje zwrot z dzisiejszą datą")
        void shouldReturnLoan() {
            Loan loan = activeLoan(UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 4, 1));
            when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

            Loan result = service.returnLoan(loan.getId());

            assertAll(
                    () -> assertEquals(loan, result),
                    () -> assertEquals(TODAY, result.getReturnDate()),
                    () -> assertFalse(result.isActive()),
                    () -> verify(loanRepository).save(loan)
            );
        }

        /**
         * Książkę po terminie też można zwrócić.
         */
        @Test
        @DisplayName("pozwala zwrócić książkę po terminie")
        void shouldReturnOverdueLoan() {
            Loan loan = activeLoan(UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 3, 1));
            when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

            Loan result = service.returnLoan(loan.getId());

            assertEquals(TODAY, result.getReturnDate());
        }

        /**
         * Ponowny zwrot tej samej książki powinien być odrzucony i nie zmieniać daty zwrotu.
         */
        @Test
        @DisplayName("odrzuca ponowny zwrot")
        void shouldRejectSecondReturn() {
            Loan loan = returnedLoan(UUID.randomUUID(), UUID.randomUUID());
            LocalDate returnedAt = loan.getReturnDate();
            when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class, () -> service.returnLoan(loan.getId())),
                    () -> assertEquals(returnedAt, loan.getReturnDate()),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Nieistniejące wypożyczenie powinno zgłosić błąd.
         */
        @Test
        @DisplayName("zgłasza błąd dla nieistniejącego wypożyczenia")
        void shouldFailForUnknownLoan() {
            UUID id = UUID.randomUUID();
            when(loanRepository.findById(id)).thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(EntityNotFoundException.class, () -> service.returnLoan(id)),
                    () -> verify(loanRepository, never()).save(any(Loan.class))
            );
        }

        /**
         * Pusty identyfikator powinien być odrzucony przed odczytem repozytorium.
         */
        @Test
        @DisplayName("odrzuca pusty identyfikator")
        void shouldRejectNullId() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> service.returnLoan(null)),
                    () -> verifyNoInteractions(loanRepository)
            );
        }
    }

    /**
     * Testy list wypożyczeń.
     */
    @Nested
    @DisplayName("listy wypożyczeń")
    class QueryTests {

        /**
         * Ustawia w atrapach wypożyczenia, książki i czytelników.
         *
         * @param loans   wypożyczenia
         * @param books   egzemplarze
         * @param members czytelnicy
         */
        private void stubData(List<Loan> loans, List<Book> books, List<Member> members) {
            when(loanRepository.findAll()).thenReturn(loans);
            when(bookRepository.findAll()).thenReturn(books);
            when(memberRepository.findAll()).thenReturn(members);
        }

        /**
         * Brak wypożyczeń powinien dać puste listy.
         */
        @Test
        @DisplayName("zwracają puste listy, gdy nic nie wypożyczono")
        void shouldReturnEmptyLists() {
            assertAll(
                    () -> assertTrue(service.getActiveLoans().isEmpty()),
                    () -> assertTrue(service.getOverdueLoans().isEmpty()),
                    () -> assertTrue(service.getHistory().isEmpty())
            );
        }

        /**
         * Lista aktywnych powinna pomijać zwrócone, zawierać tytuły i nazwiska oraz być posortowana po terminie.
         */
        @Test
        @DisplayName("getActiveLoans: tylko aktywne, z tytułem i nazwiskiem, od najwcześniejszego terminu")
        void shouldListActiveLoans() {
            Member member = member("Jan", "Kowalski");
            Book bookA = book("Czysty kod", ISBN_A);
            Book bookB = book("Algorytmy", ISBN_B);
            Loan later = activeLoan(bookB.getId(), member.getId(), LocalDate.of(2026, 4, 10));
            Loan earlier = activeLoan(bookA.getId(), member.getId(), LocalDate.of(2026, 3, 1));
            Loan returned = returnedLoan(bookA.getId(), member.getId());
            stubData(List.of(later, returned, earlier), List.of(bookA, bookB), List.of(member));

            List<LoanSummary> result = service.getActiveLoans();

            assertAll(
                    () -> assertEquals(List.of(earlier.getId(), later.getId()),
                            result.stream().map(LoanSummary::loanId).toList()),
                    () -> assertEquals("Czysty kod", result.get(0).bookTitle()),
                    () -> assertEquals(ISBN_A, result.get(0).isbn()),
                    () -> assertEquals("Jan Kowalski", result.get(0).memberName()),
                    () -> assertTrue(result.get(0).overdue()),
                    () -> assertFalse(result.get(1).overdue()),
                    () -> assertTrue(result.get(0).isActive())
            );
        }

        /**
         * Lista przeterminowanych powinna zawierać tylko aktywne wypożyczenia po terminie.
         */
        @Test
        @DisplayName("getOverdueLoans: tylko aktywne po terminie")
        void shouldListOnlyOverdueLoans() {
            Member member = member("Jan", "Kowalski");
            Book bookA = book("Czysty kod", ISBN_A);
            Book bookB = book("Algorytmy", ISBN_B);
            Loan overdue = activeLoan(bookA.getId(), member.getId(), LocalDate.of(2026, 3, 1));
            Loan onTime = activeLoan(bookB.getId(), member.getId(), LocalDate.of(2026, 4, 10));
            Loan returned = returnedLoan(bookA.getId(), member.getId());
            stubData(List.of(onTime, overdue, returned), List.of(bookA, bookB), List.of(member));

            List<LoanSummary> result = service.getOverdueLoans();

            assertEquals(List.of(overdue.getId()), result.stream().map(LoanSummary::loanId).toList());
        }

        /**
         * Lista wypożyczeń czytelnika powinna zawierać tylko jego aktywne wypożyczenia.
         */
        @Test
        @DisplayName("getMemberLoans: tylko aktywne wypożyczenia wskazanego czytelnika")
        void shouldListMemberLoans() {
            Member member = member("Jan", "Kowalski");
            Member other = member("Anna", "Nowak");
            Book bookA = book("Czysty kod", ISBN_A);
            Book bookB = book("Algorytmy", ISBN_B);
            Loan mine = activeLoan(bookA.getId(), member.getId(), LocalDate.of(2026, 4, 10));
            Loan notMine = activeLoan(bookB.getId(), other.getId(), LocalDate.of(2026, 4, 10));
            Loan mineReturned = returnedLoan(bookB.getId(), member.getId());
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            stubData(List.of(mine, notMine, mineReturned), List.of(bookA, bookB), List.of(member, other));

            List<LoanSummary> result = service.getMemberLoans(member.getId());

            assertEquals(List.of(mine.getId()), result.stream().map(LoanSummary::loanId).toList());
        }

        /**
         * Nieistniejący czytelnik powinien zgłosić błąd.
         */
        @Test
        @DisplayName("getMemberLoans: błąd dla nieistniejącego czytelnika")
        void shouldFailForUnknownMember() {
            UUID id = UUID.randomUUID();
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(EntityNotFoundException.class, () -> service.getMemberLoans(id)),
                    () -> verifyNoInteractions(loanRepository)
            );
        }

        /**
         * Historia powinna zawierać także zwrócone wypożyczenia, od najnowszego.
         */
        @Test
        @DisplayName("getHistory: wszystkie wypożyczenia od najnowszego")
        void shouldListHistoryNewestFirst() {
            Member member = member("Jan", "Kowalski");
            Book bookA = book("Czysty kod", ISBN_A);
            Loan oldest = returnedLoan(bookA.getId(), member.getId());           // wypożyczone 2026-02-01
            Loan middle = activeLoan(bookA.getId(), member.getId(), LocalDate.of(2026, 3, 1));
            Loan newest = activeLoan(bookA.getId(), member.getId(), LocalDate.of(2026, 4, 10));
            stubData(List.of(middle, oldest, newest), List.of(bookA), List.of(member));

            List<LoanSummary> result = service.getHistory();

            assertAll(
                    () -> assertEquals(List.of(newest.getId(), middle.getId(), oldest.getId()),
                            result.stream().map(LoanSummary::loanId).toList()),
                    () -> assertFalse(result.get(2).isActive()),
                    () -> assertFalse(result.get(2).overdue()),
                    () -> assertNull(result.get(0).returnDate())
            );
        }

        /**
         * Usunięta książka i usunięty czytelnik powinni być zastąpieni tekstami zastępczymi.
         */
        @Test
        @DisplayName("używa tekstów zastępczych dla usuniętej książki i czytelnika")
        void shouldUsePlaceholdersForDeletedEntities() {
            Loan loan = returnedLoan(UUID.randomUUID(), UUID.randomUUID());
            stubData(List.of(loan), List.of(), List.of());

            LoanSummary summary = service.getHistory().get(0);

            assertAll(
                    () -> assertEquals(LoanService.UNKNOWN_BOOK_TITLE, summary.bookTitle()),
                    () -> assertEquals(LoanService.UNKNOWN_ISBN, summary.isbn()),
                    () -> assertEquals(LoanService.UNKNOWN_MEMBER_NAME, summary.memberName())
            );
        }
    }

    /**
     * Tworzy poprawnego czytelnika.
     *
     * @param firstName imię
     * @param lastName  nazwisko
     * @return nowy czytelnik z losowym identyfikatorem i unikalnym adresem e-mail
     */
    private static Member member(String firstName, String lastName) {
        String email = UUID.randomUUID() + "@example.com";
        return new Member(firstName, lastName, email, LocalDate.of(2025, 1, 15));
    }

    /**
     * Tworzy poprawny egzemplarz książki z losowym identyfikatorem.
     * Jeśli kolejność parametrów konstruktora {@link Book} u Ciebie jest inna, zmień tylko tę metodę.
     *
     * @param title tytuł
     * @param isbn  poprawny numer ISBN
     * @return nowy egzemplarz
     */
    private static Book book(String title, String isbn) {
        return new Book(title, "Autor Testowy", 2008, isbn, Genre.SCIENCE);
    }

    /**
     * Tworzy aktywne wypożyczenie.
     *
     * @param bookId   identyfikator egzemplarza
     * @param memberId identyfikator czytelnika
     * @param loanDate data wypożyczenia (termin zwrotu to 30 dni później)
     * @return aktywne wypożyczenie
     */
    private static Loan activeLoan(UUID bookId, UUID memberId, LocalDate loanDate) {
        return new Loan(bookId, memberId, loanDate);
    }

    /**
     * Tworzy zwrócone wypożyczenie (wypożyczone 2026-02-01, zwrócone 2026-02-10).
     *
     * @param bookId   identyfikator egzemplarza
     * @param memberId identyfikator czytelnika
     * @return zwrócone wypożyczenie
     */
    private static Loan returnedLoan(UUID bookId, UUID memberId) {
        Loan loan = new Loan(bookId, memberId, LocalDate.of(2026, 2, 1));
        loan.markAsReturned(LocalDate.of(2026, 2, 10));
        return loan;
    }
}
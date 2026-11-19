package pl.tajko.library.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy jednostkowe klasy {@link Loan}.
 * <p>
 * Wszystkie daty w testach są jawnie podane, więc wyniki nie zależą od zegara systemowego.
 */
@DisplayName("Loan")
class LoanTest {

    /** Stała data wypożyczenia używana w testach. */
    private static final LocalDate LOAN_DATE = LocalDate.of(2026, 3, 1);

    /** Termin zwrotu dla {@link #LOAN_DATE}: 30 dni później. */
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 3, 31);

    /** Identyfikator książki używany w testach. */
    private UUID bookId;

    /** Identyfikator czytelnika używany w testach. */
    private UUID memberId;

    /** Wypożyczenie tworzone przed każdym testem. */
    private Loan loan;

    /**
     * Tworzy świeże, aktywne wypożyczenie przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        loan = new Loan(bookId, memberId, LOAN_DATE);
    }

    /**
     * Testy tworzenia wypożyczenia.
     */
    @Nested
    @DisplayName("konstruktor")
    class ConstructorTests {

        /**
         * Nowe wypożyczenie powinno być aktywne i mieć termin zwrotu po 30 dniach.
         */
        @Test
        @DisplayName("tworzy aktywne wypożyczenie z terminem zwrotu po 30 dniach")
        void shouldCreateActiveLoanWithDefaultDueDate() {
            assertAll(
                    () -> assertNotNull(loan.getId()),
                    () -> assertEquals(bookId, loan.getBookId()),
                    () -> assertEquals(memberId, loan.getMemberId()),
                    () -> assertEquals(LOAN_DATE, loan.getLoanDate()),
                    () -> assertEquals(DUE_DATE, loan.getDueDate()),
                    () -> assertNull(loan.getReturnDate()),
                    () -> assertTrue(loan.isActive())
            );
        }

        /**
         * Dwa wypożyczenia powinny mieć różne identyfikatory.
         */
        @Test
        @DisplayName("nadaje różne identyfikatory kolejnym wypożyczeniom")
        void shouldGenerateUniqueIds() {
            Loan other = new Loan(bookId, memberId, LOAN_DATE);

            assertNotEquals(loan.getId(), other.getId());
        }

        /**
         * Brak identyfikatora książki, czytelnika lub daty wypożyczenia powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak książki, czytelnika lub daty wypożyczenia")
        void shouldRejectNullArguments() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(null, memberId, LOAN_DATE)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(bookId, null, LOAN_DATE)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(bookId, memberId, null))
            );
        }

        /**
         * Konstruktor odczytu powinien odtworzyć zwrócone wypożyczenie.
         */
        @Test
        @DisplayName("odtwarza zwrócone wypożyczenie z zapisanych danych")
        void shouldRestoreReturnedLoan() {
            UUID id = UUID.randomUUID();
            LocalDate returnDate = LocalDate.of(2026, 3, 10);

            Loan restored = new Loan(id, bookId, memberId, LOAN_DATE, DUE_DATE, returnDate);

            assertAll(
                    () -> assertEquals(id, restored.getId()),
                    () -> assertEquals(returnDate, restored.getReturnDate()),
                    () -> assertFalse(restored.isActive())
            );
        }

        /**
         * Termin zwrotu wcześniejszy niż data wypożyczenia powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca termin zwrotu wcześniejszy niż data wypożyczenia")
        void shouldRejectDueDateBeforeLoanDate() {
            LocalDate earlier = LOAN_DATE.minusDays(1);

            assertThrows(IllegalArgumentException.class,
                    () -> new Loan(UUID.randomUUID(), bookId, memberId, LOAN_DATE, earlier, null));
        }

        /**
         * Data zwrotu wcześniejsza niż data wypożyczenia powinna być odrzucona.
         */
        @Test
        @DisplayName("odrzuca datę zwrotu wcześniejszą niż data wypożyczenia")
        void shouldRejectReturnDateBeforeLoanDate() {
            LocalDate earlier = LOAN_DATE.minusDays(1);

            assertThrows(IllegalArgumentException.class,
                    () -> new Loan(UUID.randomUUID(), bookId, memberId, LOAN_DATE, DUE_DATE, earlier));
        }

        /**
         * Brak któregokolwiek wymaganego argumentu w konstruktorze odczytu powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak wymaganych danych w konstruktorze odczytu")
        void shouldRejectNullArgumentsWhenRestoring() {
            UUID id = UUID.randomUUID();

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(null, bookId, memberId, LOAN_DATE, DUE_DATE, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(id, null, memberId, LOAN_DATE, DUE_DATE, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(id, bookId, null, LOAN_DATE, DUE_DATE, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(id, bookId, memberId, null, DUE_DATE, null)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> new Loan(id, bookId, memberId, LOAN_DATE, null, null))
            );
        }
    }

    /**
     * Testy zwrotu książki.
     */
    @Nested
    @DisplayName("zwrot")
    class ReturnTests {

        /**
         * Zwrot powinien zapisać datę i zakończyć aktywność wypożyczenia.
         */
        @Test
        @DisplayName("markAsReturned zapisuje datę zwrotu i kończy wypożyczenie")
        void shouldMarkLoanAsReturned() {
            LocalDate returnDate = LocalDate.of(2026, 3, 15);

            loan.markAsReturned(returnDate);

            assertAll(
                    () -> assertEquals(returnDate, loan.getReturnDate()),
                    () -> assertFalse(loan.isActive())
            );
        }

        /**
         * Zwrot w dniu wypożyczenia jest dozwolony.
         */
        @Test
        @DisplayName("pozwala zwrócić książkę w dniu wypożyczenia")
        void shouldAllowReturnOnLoanDate() {
            loan.markAsReturned(LOAN_DATE);

            assertFalse(loan.isActive());
        }

        /**
         * Ponowny zwrot tego samego wypożyczenia powinien zgłosić błąd stanu obiektu.
         */
        @Test
        @DisplayName("odrzuca ponowny zwrot tego samego wypożyczenia")
        void shouldRejectSecondReturn() {
            LocalDate firstReturn = LocalDate.of(2026, 3, 15);
            loan.markAsReturned(firstReturn);

            assertAll(
                    () -> assertThrows(IllegalStateException.class,
                            () -> loan.markAsReturned(LocalDate.of(2026, 3, 20))),
                    () -> assertEquals(firstReturn, loan.getReturnDate())
            );
        }

        /**
         * Zwrot przed datą wypożyczenia powinien być odrzucony, a wypożyczenie zostaje aktywne.
         */
        @Test
        @DisplayName("odrzuca zwrot przed datą wypożyczenia")
        void shouldRejectReturnBeforeLoanDate() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> loan.markAsReturned(LOAN_DATE.minusDays(1))),
                    () -> assertTrue(loan.isActive())
            );
        }

        /**
         * Brak daty zwrotu powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak daty zwrotu")
        void shouldRejectNullReturnDate() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> loan.markAsReturned(null)),
                    () -> assertTrue(loan.isActive())
            );
        }
    }

    /**
     * Testy sprawdzania przeterminowania.
     */
    @Nested
    @DisplayName("isOverdue")
    class OverdueTests {

        /**
         * Przeterminowanie zależy od dnia sprawdzenia względem terminu zwrotu (2026-03-31).
         *
         * @param today    dzień sprawdzenia
         * @param expected oczekiwany wynik
         */
        @ParameterizedTest(name = "dnia {0} wynik to {1}")
        @CsvSource({
                "2026-03-01, false",
                "2026-03-30, false",
                "2026-03-31, false",
                "2026-04-01, true",
                "2026-12-31, true"
        })
        void shouldCheckOverdueRelativeToDueDate(LocalDate today, boolean expected) {
            assertEquals(expected, loan.isOverdue(today));
        }

        /**
         * Zwrócone wypożyczenie nigdy nie jest przeterminowane, nawet po terminie.
         */
        @Test
        @DisplayName("zwrócone wypożyczenie nie jest przeterminowane")
        void shouldNotBeOverdueWhenReturned() {
            loan.markAsReturned(LocalDate.of(2026, 4, 10));

            assertFalse(loan.isOverdue(LocalDate.of(2026, 5, 1)));
        }

        /**
         * Brak daty sprawdzenia powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak daty sprawdzenia")
        void shouldRejectNullToday() {
            assertThrows(IllegalArgumentException.class, () -> loan.isOverdue(null));
        }
    }

    /**
     * Testy porównywania wypożyczeń i opisu tekstowego.
     */
    @Nested
    @DisplayName("equals, hashCode i toString")
    class EqualityTests {

        /**
         * Wypożyczenia o tym samym id są równe, nawet jeśli różnią się pozostałymi danymi.
         */
        @Test
        @DisplayName("wypożyczenia o tym samym id są równe i mają ten sam hashCode")
        void shouldBeEqualWhenIdsMatch() {
            UUID id = UUID.randomUUID();
            Loan first = new Loan(id, bookId, memberId, LOAN_DATE, DUE_DATE, null);
            Loan second = new Loan(id, UUID.randomUUID(), UUID.randomUUID(), LOAN_DATE, DUE_DATE, LOAN_DATE);

            assertAll(
                    () -> assertEquals(first, second),
                    () -> assertEquals(first.hashCode(), second.hashCode())
            );
        }

        /**
         * Wypożyczenia o identycznych danych, ale różnych id, nie są równe.
         */
        @Test
        @DisplayName("wypożyczenia o różnych id nie są równe, nawet przy identycznych danych")
        void shouldNotBeEqualWhenIdsDiffer() {
            Loan other = new Loan(bookId, memberId, LOAN_DATE);

            assertNotEquals(loan, other);
        }

        /**
         * Wypożyczenie nie jest równe {@code null} ani obiektowi innego typu.
         */
        @Test
        @DisplayName("nie jest równe null ani obiektowi innego typu")
        void shouldNotBeEqualToNullOrOtherType() {
            assertAll(
                    () -> assertFalse(loan.equals(null)),
                    () -> assertFalse(loan.equals("wypożyczenie"))
            );
        }

        /**
         * Opis tekstowy powinien zawierać identyfikatory i daty.
         */
        @Test
        @DisplayName("toString zawiera identyfikatory książki i czytelnika oraz termin zwrotu")
        void shouldContainKeyDataInToString() {
            String text = loan.toString();

            assertAll(
                    () -> assertTrue(text.contains(bookId.toString())),
                    () -> assertTrue(text.contains(memberId.toString())),
                    () -> assertTrue(text.contains(DUE_DATE.toString()))
            );
        }
    }
}
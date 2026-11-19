package pl.tajko.library.model;

import java.time.LocalDate;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wypożyczenie jednego egzemplarza książki przez czytelnika.
 * <p>
 * Wypożyczenie przechowuje tylko identyfikatory książki i czytelnika, a nie same obiekty.
 * Pełne dane pobiera serwis z repozytoriów.
 * <p>
 * Wypożyczenie jest <b>aktywne</b>, dopóki nie ma daty zwrotu ({@code returnDate == null}).
 * Termin zwrotu wynosi domyślnie {@value #LOAN_PERIOD_DAYS} dni od daty wypożyczenia.
 * Po utworzeniu można zmienić wyłącznie datę zwrotu, metodą {@link #markAsReturned(LocalDate)}.
 * <p>
 * Dwa wypożyczenia są równe, gdy mają ten sam identyfikator.
 */
public class Loan {

    /** Domyślna liczba dni, na które wypożycza się książkę. */
    public static final int LOAN_PERIOD_DAYS = 30;

    /** Unikalny identyfikator wypożyczenia. */
    private final UUID id;

    /** Identyfikator wypożyczonego egzemplarza książki. */
    private final UUID bookId;

    /** Identyfikator czytelnika, który wypożyczył książkę. */
    private final UUID memberId;

    /** Data wypożyczenia. */
    private final LocalDate loanDate;

    /** Termin zwrotu. */
    private final LocalDate dueDate;

    /** Data faktycznego zwrotu; {@code null} oznacza, że książka nie została jeszcze oddana. */
    private LocalDate returnDate;

    /**
     * Tworzy nowe, aktywne wypożyczenie z automatycznym identyfikatorem
     * i terminem zwrotu ustawionym na {@value #LOAN_PERIOD_DAYS} dni po dacie wypożyczenia.
     *
     * @param bookId   identyfikator egzemplarza książki, nie może być {@code null}
     * @param memberId identyfikator czytelnika, nie może być {@code null}
     * @param loanDate data wypożyczenia, nie może być {@code null}
     * @throws IllegalArgumentException jeśli którykolwiek argument jest {@code null}
     */
    public Loan(UUID bookId, UUID memberId, LocalDate loanDate) {
        this(UUID.randomUUID(), bookId, memberId, loanDate, calculateDueDate(loanDate), null);
    }

    /**
     * Tworzy wypożyczenie ze wszystkimi danymi.
     * Używany przy odczycie zapisanych danych, także dla wypożyczeń już zwróconych.
     *
     * @param id         identyfikator wypożyczenia, nie może być {@code null}
     * @param bookId     identyfikator egzemplarza książki, nie może być {@code null}
     * @param memberId   identyfikator czytelnika, nie może być {@code null}
     * @param loanDate   data wypożyczenia, nie może być {@code null}
     * @param dueDate    termin zwrotu, nie może być {@code null} ani wcześniejszy niż data wypożyczenia
     * @param returnDate data zwrotu lub {@code null}, jeśli książka nie została oddana;
     *                   nie może być wcześniejsza niż data wypożyczenia
     * @throws IllegalArgumentException jeśli którykolwiek argument jest niepoprawny
     */
    @JsonCreator
    public Loan(@JsonProperty("id") UUID id,
                @JsonProperty("bookId") UUID bookId,
                @JsonProperty("memberId") UUID memberId,
                @JsonProperty("loanDate") LocalDate loanDate,
                @JsonProperty("dueDate") LocalDate dueDate,
                @JsonProperty("returnDate") LocalDate returnDate) {
        this.id = requireNonNull(id, "Identyfikator wypożyczenia");
        this.bookId = requireNonNull(bookId, "Identyfikator książki");
        this.memberId = requireNonNull(memberId, "Identyfikator czytelnika");
        this.loanDate = requireNonNull(loanDate, "Data wypożyczenia");
        this.dueDate = requireNonNull(dueDate, "Termin zwrotu");
        if (dueDate.isBefore(loanDate)) {
            throw new IllegalArgumentException("Termin zwrotu nie może być wcześniejszy niż data wypożyczenia");
        }
        if (returnDate != null && returnDate.isBefore(loanDate)) {
            throw new IllegalArgumentException("Data zwrotu nie może być wcześniejsza niż data wypożyczenia");
        }
        this.returnDate = returnDate;
    }

    /**
     * Zwraca identyfikator wypożyczenia.
     *
     * @return identyfikator UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Zwraca identyfikator wypożyczonego egzemplarza książki.
     *
     * @return identyfikator książki
     */
    public UUID getBookId() {
        return bookId;
    }

    /**
     * Zwraca identyfikator czytelnika.
     *
     * @return identyfikator czytelnika
     */
    public UUID getMemberId() {
        return memberId;
    }

    /**
     * Zwraca datę wypożyczenia.
     *
     * @return data wypożyczenia
     */
    public LocalDate getLoanDate() {
        return loanDate;
    }

    /**
     * Zwraca termin zwrotu.
     *
     * @return termin zwrotu
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Zwraca datę faktycznego zwrotu.
     *
     * @return data zwrotu lub {@code null}, jeśli książka nie została jeszcze oddana
     */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    /**
     * Sprawdza, czy wypożyczenie jest aktywne, czyli książka nie została jeszcze oddana.
     *
     * @return {@code true}, jeśli nie ma daty zwrotu
     */
    @JsonIgnore
    public boolean isActive() {
        return returnDate == null;
    }

    /**
     * Sprawdza, czy aktywne wypożyczenie jest przeterminowane w podanym dniu.
     * Wypożyczenie jest przeterminowane, gdy podany dzień jest późniejszy niż termin zwrotu.
     * W dniu terminu zwrotu książka nie jest jeszcze przeterminowana.
     * Wypożyczenie już zwrócone nigdy nie jest uznawane za przeterminowane.
     * <p>
     * Data jest przekazywana jako argument (zamiast pobierania jej z zegara),
     * dzięki czemu metodę łatwo testować.
     *
     * @param today dzień, dla którego sprawdzamy przeterminowanie, nie może być {@code null}
     * @return {@code true}, jeśli wypożyczenie jest aktywne i minął termin zwrotu
     * @throws IllegalArgumentException jeśli {@code today} jest {@code null}
     */
    public boolean isOverdue(LocalDate today) {
        requireNonNull(today, "Data sprawdzenia");
        return isActive() && today.isAfter(dueDate);
    }

    /**
     * Oznacza wypożyczenie jako zwrócone w podanym dniu.
     *
     * @param returnDate data zwrotu, nie może być {@code null} ani wcześniejsza niż data wypożyczenia
     * @throws IllegalStateException    jeśli wypożyczenie zostało już zwrócone
     * @throws IllegalArgumentException jeśli data zwrotu jest niepoprawna
     */
    public void markAsReturned(LocalDate returnDate) {
        if (!isActive()) {
            throw new IllegalStateException("Wypożyczenie zostało już zwrócone");
        }
        requireNonNull(returnDate, "Data zwrotu");
        if (returnDate.isBefore(loanDate)) {
            throw new IllegalArgumentException("Data zwrotu nie może być wcześniejsza niż data wypożyczenia");
        }
        this.returnDate = returnDate;
    }

    /**
     * Porównuje wypożyczenia na podstawie identyfikatora.
     *
     * @param o porównywany obiekt
     * @return {@code true}, jeśli obiekt jest wypożyczeniem o tym samym identyfikatorze
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Loan other)) {
            return false;
        }
        return id.equals(other.id);
    }

    /**
     * Zwraca kod skrótu spójny z {@link #equals(Object)}, liczony z identyfikatora.
     *
     * @return kod skrótu
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Zwraca czytelny opis wypożyczenia, przydatny w logach i podczas debugowania.
     *
     * @return tekst ze wszystkimi polami
     */
    @Override
    public String toString() {
        return "Loan{id=" + id
                + ", bookId=" + bookId
                + ", memberId=" + memberId
                + ", loanDate=" + loanDate
                + ", dueDate=" + dueDate
                + ", returnDate=" + returnDate
                + '}';
    }

    /**
     * Wylicza termin zwrotu na podstawie daty wypożyczenia.
     *
     * @param loanDate data wypożyczenia
     * @return data wypożyczenia powiększona o {@value #LOAN_PERIOD_DAYS} dni
     * @throws IllegalArgumentException jeśli data wypożyczenia jest {@code null}
     */
    private static LocalDate calculateDueDate(LocalDate loanDate) {
        return requireNonNull(loanDate, "Data wypożyczenia").plusDays(LOAN_PERIOD_DAYS);
    }

    /**
     * Sprawdza, że wartość nie jest {@code null}, i zwraca ją.
     *
     * @param value     sprawdzana wartość
     * @param fieldName nazwa pola użyta w komunikacie błędu
     * @param <T>       typ wartości
     * @return ta sama wartość
     * @throws IllegalArgumentException jeśli wartość jest {@code null}
     */
    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " nie może być pusty");
        }
        return value;
    }
}
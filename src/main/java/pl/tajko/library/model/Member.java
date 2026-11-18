package pl.tajko.library.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Czytelnik zarejestrowany w bibliotece.
 * <p>
 * Czytelnik ma unikalny identyfikator {@link UUID}, imię, nazwisko, adres e-mail
 * oraz datę rejestracji. Data rejestracji nie zmienia się po utworzeniu obiektu.
 * <p>
 * Dwóch czytelników jest równych, gdy mają ten sam identyfikator.
 * Unikalność adresu e-mail pilnuje serwis, a nie ta klasa.
 */
public class Member {

    /** Uproszczony wzorzec adresu e-mail (nie obejmuje wszystkich przypadków z RFC 5322). */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    /** Unikalny identyfikator czytelnika, niezmienny po utworzeniu. */
    private final UUID id;

    /** Imię czytelnika. */
    private String firstName;

    /** Nazwisko czytelnika. */
    private String lastName;

    /** Znormalizowany adres e-mail (małe litery, bez spacji na brzegach). */
    private String email;

    /** Data rejestracji w bibliotece, niezmienna po utworzeniu. */
    private final LocalDate registeredAt;

    /**
     * Tworzy nowego czytelnika z automatycznie wygenerowanym identyfikatorem.
     *
     * @param firstName    imię, nie może być puste
     * @param lastName     nazwisko, nie może być puste
     * @param email        poprawny adres e-mail
     * @param registeredAt data rejestracji, nie może być {@code null} ani z przyszłości
     * @throws IllegalArgumentException jeśli którykolwiek argument jest niepoprawny
     */
    public Member(String firstName, String lastName, String email, LocalDate registeredAt) {
        this(UUID.randomUUID(), firstName, lastName, email, registeredAt);
    }

    /**
     * Tworzy czytelnika z podanym identyfikatorem.
     * Używany przy odczycie zapisanych danych, gdy identyfikator już istnieje.
     *
     * @param id           istniejący identyfikator, nie może być {@code null}
     * @param firstName    imię, nie może być puste
     * @param lastName     nazwisko, nie może być puste
     * @param email        poprawny adres e-mail
     * @param registeredAt data rejestracji, nie może być {@code null} ani z przyszłości
     * @throws IllegalArgumentException jeśli którykolwiek argument jest niepoprawny
     */
    public Member(UUID id, String firstName, String lastName, String email, LocalDate registeredAt) {
        if (id == null) {
            throw new IllegalArgumentException("Identyfikator nie może być pusty");
        }
        this.id = id;
        this.firstName = requireText(firstName, "Imię");
        this.lastName = requireText(lastName, "Nazwisko");
        this.email = requireValidEmail(email);
        this.registeredAt = requireValidRegistrationDate(registeredAt);
    }

    /**
     * Zwraca unikalny identyfikator czytelnika.
     *
     * @return identyfikator UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Zwraca imię czytelnika.
     *
     * @return imię
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Ustawia nowe imię. Wartość jest przycinana z białych znaków na początku i końcu.
     *
     * @param firstName nowe imię, nie może być puste
     * @throws IllegalArgumentException jeśli imię jest puste
     */
    public void setFirstName(String firstName) {
        this.firstName = requireText(firstName, "Imię");
    }

    /**
     * Zwraca nazwisko czytelnika.
     *
     * @return nazwisko
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Ustawia nowe nazwisko. Wartość jest przycinana z białych znaków na początku i końcu.
     *
     * @param lastName nowe nazwisko, nie może być puste
     * @throws IllegalArgumentException jeśli nazwisko jest puste
     */
    public void setLastName(String lastName) {
        this.lastName = requireText(lastName, "Nazwisko");
    }

    /**
     * Zwraca imię i nazwisko w jednym tekście, np. do wyświetlania w tabelach.
     *
     * @return tekst w formacie "Imię Nazwisko"
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Zwraca znormalizowany adres e-mail (małe litery).
     *
     * @return adres e-mail
     */
    public String getEmail() {
        return email;
    }

    /**
     * Ustawia nowy adres e-mail. Adres jest sprawdzany i zamieniany na małe litery.
     *
     * @param email poprawny adres e-mail
     * @throws IllegalArgumentException jeśli adres jest niepoprawny
     */
    public void setEmail(String email) {
        this.email = requireValidEmail(email);
    }

    /**
     * Zwraca datę rejestracji czytelnika w bibliotece.
     *
     * @return data rejestracji
     */
    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    /**
     * Porównuje czytelników na podstawie identyfikatora.
     *
     * @param o porównywany obiekt
     * @return {@code true}, jeśli obiekt jest czytelnikiem o tym samym identyfikatorze
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Member other)) {
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
     * Zwraca czytelny opis czytelnika, przydatny w logach i podczas debugowania.
     *
     * @return tekst ze wszystkimi polami
     */
    @Override
    public String toString() {
        return "Member{id=" + id
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", email='" + email + '\''
                + ", registeredAt=" + registeredAt
                + '}';
    }

    /**
     * Sprawdza, że tekst nie jest pusty, i zwraca go bez białych znaków na brzegach.
     *
     * @param value     sprawdzany tekst
     * @param fieldName nazwa pola użyta w komunikacie błędu
     * @return przycięty tekst
     * @throws IllegalArgumentException jeśli tekst jest {@code null} lub pusty
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " nie może być pusty");
        }
        return value.trim();
    }

    /**
     * Sprawdza poprawność adresu e-mail i zwraca go w postaci znormalizowanej.
     *
     * @param email sprawdzany adres
     * @return adres bez białych znaków na brzegach, zapisany małymi literami
     * @throws IllegalArgumentException jeśli adres jest {@code null} lub niepoprawny
     */
    private static String requireValidEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Adres e-mail nie może być pusty");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Niepoprawny adres e-mail: " + email);
        }
        return normalized;
    }

    /**
     * Sprawdza, że data rejestracji została podana i nie jest z przyszłości.
     *
     * @param date sprawdzana data
     * @return ta sama data, jeśli jest poprawna
     * @throws IllegalArgumentException jeśli data jest {@code null} lub z przyszłości
     */
    private static LocalDate requireValidRegistrationDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Data rejestracji nie może być pusta");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data rejestracji nie może być z przyszłości");
        }
        return date;
    }
}
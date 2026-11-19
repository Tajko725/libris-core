package pl.tajko.library.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testy jednostkowe klasy {@link Member}.
 */
@DisplayName("Member")
class MemberTest {

    /** Stała, przeszła data rejestracji używana w testach. */
    private static final LocalDate REGISTERED_AT = LocalDate.of(2025, 1, 15);

    /** Czytelnik tworzony przed każdym testem. */
    private Member member;

    /**
     * Tworzy świeżego, poprawnego czytelnika przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        member = new Member("Jan", "Kowalski", "jan.kowalski@example.com", REGISTERED_AT);
    }

    /**
     * Testy tworzenia czytelnika.
     */
    @Nested
    @DisplayName("konstruktor")
    class ConstructorTests {

        /**
         * Poprawne dane powinny trafić do pól czytelnika.
         */
        @Test
        @DisplayName("tworzy czytelnika z poprawnymi danymi")
        void shouldCreateMemberWithValidData() {
            assertAll(
                    () -> assertNotNull(member.getId()),
                    () -> assertEquals("Jan", member.getFirstName()),
                    () -> assertEquals("Kowalski", member.getLastName()),
                    () -> assertEquals("jan.kowalski@example.com", member.getEmail()),
                    () -> assertEquals(REGISTERED_AT, member.getRegisteredAt())
            );
        }

        /**
         * Dwóch czytelników o identycznych danych powinno mieć różne identyfikatory.
         */
        @Test
        @DisplayName("nadaje różne identyfikatory kolejnym czytelnikom")
        void shouldGenerateUniqueIds() {
            Member other = new Member("Jan", "Kowalski", "jan.kowalski@example.com", REGISTERED_AT);

            assertNotEquals(member.getId(), other.getId());
        }

        /**
         * Konstruktor z identyfikatorem powinien zachować przekazane id.
         */
        @Test
        @DisplayName("zachowuje identyfikator przekazany przy odczycie danych")
        void shouldKeepProvidedId() {
            UUID id = UUID.randomUUID();

            Member loaded = new Member(id, "Anna", "Nowak", "anna@example.com", REGISTERED_AT);

            assertEquals(id, loaded.getId());
        }

        /**
         * Imię i nazwisko powinny być przycięte, a e-mail zapisany małymi literami.
         */
        @Test
        @DisplayName("przycina spacje i zamienia e-mail na małe litery")
        void shouldNormalizeInput() {
            Member normalized = new Member("  Anna ", " Nowak  ", "  Anna.NOWAK@Example.PL ", REGISTERED_AT);

            assertAll(
                    () -> assertEquals("Anna", normalized.getFirstName()),
                    () -> assertEquals("Nowak", normalized.getLastName()),
                    () -> assertEquals("anna.nowak@example.pl", normalized.getEmail())
            );
        }

        /**
         * Dzisiejsza data rejestracji jest dozwolona.
         */
        @Test
        @DisplayName("akceptuje dzisiejszą datę rejestracji")
        void shouldAcceptTodayAsRegistrationDate() {
            assertDoesNotThrow(() ->
                    new Member("Jan", "Kowalski", "jan@example.com", LocalDate.now()));
        }

        /**
         * Puste lub nieustawione imię powinno być odrzucone.
         *
         * @param firstName niepoprawne imię
         */
        @ParameterizedTest(name = "odrzuca imię: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void shouldRejectBlankFirstName(String firstName) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Member(firstName, "Kowalski", "jan@example.com", REGISTERED_AT));
        }

        /**
         * Puste lub nieustawione nazwisko powinno być odrzucone.
         *
         * @param lastName niepoprawne nazwisko
         */
        @ParameterizedTest(name = "odrzuca nazwisko: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void shouldRejectBlankLastName(String lastName) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Member("Jan", lastName, "jan@example.com", REGISTERED_AT));
        }

        /**
         * Poprawne adresy e-mail w różnych zapisach powinny być zaakceptowane.
         *
         * @param email poprawny adres
         */
        @ParameterizedTest(name = "akceptuje e-mail: {0}")
        @ValueSource(strings = {
                "jan.kowalski@example.com",
                "JAN+biblioteka@Example.PL",
                "a_b@sub.domain.org"
        })
        void shouldAcceptValidEmail(String email) {
            assertDoesNotThrow(() -> new Member("Jan", "Kowalski", email, REGISTERED_AT));
        }

        /**
         * Niepoprawne adresy e-mail powinny być odrzucone.
         *
         * @param email niepoprawny adres
         */
        @ParameterizedTest(name = "odrzuca e-mail: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "abc", "a@b", "a@@b.pl", "@example.pl", "jan kowalski@example.pl", "jan@example."})
        void shouldRejectInvalidEmail(String email) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Member("Jan", "Kowalski", email, REGISTERED_AT));
        }

        /**
         * Brak daty rejestracji powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak daty rejestracji")
        void shouldRejectNullRegistrationDate() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Member("Jan", "Kowalski", "jan@example.com", null));
        }

        /**
         * Data rejestracji z przyszłości powinna być odrzucona.
         */
        @Test
        @DisplayName("odrzuca datę rejestracji z przyszłości")
        void shouldRejectFutureRegistrationDate() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            assertThrows(IllegalArgumentException.class,
                    () -> new Member("Jan", "Kowalski", "jan@example.com", tomorrow));
        }

        /**
         * Brak identyfikatora w konstruktorze odczytu powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak identyfikatora")
        void shouldRejectNullId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Member(null, "Jan", "Kowalski", "jan@example.com", REGISTERED_AT));
        }
    }

    /**
     * Testy metod zmieniających dane czytelnika.
     */
    @Nested
    @DisplayName("settery")
    class SetterTests {

        /**
         * Poprawne wartości powinny zostać zapisane i znormalizowane.
         */
        @Test
        @DisplayName("zmieniają dane na poprawne wartości")
        void shouldUpdateFieldsWithValidValues() {
            member.setFirstName(" Piotr ");
            member.setLastName(" Zieliński");
            member.setEmail("Piotr.Zielinski@Example.com");

            assertAll(
                    () -> assertEquals("Piotr", member.getFirstName()),
                    () -> assertEquals("Zieliński", member.getLastName()),
                    () -> assertEquals("piotr.zielinski@example.com", member.getEmail())
            );
        }

        /**
         * Po odrzuceniu niepoprawnej wartości czytelnik powinien zachować poprzedni stan.
         */
        @Test
        @DisplayName("niepoprawna wartość nie zmienia dotychczasowych danych")
        void shouldKeepOldValuesWhenSetterRejectsInput() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> member.setFirstName(" ")),
                    () -> assertThrows(IllegalArgumentException.class, () -> member.setLastName(null)),
                    () -> assertThrows(IllegalArgumentException.class, () -> member.setEmail("zly-adres")),
                    () -> assertEquals("Jan", member.getFirstName()),
                    () -> assertEquals("Kowalski", member.getLastName()),
                    () -> assertEquals("jan.kowalski@example.com", member.getEmail())
            );
        }
    }

    /**
     * Testy porównywania czytelników.
     */
    @Nested
    @DisplayName("equals i hashCode")
    class EqualityTests {

        /**
         * Czytelnicy o tym samym id są równi, nawet jeśli różnią się pozostałymi danymi.
         */
        @Test
        @DisplayName("czytelnicy o tym samym id są równi i mają ten sam hashCode")
        void shouldBeEqualWhenIdsMatch() {
            UUID id = UUID.randomUUID();
            Member first = new Member(id, "Jan", "Kowalski", "jan@example.com", REGISTERED_AT);
            Member second = new Member(id, "Anna", "Nowak", "anna@example.com", REGISTERED_AT);

            assertAll(
                    () -> assertEquals(first, second),
                    () -> assertEquals(first.hashCode(), second.hashCode())
            );
        }

        /**
         * Czytelnicy o identycznych danych, ale różnych id, nie są równi.
         */
        @Test
        @DisplayName("czytelnicy o różnych id nie są równi, nawet przy identycznych danych")
        void shouldNotBeEqualWhenIdsDiffer() {
            Member other = new Member("Jan", "Kowalski", "jan.kowalski@example.com", REGISTERED_AT);

            assertNotEquals(member, other);
        }

        /**
         * Czytelnik nie jest równy {@code null} ani obiektowi innego typu.
         */
        @Test
        @DisplayName("nie jest równy null ani obiektowi innego typu")
        void shouldNotBeEqualToNullOrOtherType() {
            assertAll(
                    () -> assertFalse(member.equals(null)),
                    () -> assertFalse(member.equals("Jan Kowalski"))
            );
        }
    }

    /**
     * Testy metod pomocniczych do wyświetlania.
     */
    @Nested
    @DisplayName("wyświetlanie")
    class DisplayTests {

        /**
         * Pełne imię i nazwisko powinno być połączone spacją.
         */
        @Test
        @DisplayName("getFullName łączy imię i nazwisko")
        void shouldReturnFullName() {
            assertEquals("Jan Kowalski", member.getFullName());
        }

        /**
         * Opis tekstowy powinien zawierać najważniejsze dane czytelnika.
         */
        @Test
        @DisplayName("toString zawiera nazwisko i e-mail")
        void shouldContainKeyDataInToString() {
            String text = member.toString();

            assertAll(
                    () -> assertEquals(true, text.contains("Kowalski")),
                    () -> assertEquals(true, text.contains("jan.kowalski@example.com"))
            );
        }
    }
}
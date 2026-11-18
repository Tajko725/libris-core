package pl.tajko.library.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy jednostkowe klasy {@link Book}.
 * <p>
 * Klasa jest prostym obiektem domenowym bez zależności, więc nie wymaga mocków.
 */
@DisplayName("Book")
class BookTest {

    /** Poprawny numer ISBN-13 używany w testach. */
    private static final String VALID_ISBN = "978-0-306-40615-7";

    /** Ten sam numer w postaci znormalizowanej. */
    private static final String NORMALIZED_ISBN = "9780306406157";

    /** Książka tworzona przed każdym testem. */
    private Book book;

    /**
     * Tworzy świeżą, poprawną książkę przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        book = new Book("Pan Tadeusz", "Adam Mickiewicz", 1834, VALID_ISBN, Genre.NOVEL);
    }

    /**
     * Testy tworzenia książki.
     */
    @Nested
    @DisplayName("konstruktor")
    class ConstructorTests {

        /**
         * Poprawne dane powinny trafić do pól książki.
         */
        @Test
        @DisplayName("tworzy książkę z poprawnymi danymi")
        void shouldCreateBookWithValidData() {
            assertAll(
                    () -> assertNotNullId(book),
                    () -> assertEquals("Pan Tadeusz", book.getTitle()),
                    () -> assertEquals("Adam Mickiewicz", book.getAuthor()),
                    () -> assertEquals(1834, book.getPublicationYear()),
                    () -> assertEquals(NORMALIZED_ISBN, book.getIsbn()),
                    () -> assertEquals(Genre.NOVEL, book.getGenre())
            );
        }

        /**
         * Dwa egzemplarze tego samego tytułu powinny mieć różne identyfikatory.
         */
        @Test
        @DisplayName("nadaje różne identyfikatory dwóm egzemplarzom tej samej książki")
        void shouldGenerateUniqueIds() {
            Book copy = new Book("Pan Tadeusz", "Adam Mickiewicz", 1834, VALID_ISBN, Genre.NOVEL);

            assertNotEquals(book.getId(), copy.getId());
        }

        /**
         * Konstruktor z identyfikatorem powinien zachować przekazane id.
         */
        @Test
        @DisplayName("zachowuje identyfikator przekazany przy odczycie danych")
        void shouldKeepProvidedId() {
            UUID id = UUID.randomUUID();

            Book loaded = new Book(id, "Dziady", "Adam Mickiewicz", 1823, VALID_ISBN, Genre.NOVEL);

            assertEquals(id, loaded.getId());
        }

        /**
         * Białe znaki na początku i końcu tytułu oraz autora powinny zostać usunięte.
         */
        @Test
        @DisplayName("przycina spacje w tytule i autorze")
        void shouldTrimTitleAndAuthor() {
            Book trimmed = new Book("  Dziady  ", "  Adam Mickiewicz ", 1823, VALID_ISBN, Genre.NOVEL);

            assertAll(
                    () -> assertEquals("Dziady", trimmed.getTitle()),
                    () -> assertEquals("Adam Mickiewicz", trimmed.getAuthor())
            );
        }

        /**
         * Graniczne lata (najwcześniejszy i bieżący) powinny być akceptowane.
         */
        @Test
        @DisplayName("akceptuje graniczne lata wydania")
        void shouldAcceptBoundaryYears() {
            int currentYear = Year.now().getValue();

            assertAll(
                    () -> assertDoesNotThrow(() ->
                            new Book("Biblia", "Autor", Book.MIN_PUBLICATION_YEAR, VALID_ISBN, Genre.OTHER)),
                    () -> assertDoesNotThrow(() ->
                            new Book("Nowość", "Autor", currentYear, VALID_ISBN, Genre.OTHER))
            );
        }

        /**
         * Pusty, biały lub nieustawiony tytuł powinien być odrzucony.
         *
         * @param title niepoprawny tytuł
         */
        @ParameterizedTest(name = "odrzuca tytuł: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "   "})
        void shouldRejectBlankTitle(String title) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book(title, "Autor", 2000, VALID_ISBN, Genre.NOVEL));
        }

        /**
         * Pusty, biały lub nieustawiony autor powinien być odrzucony.
         *
         * @param author niepoprawny autor
         */
        @ParameterizedTest(name = "odrzuca autora: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void shouldRejectBlankAuthor(String author) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("Tytuł", author, 2000, VALID_ISBN, Genre.NOVEL));
        }

        /**
         * Rok przed wynalezieniem druku powinien być odrzucony.
         *
         * @param year niepoprawny rok
         */
        @ParameterizedTest(name = "odrzuca rok: {0}")
        @ValueSource(ints = {Integer.MIN_VALUE, -5, 0, 1449})
        void shouldRejectYearBeforeMinimum(int year) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("Tytuł", "Autor", year, VALID_ISBN, Genre.NOVEL));
        }

        /**
         * Rok z przyszłości powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca rok z przyszłości")
        void shouldRejectFutureYear() {
            int nextYear = Year.now().getValue() + 1;

            assertThrows(IllegalArgumentException.class,
                    () -> new Book("Tytuł", "Autor", nextYear, VALID_ISBN, Genre.NOVEL));
        }

        /**
         * Niepoprawny numer ISBN powinien być odrzucony.
         *
         * @param isbn niepoprawny numer
         */
        @ParameterizedTest(name = "odrzuca ISBN: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"123", "9780306406158", "abcdefghij"})
        void shouldRejectInvalidIsbn(String isbn) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("Tytuł", "Autor", 2000, isbn, Genre.NOVEL));
        }

        /**
         * Brak gatunku powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak gatunku")
        void shouldRejectNullGenre() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book("Tytuł", "Autor", 2000, VALID_ISBN, null));
        }

        /**
         * Brak identyfikatora w konstruktorze odczytu powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca brak identyfikatora")
        void shouldRejectNullId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Book(null, "Tytuł", "Autor", 2000, VALID_ISBN, Genre.NOVEL));
        }
    }

    /**
     * Testy metod zmieniających dane książki.
     */
    @Nested
    @DisplayName("settery")
    class SetterTests {

        /**
         * Poprawne wartości powinny zostać zapisane.
         */
        @Test
        @DisplayName("zmieniają dane na poprawne wartości")
        void shouldUpdateFieldsWithValidValues() {
            book.setTitle("  Dziady ");
            book.setAuthor("Juliusz Słowacki");
            book.setPublicationYear(1840);
            book.setIsbn("0-306-40615-2");
            book.setGenre(Genre.HISTORY);

            assertAll(
                    () -> assertEquals("Dziady", book.getTitle()),
                    () -> assertEquals("Juliusz Słowacki", book.getAuthor()),
                    () -> assertEquals(1840, book.getPublicationYear()),
                    () -> assertEquals("0306406152", book.getIsbn()),
                    () -> assertEquals(Genre.HISTORY, book.getGenre())
            );
        }

        /**
         * Po odrzuceniu niepoprawnej wartości książka powinna zachować poprzedni stan.
         */
        @Test
        @DisplayName("niepoprawna wartość nie zmienia dotychczasowych danych")
        void shouldKeepOldValuesWhenSetterRejectsInput() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> book.setTitle(" ")),
                    () -> assertThrows(IllegalArgumentException.class, () -> book.setAuthor(null)),
                    () -> assertThrows(IllegalArgumentException.class, () -> book.setPublicationYear(1000)),
                    () -> assertThrows(IllegalArgumentException.class, () -> book.setIsbn("123")),
                    () -> assertThrows(IllegalArgumentException.class, () -> book.setGenre(null)),
                    () -> assertEquals("Pan Tadeusz", book.getTitle()),
                    () -> assertEquals("Adam Mickiewicz", book.getAuthor()),
                    () -> assertEquals(1834, book.getPublicationYear()),
                    () -> assertEquals(NORMALIZED_ISBN, book.getIsbn()),
                    () -> assertEquals(Genre.NOVEL, book.getGenre())
            );
        }
    }

    /**
     * Testy porównywania książek.
     */
    @Nested
    @DisplayName("equals i hashCode")
    class EqualityTests {

        /**
         * Książki o tym samym id są równe, nawet jeśli różnią się pozostałymi danymi.
         */
        @Test
        @DisplayName("książki o tym samym id są równe i mają ten sam hashCode")
        void shouldBeEqualWhenIdsMatch() {
            UUID id = UUID.randomUUID();
            Book first = new Book(id, "Tytuł A", "Autor A", 2000, VALID_ISBN, Genre.NOVEL);
            Book second = new Book(id, "Tytuł B", "Autor B", 2010, "0306406152", Genre.HISTORY);

            assertAll(
                    () -> assertEquals(first, second),
                    () -> assertEquals(first.hashCode(), second.hashCode())
            );
        }

        /**
         * Dwa egzemplarze o identycznych danych, ale różnych id, nie są równe.
         */
        @Test
        @DisplayName("egzemplarze o różnych id nie są równe, nawet przy identycznych danych")
        void shouldNotBeEqualWhenIdsDiffer() {
            Book copy = new Book("Pan Tadeusz", "Adam Mickiewicz", 1834, VALID_ISBN, Genre.NOVEL);

            assertNotEquals(book, copy);
        }

        /**
         * Książka nie jest równa {@code null} ani obiektowi innego typu.
         */
        @Test
        @DisplayName("nie jest równa null ani obiektowi innego typu")
        void shouldNotBeEqualToNullOrOtherType() {
            assertAll(
                    () -> assertFalse(book.equals(null)),
                    () -> assertFalse(book.equals("Pan Tadeusz"))
            );
        }

        /**
         * Zbiór oparty o hashCode powinien rozpoznawać tę samą książkę.
         */
        @Test
        @DisplayName("działa poprawnie w HashSet")
        void shouldWorkInHashSet() {
            Set<Book> books = new HashSet<>();
            books.add(book);
            books.add(book);

            assertAll(
                    () -> assertEquals(1, books.size()),
                    () -> assertTrue(books.contains(book))
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
         * Skrócony identyfikator to pierwsze 8 znaków pełnego UUID.
         */
        @Test
        @DisplayName("getShortId zwraca pierwsze 8 znaków UUID")
        void shouldReturnShortId() {
            assertAll(
                    () -> assertEquals(8, book.getShortId().length()),
                    () -> assertTrue(book.getId().toString().startsWith(book.getShortId()))
            );
        }

        /**
         * Opis tekstowy powinien zawierać najważniejsze dane książki.
         */
        @Test
        @DisplayName("toString zawiera tytuł, autora i ISBN")
        void shouldContainKeyDataInToString() {
            String text = book.toString();

            assertAll(
                    () -> assertTrue(text.contains("Pan Tadeusz")),
                    () -> assertTrue(text.contains("Adam Mickiewicz")),
                    () -> assertTrue(text.contains(NORMALIZED_ISBN))
            );
        }
    }

    /**
     * Pomocnicza asercja sprawdzająca, że książka ma identyfikator.
     *
     * @param value sprawdzana książka
     */
    private static void assertNotNullId(Book value) {
        assertTrue(value.getId() != null, "Książka powinna mieć identyfikator");
    }
}
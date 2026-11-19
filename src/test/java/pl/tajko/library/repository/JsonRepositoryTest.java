package pl.tajko.library.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.tajko.library.model.Book;
import pl.tajko.library.model.Genre;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy jednostkowe klasy {@link JsonRepository}.
 * <p>
 * Testy używają prawdziwych plików w katalogu tymczasowym ({@link TempDir}), usuwanym automatycznie
 * po każdym teście, więc nie wymagają mocków i nie dotykają prawdziwych danych.
 */
@DisplayName("JsonRepository")
class JsonRepositoryTest {

    /** Poprawny numer ISBN używany w testach. */
    private static final String ISBN = "9780306406157";

    /** Poprawny wpis książki w pliku JSON, zapisany ręcznie. */
    private static final String VALID_BOOK_JSON = """
            {"id":"123e4567-e89b-12d3-a456-426614174000","title":"Dziady","author":"Adam Mickiewicz",\
            "publicationYear":1823,"isbn":"9780306406157","genre":"NOVEL"}""";

    /** Wpis książki z niepoprawnym numerem ISBN. */
    private static final String INVALID_ISBN_BOOK_JSON = """
            {"id":"123e4567-e89b-12d3-a456-426614174001","title":"Dziady","author":"Adam Mickiewicz",\
            "publicationYear":1823,"isbn":"123","genre":"NOVEL"}""";

    /** Katalog tymczasowy tworzony przez JUnit dla każdego testu. */
    @TempDir
    Path tempDir;

    /** Ścieżka do pliku z książkami; katalog nadrzędny jeszcze nie istnieje. */
    private Path booksFile;

    /** Repozytorium książek tworzone przed każdym testem. */
    private JsonRepository<Book> repository;

    /**
     * Tworzy puste repozytorium książek przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        booksFile = tempDir.resolve("data").resolve("books.json");
        repository = createBookRepository();
    }

    /**
     * Testy startu repozytorium bez pliku.
     */
    @Nested
    @DisplayName("start bez pliku")
    class EmptyStartTests {

        /**
         * Brak pliku oznacza puste repozytorium i nie tworzy pliku.
         */
        @Test
        @DisplayName("jest puste i nie tworzy pliku, dopóki nic nie zapisano")
        void shouldStartEmptyWithoutCreatingFile() {
            assertAll(
                    () -> assertTrue(repository.findAll().isEmpty()),
                    () -> assertFalse(Files.exists(booksFile))
            );
        }
    }

    /**
     * Testy zapisu i odczytu.
     */
    @Nested
    @DisplayName("zapis i odczyt")
    class SaveAndLoadTests {

        /**
         * Zapisana książka powinna być dostępna po identyfikatorze.
         */
        @Test
        @DisplayName("zapisana książka jest dostępna po id")
        void shouldFindSavedBook() {
            Book book = newBook("Dziady");

            repository.save(book);

            assertEquals(book, repository.findById(book.getId()).orElseThrow());
        }

        /**
         * Pierwszy zapis powinien utworzyć brakujący katalog i plik.
         */
        @Test
        @DisplayName("pierwszy zapis tworzy katalog i plik")
        void shouldCreateDirectoryAndFileOnFirstSave() {
            repository.save(newBook("Dziady"));

            assertTrue(Files.exists(booksFile));
        }

        /**
         * Po zapisie nie powinien zostać plik tymczasowy.
         */
        @Test
        @DisplayName("nie zostawia pliku tymczasowego")
        void shouldNotLeaveTempFile() {
            repository.save(newBook("Dziady"));

            assertFalse(Files.exists(booksFile.resolveSibling("books.json.tmp")));
        }

        /**
         * Nieistniejący identyfikator powinien dać pusty wynik.
         */
        @Test
        @DisplayName("zwraca pusty wynik dla nieznanego id")
        void shouldReturnEmptyForUnknownId() {
            assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
        }

        /**
         * Zapis obiektu o istniejącym id powinien go zastąpić, a nie dodać drugi.
         */
        @Test
        @DisplayName("ponowny zapis tego samego id zastępuje obiekt")
        void shouldReplaceExistingItem() {
            Book book = newBook("Stary tytuł");
            repository.save(book);

            book.setTitle("Nowy tytuł");
            repository.save(book);

            assertAll(
                    () -> assertEquals(1, repository.findAll().size()),
                    () -> assertEquals("Nowy tytuł", createBookRepository()
                            .findById(book.getId()).orElseThrow().getTitle())
            );
        }

        /**
         * Kolejność zwracanych obiektów powinna odpowiadać kolejności dodania.
         */
        @Test
        @DisplayName("findAll zwraca obiekty w kolejności dodania")
        void shouldKeepInsertionOrder() {
            Book first = newBook("Pierwsza");
            Book second = newBook("Druga");
            Book third = newBook("Trzecia");
            repository.save(first);
            repository.save(second);
            repository.save(third);

            assertEquals(List.of(first, second, third), createBookRepository().findAll());
        }

        /**
         * Lista zwracana z repozytorium nie powinna pozwalać na zmiany z zewnątrz.
         */
        @Test
        @DisplayName("findAll zwraca listę niemodyfikowalną")
        void shouldReturnUnmodifiableList() {
            Book book = newBook("Dziady");
            List<Book> all = repository.findAll();

            assertThrows(UnsupportedOperationException.class, () -> all.add(book));
        }

        /**
         * Po ponownym otwarciu pliku wszystkie pola książki powinny być zachowane.
         */
        @Test
        @DisplayName("dane przetrwają ponowne otwarcie pliku")
        void shouldPersistAllFieldsAcrossReopen() {
            Book book = newBook("Dziady");
            repository.save(book);

            Book loaded = createBookRepository().findById(book.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals("Dziady", loaded.getTitle()),
                    () -> assertEquals("Adam Mickiewicz", loaded.getAuthor()),
                    () -> assertEquals(1823, loaded.getPublicationYear()),
                    () -> assertEquals(ISBN, loaded.getIsbn()),
                    () -> assertEquals(Genre.NOVEL, loaded.getGenre())
            );
        }

        /**
         * Zapis pustego obiektu powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca zapis null")
        void shouldRejectNullItem() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }
    }

    /**
     * Testy usuwania.
     */
    @Nested
    @DisplayName("usuwanie")
    class DeleteTests {

        /**
         * Usunięcie istniejącej książki powinno zwrócić true i zapisać zmianę w pliku.
         */
        @Test
        @DisplayName("usuwa istniejącą książkę, także z pliku")
        void shouldDeleteExistingBook() {
            Book kept = newBook("Zostaje");
            Book removed = newBook("Do usunięcia");
            repository.save(kept);
            repository.save(removed);

            boolean result = repository.deleteById(removed.getId());

            assertAll(
                    () -> assertTrue(result),
                    () -> assertTrue(repository.findById(removed.getId()).isEmpty()),
                    () -> assertEquals(List.of(kept), createBookRepository().findAll())
            );
        }

        /**
         * Usunięcie nieistniejącej książki powinno zwrócić false.
         */
        @Test
        @DisplayName("zwraca false dla nieistniejącego id")
        void shouldReturnFalseForUnknownId() {
            assertFalse(repository.deleteById(UUID.randomUUID()));
        }
    }

    /**
     * Testy odczytu plików uszkodzonych lub przygotowanych ręcznie.
     */
    @Nested
    @DisplayName("odczyt z pliku")
    class FileReadingTests {

        /**
         * Ręcznie przygotowany, poprawny plik powinien zostać wczytany.
         *
         * @throws IOException jeśli nie uda się przygotować pliku
         */
        @Test
        @DisplayName("wczytuje ręcznie przygotowany plik")
        void shouldLoadHandWrittenFile() throws IOException {
            writeBooksFile("[" + VALID_BOOK_JSON + "]");

            Book loaded = createBookRepository().findAll().get(0);

            assertAll(
                    () -> assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), loaded.getId()),
                    () -> assertEquals("Dziady", loaded.getTitle()),
                    () -> assertEquals(Genre.NOVEL, loaded.getGenre())
            );
        }

        /**
         * Plik, który nie jest poprawnym JSON-em, powinien zgłosić błąd z nazwą pliku.
         *
         * @throws IOException jeśli nie uda się przygotować pliku
         */
        @Test
        @DisplayName("zgłasza błąd dla uszkodzonego pliku")
        void shouldFailOnCorruptedFile() throws IOException {
            writeBooksFile("to nie jest json");

            RepositoryException exception = assertThrows(RepositoryException.class, () -> createBookRepository());

            assertTrue(exception.getMessage().contains("books.json"));
        }

        /**
         * Plik z danymi niezgodnymi z regułami modelu (np. zły ISBN) powinien zgłosić błąd.
         *
         * @throws IOException jeśli nie uda się przygotować pliku
         */
        @Test
        @DisplayName("zgłasza błąd dla danych niezgodnych z regułami modelu")
        void shouldFailOnInvalidData() throws IOException {
            writeBooksFile("[" + INVALID_ISBN_BOOK_JSON + "]");

            assertThrows(RepositoryException.class, () -> createBookRepository());
        }

        /**
         * Plik z dwoma wpisami o tym samym identyfikatorze powinien zgłosić błąd.
         *
         * @throws IOException jeśli nie uda się przygotować pliku
         */
        @Test
        @DisplayName("zgłasza błąd dla powtórzonych identyfikatorów")
        void shouldFailOnDuplicateIds() throws IOException {
            writeBooksFile("[" + VALID_BOOK_JSON + "," + VALID_BOOK_JSON + "]");

            assertThrows(RepositoryException.class, () -> createBookRepository());
        }
    }

    /**
     * Testy formatu zapisu i pozostałych typów modelu.
     */
    @Nested
    @DisplayName("format JSON i pozostałe modele")
    class FormatTests {

        /**
         * W pliku nie powinny pojawić się pola pochodne, a gatunek ma być zapisany nazwą.
         *
         * @throws IOException jeśli nie uda się odczytać pliku
         */
        @Test
        @DisplayName("zapisuje gatunek nazwą i pomija pola pochodne")
        void shouldWriteGenreNameAndSkipDerivedFields() throws IOException {
            repository.save(newBook("Dziady"));

            String json = Files.readString(booksFile);

            assertAll(
                    () -> assertTrue(json.contains("NOVEL")),
                    () -> assertFalse(json.contains("shortId"))
            );
        }

        /**
         * Wypożyczenia, aktywne i zwrócone, powinny przetrwać zapis i odczyt, a daty mają format ISO.
         *
         * @throws IOException jeśli nie uda się odczytać pliku
         */
        @Test
        @DisplayName("zapisuje i odczytuje wypożyczenia (aktywne i zwrócone)")
        void shouldPersistLoans() throws IOException {
            Path loansFile = tempDir.resolve("loans.json");
            JsonRepository<Loan> loans = new JsonRepository<>(loansFile, Loan.class, Loan::getId);
            LocalDate loanDate = LocalDate.of(2026, 3, 1);
            Loan active = new Loan(UUID.randomUUID(), UUID.randomUUID(), loanDate);
            Loan returned = new Loan(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    loanDate, LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 10));
            loans.save(active);
            loans.save(returned);

            JsonRepository<Loan> reopened = new JsonRepository<>(loansFile, Loan.class, Loan::getId);
            Loan loadedActive = reopened.findById(active.getId()).orElseThrow();
            Loan loadedReturned = reopened.findById(returned.getId()).orElseThrow();
            String json = Files.readString(loansFile);

            assertAll(
                    () -> assertEquals(loanDate, loadedActive.getLoanDate()),
                    () -> assertEquals(LocalDate.of(2026, 3, 31), loadedActive.getDueDate()),
                    () -> assertNull(loadedActive.getReturnDate()),
                    () -> assertTrue(loadedActive.isActive()),
                    () -> assertEquals(LocalDate.of(2026, 3, 10), loadedReturned.getReturnDate()),
                    () -> assertFalse(loadedReturned.isActive()),
                    () -> assertEquals(returned.getBookId(), loadedReturned.getBookId()),
                    () -> assertEquals(returned.getMemberId(), loadedReturned.getMemberId()),
                    () -> assertTrue(json.contains("2026-03-01")),
                    () -> assertFalse(json.contains("\"active\""))
            );
        }

        /**
         * Czytelnicy powinni przetrwać zapis i odczyt, a pole pochodne nie powinno trafić do pliku.
         *
         * @throws IOException jeśli nie uda się odczytać pliku
         */
        @Test
        @DisplayName("zapisuje i odczytuje czytelników")
        void shouldPersistMembers() throws IOException {
            Path membersFile = tempDir.resolve("members.json");
            JsonRepository<Member> members = new JsonRepository<>(membersFile, Member.class, Member::getId);
            Member member = new Member("Jan", "Kowalski", "Jan.Kowalski@Example.com", LocalDate.of(2025, 1, 15));
            members.save(member);

            Member loaded = new JsonRepository<>(membersFile, Member.class, Member::getId)
                    .findById(member.getId()).orElseThrow();
            String json = Files.readString(membersFile);

            assertAll(
                    () -> assertEquals("Jan", loaded.getFirstName()),
                    () -> assertEquals("Kowalski", loaded.getLastName()),
                    () -> assertEquals("jan.kowalski@example.com", loaded.getEmail()),
                    () -> assertEquals(LocalDate.of(2025, 1, 15), loaded.getRegisteredAt()),
                    () -> assertFalse(json.contains("fullName"))
            );
        }
    }

    /**
     * Tworzy repozytorium książek korzystające z pliku testowego.
     *
     * @return nowe repozytorium wczytujące aktualną zawartość pliku
     */
    private JsonRepository<Book> createBookRepository() {
        return new JsonRepository<>(booksFile, Book.class, Book::getId);
    }

    /**
     * Tworzy poprawną książkę o podanym tytule.
     *
     * @param title tytuł książki
     * @return nowa książka z losowym identyfikatorem
     */
    private static Book newBook(String title) {
        return new Book(title, "Adam Mickiewicz", 1823, ISBN, Genre.NOVEL);
    }

    /**
     * Zapisuje podaną treść do pliku z książkami, tworząc katalog, jeśli go brakuje.
     *
     * @param content treść pliku
     * @throws IOException jeśli zapis się nie powiedzie
     */
    private void writeBooksFile(String content) throws IOException {
        Files.createDirectories(booksFile.getParent());
        Files.writeString(booksFile, content);
    }
}
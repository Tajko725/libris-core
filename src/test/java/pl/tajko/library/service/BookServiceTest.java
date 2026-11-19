package pl.tajko.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.exception.EntityNotFoundException;
import pl.tajko.library.model.Book;
import pl.tajko.library.model.Genre;
import pl.tajko.library.model.Loan;
import pl.tajko.library.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe klasy {@link BookService}.
 * <p>
 * Repozytoria są zastąpione atrapami Mockito: testy sprawdzają wyłącznie logikę serwisu,
 * bez zapisu do plików. Atrapa domyślnie zwraca puste listy, więc {@code when(...)} ustawiamy tylko tam,
 * gdzie test potrzebuje konkretnych danych.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService")
class BookServiceTest {

    /** Poprawne, różne numery ISBN używane w testach. */
    private static final String ISBN_A = "9780306406157";
    private static final String ISBN_B = "9780134685991";
    private static final String ISBN_C = "0306406152";
    private static final String ISBN_D = "080442957X";

    /** Atrapa repozytorium książek. */
    @Mock
    private Repository<Book> bookRepository;

    /** Atrapa repozytorium wypożyczeń. */
    @Mock
    private Repository<Loan> loanRepository;

    /** Testowany serwis. */
    private BookService service;

    /**
     * Tworzy serwis z atrapami przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        service = new BookService(bookRepository, loanRepository);
    }

    /**
     * Testy konstruktora serwisu.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste repozytoria")
    void shouldRejectNullRepositories() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new BookService(null, loanRepository)),
                () -> assertThrows(IllegalArgumentException.class, () -> new BookService(bookRepository, null))
        );
    }

    /**
     * Testy dodawania książek.
     */
    @Nested
    @DisplayName("addBook")
    class AddBookTests {

        /**
         * Serwis powinien zapisać żądaną liczbę egzemplarzy z różnymi identyfikatorami.
         */
        @Test
        @DisplayName("dodaje podaną liczbę egzemplarzy z różnymi identyfikatorami")
        void shouldAddRequestedNumberOfCopies() {
            List<Book> result = service.addBook("Wiedźmin", "Andrzej Sapkowski", 1993,
                    "978-0-306-40615-7", Genre.FANTASY, 3);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository, times(3)).save(captor.capture());
            assertAll(
                    () -> assertEquals(3, result.size()),
                    () -> assertEquals(result, captor.getAllValues()),
                    () -> assertEquals(3, result.stream().map(Book::getId).distinct().count()),
                    () -> assertTrue(result.stream().allMatch(book -> book.getIsbn().equals(ISBN_A))),
                    () -> assertTrue(result.stream().allMatch(book -> book.getTitle().equals("Wiedźmin")))
            );
        }

        /**
         * Dodanie egzemplarza książki, która już jest w bibliotece z tymi samymi danymi, jest dozwolone.
         */
        @Test
        @DisplayName("pozwala dodać egzemplarze istniejącej książki o identycznych danych")
        void shouldAllowCopiesWithIdenticalDetails() {
            when(bookRepository.findAll()).thenReturn(List.of(book("Wiedźmin", "Andrzej Sapkowski", ISBN_A)));

            List<Book> result = service.addBook("Wiedźmin", "Andrzej Sapkowski", 1993, ISBN_A, Genre.FANTASY, 1);

            assertEquals(1, result.size());
            verify(bookRepository, times(1)).save(any(Book.class));
        }

        /**
         * Ten sam ISBN z innym tytułem powinien być odrzucony, bez żadnego zapisu.
         */
        @Test
        @DisplayName("odrzuca ten sam ISBN z innymi danymi i niczego nie zapisuje")
        void shouldRejectSameIsbnWithDifferentDetails() {
            when(bookRepository.findAll()).thenReturn(List.of(book("Wiedźmin", "Andrzej Sapkowski", ISBN_A)));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                    () -> service.addBook("Inny tytuł", "Andrzej Sapkowski", 1993, ISBN_A, Genre.FANTASY, 2));

            assertAll(
                    () -> assertTrue(exception.getMessage().contains("Wiedźmin")),
                    () -> verify(bookRepository, never()).save(any(Book.class))
            );
        }

        /**
         * Liczba egzemplarzy poza zakresem powinna być odrzucona przed jakąkolwiek operacją.
         *
         * @param copies niepoprawna liczba egzemplarzy
         */
        @ParameterizedTest(name = "odrzuca liczbę egzemplarzy: {0}")
        @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 101})
        void shouldRejectInvalidCopiesCount(int copies) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.addBook("Wiedźmin", "Andrzej Sapkowski", 1993, ISBN_A, Genre.FANTASY, copies));

            verifyNoInteractions(bookRepository);
        }

        /**
         * Niepoprawne dane książki (np. ISBN) powinny być odrzucone przed jakąkolwiek operacją.
         */
        @Test
        @DisplayName("odrzuca niepoprawny ISBN i niczego nie zapisuje")
        void shouldRejectInvalidIsbn() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.addBook("Wiedźmin", "Andrzej Sapkowski", 1993, "123", Genre.FANTASY, 2));

            verifyNoInteractions(bookRepository);
        }
    }

    /**
     * Testy dodawania kolejnych egzemplarzy istniejącej książki.
     */
    @Nested
    @DisplayName("addCopies")
    class AddCopiesTests {

        /**
         * Nowe egzemplarze powinny mieć takie same dane jak istniejące i nowe identyfikatory.
         */
        @Test
        @DisplayName("dodaje egzemplarze o tych samych danych i nowych identyfikatorach")
        void shouldAddCopiesOfExistingBook() {
            Book existing = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            when(bookRepository.findAll()).thenReturn(List.of(existing));

            List<Book> result = service.addCopies("978-0-306-40615-7", 2);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository, times(2)).save(captor.capture());
            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> assertEquals(result, captor.getAllValues()),
                    () -> assertTrue(result.stream().noneMatch(book -> book.getId().equals(existing.getId()))),
                    () -> assertTrue(result.stream().allMatch(book -> book.getTitle().equals("Wiedźmin")))
            );
        }

        /**
         * Nieznany ISBN powinien zgłosić błąd "nie znaleziono".
         */
        @Test
        @DisplayName("zgłasza błąd dla nieznanego ISBN")
        void shouldFailForUnknownIsbn() {
            assertAll(
                    () -> assertThrows(EntityNotFoundException.class, () -> service.addCopies(ISBN_A, 1)),
                    () -> verify(bookRepository, never()).save(any(Book.class))
            );
        }

        /**
         * Niepoprawny ISBN lub liczba egzemplarzy powinny być odrzucone.
         */
        @Test
        @DisplayName("odrzuca niepoprawny ISBN i niepoprawną liczbę egzemplarzy")
        void shouldRejectInvalidArguments() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> service.addCopies("123", 1)),
                    () -> assertThrows(IllegalArgumentException.class, () -> service.addCopies(ISBN_A, 0)),
                    () -> verify(bookRepository, never()).save(any(Book.class))
            );
        }
    }

    /**
     * Testy edycji szczegółów książki.
     */
    @Nested
    @DisplayName("updateBookDetails")
    class UpdateBookDetailsTests {

        /**
         * Zmiana szczegółów powinna objąć wszystkie egzemplarze danego ISBN i je zapisać.
         */
        @Test
        @DisplayName("zmienia dane wszystkich egzemplarzy i zapisuje każdy z nich")
        void shouldUpdateAllCopies() {
            Book first = book("Stary tytuł", "Stary autor", ISBN_A);
            Book second = book("Stary tytuł", "Stary autor", ISBN_A);
            Book other = book("Inna książka", "Inny autor", ISBN_B);
            when(bookRepository.findAll()).thenReturn(List.of(first, second, other));

            int updated = service.updateBookDetails(ISBN_A, " Nowy tytuł ", "Nowy autor", 1999, Genre.HISTORY);

            assertAll(
                    () -> assertEquals(2, updated),
                    () -> assertEquals("Nowy tytuł", first.getTitle()),
                    () -> assertEquals("Nowy tytuł", second.getTitle()),
                    () -> assertEquals("Nowy autor", second.getAuthor()),
                    () -> assertEquals(1999, first.getPublicationYear()),
                    () -> assertEquals(Genre.HISTORY, first.getGenre()),
                    () -> assertEquals("Inna książka", other.getTitle()),
                    () -> verify(bookRepository).save(first),
                    () -> verify(bookRepository).save(second),
                    () -> verify(bookRepository, never()).save(other)
            );
        }

        /**
         * Niepoprawne nowe dane nie powinny zmienić żadnego egzemplarza.
         */
        @Test
        @DisplayName("niepoprawne dane nie zmieniają żadnego egzemplarza")
        void shouldNotChangeAnythingWhenNewDataIsInvalid() {
            Book first = book("Stary tytuł", "Stary autor", ISBN_A);
            Book second = book("Stary tytuł", "Stary autor", ISBN_A);
            when(bookRepository.findAll()).thenReturn(List.of(first, second));

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> service.updateBookDetails(ISBN_A, "Nowy tytuł", " ", 1999, Genre.HISTORY)),
                    () -> assertEquals("Stary tytuł", first.getTitle()),
                    () -> assertEquals("Stary tytuł", second.getTitle()),
                    () -> verify(bookRepository, never()).save(any(Book.class))
            );
        }

        /**
         * Nieznany ISBN powinien zgłosić błąd "nie znaleziono".
         */
        @Test
        @DisplayName("zgłasza błąd dla nieznanego ISBN")
        void shouldFailForUnknownIsbn() {
            assertThrows(EntityNotFoundException.class,
                    () -> service.updateBookDetails(ISBN_A, "Tytuł", "Autor", 2000, Genre.NOVEL));
        }
    }

    /**
     * Testy usuwania egzemplarza.
     */
    @Nested
    @DisplayName("deleteCopy")
    class DeleteCopyTests {

        /**
         * Egzemplarz bez wypożyczeń powinien zostać usunięty.
         */
        @Test
        @DisplayName("usuwa egzemplarz bez wypożyczeń")
        void shouldDeleteCopyWithoutLoans() {
            Book book = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

            service.deleteCopy(book.getId());

            verify(bookRepository).deleteById(book.getId());
        }

        /**
         * Zwrócone wypożyczenie nie powinno blokować usunięcia egzemplarza.
         */
        @Test
        @DisplayName("pozwala usunąć egzemplarz, którego wypożyczenia są już zwrócone")
        void shouldDeleteCopyWithOnlyReturnedLoans() {
            Book book = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
            when(loanRepository.findAll()).thenReturn(List.of(returnedLoan(book)));

            service.deleteCopy(book.getId());

            verify(bookRepository).deleteById(book.getId());
        }

        /**
         * Egzemplarz z aktywnym wypożyczeniem nie może zostać usunięty.
         */
        @Test
        @DisplayName("odrzuca usunięcie wypożyczonego egzemplarza")
        void shouldRejectDeletingBorrowedCopy() {
            Book book = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
            when(loanRepository.findAll()).thenReturn(List.of(activeLoan(book)));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class, () -> service.deleteCopy(book.getId())),
                    () -> verify(bookRepository, never()).deleteById(any(UUID.class))
            );
        }

        /**
         * Nieistniejący egzemplarz powinien zgłosić błąd i nie sprawdzać wypożyczeń.
         */
        @Test
        @DisplayName("zgłasza błąd dla nieistniejącego egzemplarza")
        void shouldFailForUnknownCopy() {
            UUID id = UUID.randomUUID();
            when(bookRepository.findById(id)).thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(EntityNotFoundException.class, () -> service.deleteCopy(id)),
                    () -> verify(bookRepository, never()).deleteById(any(UUID.class)),
                    () -> verifyNoInteractions(loanRepository)
            );
        }
    }

    /**
     * Testy katalogu (widoku zgrupowanego po ISBN).
     */
    @Nested
    @DisplayName("getCatalog")
    class CatalogTests {

        /**
         * Pusta biblioteka powinna dać pusty katalog.
         */
        @Test
        @DisplayName("zwraca pusty katalog, gdy nie ma książek")
        void shouldReturnEmptyCatalog() {
            assertTrue(service.getCatalog().isEmpty());
        }

        /**
         * Egzemplarze o tym samym ISBN powinny zostać zgrupowane z poprawnymi liczbami.
         */
        @Test
        @DisplayName("grupuje egzemplarze po ISBN i liczy dostępne oraz wypożyczone")
        void shouldGroupCopiesAndCountAvailability() {
            Book witcher1 = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            Book witcher2 = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            Book witcher3 = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            Book doll = book("Lalka", "Bolesław Prus", ISBN_B);
            when(bookRepository.findAll()).thenReturn(List.of(witcher1, witcher2, witcher3, doll));
            when(loanRepository.findAll()).thenReturn(List.of(activeLoan(witcher1), returnedLoan(witcher2)));

            List<BookSummary> catalog = service.getCatalog();

            assertAll(
                    () -> assertEquals(2, catalog.size()),
                    () -> assertEquals("Lalka", catalog.get(0).title()),
                    () -> assertEquals(1, catalog.get(0).totalCopies()),
                    () -> assertEquals(1, catalog.get(0).availableCopies()),
                    () -> assertEquals("Wiedźmin", catalog.get(1).title()),
                    () -> assertEquals(3, catalog.get(1).totalCopies()),
                    () -> assertEquals(2, catalog.get(1).availableCopies()),
                    () -> assertEquals(1, catalog.get(1).borrowedCopies())
            );
        }

        /**
         * Katalog powinien być posortowany według polskiego alfabetu (ł po l, ż po z).
         */
        @Test
        @DisplayName("sortuje według polskiego alfabetu")
        void shouldSortByPolishAlphabet() {
            when(bookRepository.findAll()).thenReturn(List.of(
                    book("Żeglarz", "Autor", ISBN_A),
                    book("Zbrodnia i kara", "Autor", ISBN_B),
                    book("Łaskawe", "Autor", ISBN_C),
                    book("Lalka", "Autor", ISBN_D)));

            List<String> titles = service.getCatalog().stream().map(BookSummary::title).toList();

            assertEquals(List.of("Lalka", "Łaskawe", "Zbrodnia i kara", "Żeglarz"), titles);
        }
    }

    /**
     * Testy wyszukiwania.
     */
    @Nested
    @DisplayName("search")
    class SearchTests {

        /**
         * Ustawia w atrapie repozytorium katalog z trzema książkami.
         * Wywoływana tylko w testach, które naprawdę odczytują książki (ścisłe stuby Mockito
         * zgłaszają błąd, gdy ustawione zachowanie nie zostanie użyte).
         */
        private void stubCatalog() {
            when(bookRepository.findAll()).thenReturn(List.of(
                    book("Wiedźmin", "Andrzej Sapkowski", ISBN_A),
                    book("Lalka", "Bolesław Prus", ISBN_B),
                    book("Łódź w ruchu", "Jan Kowalski", ISBN_C)));
        }

        /**
         * Wyszukiwanie po tytule powinno ignorować wielkość liter i polskie znaki.
         */
        @Test
        @DisplayName("znajduje po tytule bez względu na wielkość liter i polskie znaki")
        void shouldFindByTitleIgnoringCaseAndDiacritics() {
            stubCatalog();

            List<BookSummary> result = service.search("WIEDZMIN");

            assertEquals(List.of("Wiedźmin"), result.stream().map(BookSummary::title).toList());
        }

        /**
         * Litera ł powinna być traktowana jak l.
         */
        @Test
        @DisplayName("traktuje ł jak l")
        void shouldTreatPolishLAsPlainL() {
            stubCatalog();

            List<BookSummary> result = service.search("lodz");

            assertEquals(List.of("Łódź w ruchu"), result.stream().map(BookSummary::title).toList());
        }

        /**
         * Wyszukiwanie po autorze powinno działać na fragmencie imienia lub nazwiska.
         */
        @Test
        @DisplayName("znajduje po autorze")
        void shouldFindByAuthor() {
            stubCatalog();

            List<BookSummary> result = service.search("boleslaw");

            assertEquals(List.of("Lalka"), result.stream().map(BookSummary::title).toList());
        }

        /**
         * Wyszukiwanie po ISBN powinno ignorować myślniki.
         */
        @Test
        @DisplayName("znajduje po numerze ISBN z myślnikami")
        void shouldFindByIsbnWithHyphens() {
            stubCatalog();

            List<BookSummary> result = service.search("978-0-134");

            assertEquals(List.of("Lalka"), result.stream().map(BookSummary::title).toList());
        }

        /**
         * Brak dopasowań powinien dać pustą listę.
         */
        @Test
        @DisplayName("zwraca pustą listę, gdy nic nie pasuje")
        void shouldReturnEmptyListWhenNothingMatches() {
            stubCatalog();

            assertTrue(service.search("nieistniejaca fraza").isEmpty());
        }

        /**
         * Pusta fraza powinna być odrzucona jeszcze przed odczytem książek.
         *
         * @param query niepoprawna fraza
         */
        @ParameterizedTest(name = "odrzuca frazę: [{0}]")
        @ValueSource(strings = {"", " ", "\t"})
        void shouldRejectBlankQuery(String query) {
            assertThrows(IllegalArgumentException.class, () -> service.search(query));

            verifyNoInteractions(bookRepository);
        }
    }

    /**
     * Testy pobierania egzemplarzy i sprawdzania dostępności.
     */
    @Nested
    @DisplayName("egzemplarze i dostępność")
    class CopiesTests {

        /**
         * Metoda powinna zwrócić tylko egzemplarze danego ISBN.
         */
        @Test
        @DisplayName("getCopies zwraca tylko egzemplarze danego ISBN")
        void shouldReturnCopiesOfGivenIsbn() {
            Book first = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            Book second = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            Book other = book("Lalka", "Bolesław Prus", ISBN_B);
            when(bookRepository.findAll()).thenReturn(List.of(first, second, other));

            assertEquals(List.of(first, second), service.getCopies("978-0-306-40615-7"));
        }

        /**
         * Nieznany ISBN powinien zgłosić błąd, a niepoprawny ISBN błąd argumentu.
         */
        @Test
        @DisplayName("getCopies zgłasza błąd dla nieznanego i niepoprawnego ISBN")
        void shouldFailForUnknownOrInvalidIsbn() {
            assertAll(
                    () -> assertThrows(EntityNotFoundException.class, () -> service.getCopies(ISBN_A)),
                    () -> assertThrows(IllegalArgumentException.class, () -> service.getCopies("123"))
            );
        }

        /**
         * Egzemplarz bez aktywnego wypożyczenia jest dostępny, a z aktywnym nie.
         */
        @Test
        @DisplayName("isCopyAvailable rozróżnia egzemplarz wolny i wypożyczony")
        void shouldTellAvailableFromBorrowed() {
            Book free = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            Book borrowed = book("Wiedźmin", "Andrzej Sapkowski", ISBN_A);
            when(bookRepository.findById(free.getId())).thenReturn(Optional.of(free));
            when(bookRepository.findById(borrowed.getId())).thenReturn(Optional.of(borrowed));
            when(loanRepository.findAll()).thenReturn(List.of(activeLoan(borrowed), returnedLoan(free)));

            assertAll(
                    () -> assertTrue(service.isCopyAvailable(free.getId())),
                    () -> assertFalse(service.isCopyAvailable(borrowed.getId()))
            );
        }

        /**
         * Nieistniejący egzemplarz powinien zgłosić błąd.
         */
        @Test
        @DisplayName("isCopyAvailable zgłasza błąd dla nieistniejącego egzemplarza")
        void shouldFailForUnknownCopy() {
            UUID id = UUID.randomUUID();
            when(bookRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.isCopyAvailable(id));
        }
    }

    /**
     * Tworzy poprawną książkę (egzemplarz) o podanych danych.
     *
     * @param title  tytuł
     * @param author autor
     * @param isbn   numer ISBN
     * @return nowy egzemplarz z losowym identyfikatorem
     */
    private static Book book(String title, String author, String isbn) {
        return new Book(title, author, 1993, isbn, Genre.FANTASY);
    }

    /**
     * Tworzy aktywne wypożyczenie podanego egzemplarza.
     *
     * @param book wypożyczany egzemplarz
     * @return aktywne wypożyczenie
     */
    private static Loan activeLoan(Book book) {
        return new Loan(book.getId(), UUID.randomUUID(), LocalDate.of(2026, 3, 1));
    }

    /**
     * Tworzy zwrócone wypożyczenie podanego egzemplarza.
     *
     * @param book wypożyczony wcześniej egzemplarz
     * @return zwrócone wypożyczenie
     */
    private static Loan returnedLoan(Book book) {
        Loan loan = new Loan(book.getId(), UUID.randomUUID(), LocalDate.of(2026, 3, 1));
        loan.markAsReturned(LocalDate.of(2026, 3, 10));
        return loan;
    }
}
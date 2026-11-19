package pl.tajko.library.service;

import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.exception.EntityNotFoundException;
import pl.tajko.library.model.Book;
import pl.tajko.library.model.Genre;
import pl.tajko.library.model.Loan;
import pl.tajko.library.repository.Repository;
import pl.tajko.library.util.TextNormalizer;
import pl.tajko.library.validation.IsbnValidator;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serwis zarządzający książkami w bibliotece.
 * <p>
 * Każdy obiekt {@link Book} to jeden fizyczny egzemplarz. Egzemplarze tej samej książki
 * rozpoznajemy po wspólnym numerze ISBN i muszą mieć identyczny tytuł, autora, rok wydania i gatunek.
 * Serwis pilnuje tej spójności, więc edycja szczegółów zmienia wszystkie egzemplarze danego ISBN naraz.
 * <p>
 * Dostępność egzemplarza wynika z wypożyczeń: egzemplarz jest wypożyczony, jeśli istnieje jego aktywne
 * wypożyczenie ({@link Loan#isActive()}).
 * <p>
 * Zależności serwis dostaje przez konstruktor, dzięki czemu w testach można je zastąpić atrapami.
 */
public class BookService {

    /** Największa liczba egzemplarzy dodawana jedną operacją (zabezpieczenie przed pomyłką). */
    public static final int MAX_COPIES_PER_OPERATION = 100;

    /** Porównywacz tekstów według polskiego alfabetu (ł po l, ż po z). */
    private static final Collator POLISH_COLLATOR = Collator.getInstance(Locale.of("pl", "PL"));

    /** Kolejność w katalogu: alfabetycznie po tytule, potem po autorze. */
    private static final Comparator<BookSummary> CATALOG_ORDER = Comparator
            .comparing(BookSummary::title, POLISH_COLLATOR)
            .thenComparing(BookSummary::author, POLISH_COLLATOR);

    /** Repozytorium egzemplarzy książek. */
    private final Repository<Book> bookRepository;

    /** Repozytorium wypożyczeń, używane do sprawdzania dostępności egzemplarzy. */
    private final Repository<Loan> loanRepository;

    /**
     * Tworzy serwis korzystający z podanych repozytoriów.
     *
     * @param bookRepository repozytorium książek, nie może być {@code null}
     * @param loanRepository repozytorium wypożyczeń, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któreś repozytorium jest {@code null}
     */
    public BookService(Repository<Book> bookRepository, Repository<Loan> loanRepository) {
        if (bookRepository == null || loanRepository == null) {
            throw new IllegalArgumentException("Repozytoria nie mogą być puste");
        }
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    /**
     * Dodaje do biblioteki podaną liczbę egzemplarzy książki.
     * Jeśli książka o tym ISBN już istnieje, jej dane muszą być identyczne z podanymi.
     * Wszystkie dane są sprawdzane przed pierwszym zapisem.
     *
     * @param title           tytuł
     * @param author          autor
     * @param publicationYear rok wydania
     * @param isbn            numer ISBN-10 lub ISBN-13
     * @param genre           gatunek
     * @param copies          liczba egzemplarzy, od 1 do {@value #MAX_COPIES_PER_OPERATION}
     * @return lista dodanych egzemplarzy
     * @throws IllegalArgumentException jeśli dane lub liczba egzemplarzy są niepoprawne
     * @throws BusinessRuleException    jeśli książka o tym ISBN istnieje z innymi danymi
     */
    public List<Book> addBook(String title, String author, int publicationYear, String isbn,
                              Genre genre, int copies) {
        requireValidCopiesCount(copies);
        Book first = new Book(title, author, publicationYear, isbn, genre);
        findCopiesByIsbn(first.getIsbn()).stream()
                .findFirst()
                .ifPresent(existing -> requireSameDetails(existing, first));

        List<Book> created = new ArrayList<>(copies);
        created.add(first);
        for (int i = 1; i < copies; i++) {
            created.add(copyOf(first));
        }
        created.forEach(bookRepository::save);
        return List.copyOf(created);
    }

    /**
     * Dodaje kolejne egzemplarze książki, która już istnieje w bibliotece.
     *
     * @param isbn   numer ISBN istniejącej książki
     * @param copies liczba nowych egzemplarzy, od 1 do {@value #MAX_COPIES_PER_OPERATION}
     * @return lista dodanych egzemplarzy
     * @throws IllegalArgumentException jeśli ISBN lub liczba egzemplarzy są niepoprawne
     * @throws EntityNotFoundException  jeśli w bibliotece nie ma książki o tym ISBN
     */
    public List<Book> addCopies(String isbn, int copies) {
        requireValidCopiesCount(copies);
        List<Book> existing = requireCopies(requireValidIsbn(isbn));
        Book template = existing.get(0);

        List<Book> created = new ArrayList<>(copies);
        for (int i = 0; i < copies; i++) {
            created.add(copyOf(template));
        }
        created.forEach(bookRepository::save);
        return List.copyOf(created);
    }

    /**
     * Zmienia dane książki we wszystkich jej egzemplarzach (o podanym ISBN).
     * Nowe dane są sprawdzane przed zmianą pierwszego egzemplarza.
     * Numeru ISBN nie można zmienić tą metodą.
     *
     * @param isbn            numer ISBN edytowanej książki
     * @param title           nowy tytuł
     * @param author          nowy autor
     * @param publicationYear nowy rok wydania
     * @param genre           nowy gatunek
     * @return liczba zaktualizowanych egzemplarzy
     * @throws IllegalArgumentException jeśli ISBN lub nowe dane są niepoprawne
     * @throws EntityNotFoundException  jeśli w bibliotece nie ma książki o tym ISBN
     */
    public int updateBookDetails(String isbn, String title, String author, int publicationYear, Genre genre) {
        List<Book> copies = requireCopies(requireValidIsbn(isbn));
        // Obiekt pomocniczy: jego konstruktor sprawdza nowe dane, zanim zmienimy jakikolwiek egzemplarz.
        Book validated = new Book(title, author, publicationYear, copies.get(0).getIsbn(), genre);
        for (Book copy : copies) {
            copy.setTitle(validated.getTitle());
            copy.setAuthor(validated.getAuthor());
            copy.setPublicationYear(validated.getPublicationYear());
            copy.setGenre(validated.getGenre());
            bookRepository.save(copy);
        }
        return copies.size();
    }

    /**
     * Usuwa jeden egzemplarz książki. Egzemplarz z aktywnym wypożyczeniem nie może zostać usunięty.
     * Zwrócone wypożyczenia pozostają w historii.
     *
     * @param bookId identyfikator egzemplarza
     * @throws EntityNotFoundException jeśli egzemplarz nie istnieje
     * @throws BusinessRuleException   jeśli egzemplarz jest aktualnie wypożyczony
     */
    public void deleteCopy(UUID bookId) {
        Book book = requireBook(bookId);
        if (hasActiveLoan(book.getId())) {
            throw new BusinessRuleException(
                    "Egzemplarz " + book.getShortId() + " jest wypożyczony i nie można go usunąć");
        }
        bookRepository.deleteById(book.getId());
    }

    /**
     * Zwraca katalog: jedną pozycję na każdy numer ISBN, z liczbą egzemplarzy razem i dostępnych.
     * Pozycje są posortowane alfabetycznie według polskiego alfabetu.
     *
     * @return lista podsumowań, pusta jeśli biblioteka nie ma książek
     */
    public List<BookSummary> getCatalog() {
        Set<UUID> borrowedIds = findBorrowedBookIds();
        Map<String, List<Book>> groups = bookRepository.findAll().stream()
                .collect(Collectors.groupingBy(Book::getIsbn, LinkedHashMap::new, Collectors.toList()));
        return groups.values().stream()
                .map(copies -> toSummary(copies, borrowedIds))
                .sorted(CATALOG_ORDER)
                .toList();
    }

    /**
     * Wyszukuje książki po fragmencie tytułu, autora lub numeru ISBN.
     * Wielkość liter i polskie znaki diakrytyczne są ignorowane, a w numerze ISBN myślniki i spacje nie mają znaczenia.
     *
     * @param query szukana fraza, nie może być pusta
     * @return pasujące pozycje katalogu, puste jeśli nic nie pasuje
     * @throws IllegalArgumentException jeśli fraza jest pusta
     */
    public List<BookSummary> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Fraza wyszukiwania nie może być pusta");
        }
        String text = TextNormalizer.normalize(query);
        String isbnPart = IsbnValidator.normalize(query);
        return getCatalog().stream()
                .filter(summary -> matches(summary, text, isbnPart))
                .toList();
    }

    /**
     * Zwraca wszystkie egzemplarze książki o podanym ISBN.
     *
     * @param isbn numer ISBN
     * @return lista egzemplarzy, nigdy pusta
     * @throws IllegalArgumentException jeśli ISBN jest niepoprawny
     * @throws EntityNotFoundException  jeśli w bibliotece nie ma książki o tym ISBN
     */
    public List<Book> getCopies(String isbn) {
        return requireCopies(requireValidIsbn(isbn));
    }

    /**
     * Sprawdza, czy egzemplarz jest dostępny, czyli nie ma aktywnego wypożyczenia.
     *
     * @param bookId identyfikator egzemplarza
     * @return {@code true}, jeśli egzemplarz można wypożyczyć
     * @throws EntityNotFoundException jeśli egzemplarz nie istnieje
     */
    public boolean isCopyAvailable(UUID bookId) {
        Book book = requireBook(bookId);
        return !hasActiveLoan(book.getId());
    }

    /**
     * Sprawdza, czy podsumowanie pasuje do szukanej frazy (tytuł, autor lub ISBN).
     *
     * @param summary  sprawdzane podsumowanie
     * @param text     znormalizowana fraza do porównania z tytułem i autorem
     * @param isbnPart znormalizowana fraza do porównania z numerem ISBN
     * @return {@code true}, jeśli którekolwiek pole pasuje
     */
    private static boolean matches(BookSummary summary, String text, String isbnPart) {
        return TextNormalizer.normalize(summary.title()).contains(text)
                || TextNormalizer.normalize(summary.author()).contains(text)
                || (!isbnPart.isEmpty() && summary.isbn().contains(isbnPart));
    }

    /**
     * Buduje podsumowanie z listy egzemplarzy tej samej książki.
     *
     * @param copies      egzemplarze o tym samym ISBN, niepusta lista
     * @param borrowedIds identyfikatory aktualnie wypożyczonych egzemplarzy
     * @return podsumowanie z liczbą egzemplarzy razem i dostępnych
     */
    private static BookSummary toSummary(List<Book> copies, Set<UUID> borrowedIds) {
        Book first = copies.get(0);
        int available = (int) copies.stream()
                .filter(copy -> !borrowedIds.contains(copy.getId()))
                .count();
        return new BookSummary(first.getIsbn(), first.getTitle(), first.getAuthor(),
                first.getPublicationYear(), first.getGenre(), copies.size(), available);
    }

    /**
     * Tworzy nowy egzemplarz z takimi samymi danymi jak wzorzec i nowym identyfikatorem.
     *
     * @param template egzemplarz wzorcowy
     * @return nowy egzemplarz
     */
    private static Book copyOf(Book template) {
        return new Book(template.getTitle(), template.getAuthor(), template.getPublicationYear(),
                template.getIsbn(), template.getGenre());
    }

    /**
     * Sprawdza, że dane kandydata są identyczne z danymi istniejącego egzemplarza o tym samym ISBN.
     *
     * @param existing  egzemplarz już istniejący w bibliotece
     * @param candidate egzemplarz, który chcemy dodać
     * @throws BusinessRuleException jeśli dane się różnią
     */
    private static void requireSameDetails(Book existing, Book candidate) {
        boolean same = existing.getTitle().equals(candidate.getTitle())
                && existing.getAuthor().equals(candidate.getAuthor())
                && existing.getPublicationYear() == candidate.getPublicationYear()
                && existing.getGenre() == candidate.getGenre();
        if (!same) {
            throw new BusinessRuleException("ISBN " + existing.getIsbn() + " jest już w bibliotece jako \""
                    + existing.getTitle() + "\" (" + existing.getAuthor() + ", "
                    + existing.getPublicationYear() + ", " + existing.getGenre().getDisplayName()
                    + "). Dane muszą być identyczne albo dodaj kolejny egzemplarz tej książki.");
        }
    }

    /**
     * Sprawdza, że liczba egzemplarzy mieści się w dopuszczalnym zakresie.
     *
     * @param copies sprawdzana liczba
     * @throws IllegalArgumentException jeśli liczba jest poza zakresem
     */
    private static void requireValidCopiesCount(int copies) {
        if (copies < 1 || copies > MAX_COPIES_PER_OPERATION) {
            throw new IllegalArgumentException(
                    "Liczba egzemplarzy musi mieścić się w zakresie 1-" + MAX_COPIES_PER_OPERATION);
        }
    }

    /**
     * Sprawdza poprawność numeru ISBN i zwraca go w postaci znormalizowanej.
     *
     * @param isbn sprawdzany numer
     * @return numer bez myślników i spacji
     * @throws IllegalArgumentException jeśli numer jest niepoprawny
     */
    private static String requireValidIsbn(String isbn) {
        if (!IsbnValidator.isValid(isbn)) {
            throw new IllegalArgumentException("Niepoprawny numer ISBN: " + isbn);
        }
        return IsbnValidator.normalize(isbn);
    }

    /**
     * Zwraca egzemplarze o podanym ISBN lub zgłasza błąd, jeśli ich nie ma.
     *
     * @param normalizedIsbn znormalizowany numer ISBN
     * @return niepusta lista egzemplarzy
     * @throws EntityNotFoundException jeśli nie ma ani jednego egzemplarza
     */
    private List<Book> requireCopies(String normalizedIsbn) {
        List<Book> copies = findCopiesByIsbn(normalizedIsbn);
        if (copies.isEmpty()) {
            throw new EntityNotFoundException("Nie ma w bibliotece książki o numerze ISBN " + normalizedIsbn);
        }
        return copies;
    }

    /**
     * Zwraca egzemplarze o podanym ISBN.
     *
     * @param normalizedIsbn znormalizowany numer ISBN
     * @return lista egzemplarzy, może być pusta
     */
    private List<Book> findCopiesByIsbn(String normalizedIsbn) {
        return bookRepository.findAll().stream()
                .filter(book -> book.getIsbn().equals(normalizedIsbn))
                .toList();
    }

    /**
     * Zwraca egzemplarz o podanym identyfikatorze lub zgłasza błąd, jeśli nie istnieje.
     *
     * @param bookId identyfikator egzemplarza
     * @return znaleziony egzemplarz
     * @throws EntityNotFoundException jeśli egzemplarz nie istnieje
     */
    private Book requireBook(UUID bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono egzemplarza o identyfikatorze " + bookId));
    }

    /**
     * Sprawdza, czy egzemplarz ma aktywne wypożyczenie.
     *
     * @param bookId identyfikator egzemplarza
     * @return {@code true}, jeśli egzemplarz jest wypożyczony
     */
    private boolean hasActiveLoan(UUID bookId) {
        return loanRepository.findAll().stream()
                .anyMatch(loan -> loan.isActive() && loan.getBookId().equals(bookId));
    }

    /**
     * Zwraca identyfikatory wszystkich aktualnie wypożyczonych egzemplarzy.
     *
     * @return zbiór identyfikatorów
     */
    private Set<UUID> findBorrowedBookIds() {
        return loanRepository.findAll().stream()
                .filter(Loan::isActive)
                .map(Loan::getBookId)
                .collect(Collectors.toSet());
    }
}
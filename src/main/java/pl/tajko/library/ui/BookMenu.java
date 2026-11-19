package pl.tajko.library.ui;

import pl.tajko.library.model.Book;
import pl.tajko.library.model.Genre;
import pl.tajko.library.service.BookService;
import pl.tajko.library.service.BookSummary;

import java.time.Year;
import java.util.List;
import java.util.Optional;

/**
 * Menu konsolowe katalogu książek.
 * <p>
 * Katalog pokazuje książki zgrupowane po numerze ISBN (jeden wiersz na tytuł, z liczbą wolnych i wszystkich
 * egzemplarzy). Po wybraniu numeru pozycji widać poszczególne egzemplarze z krótkim identyfikatorem
 * i stanem (wolny lub wypożyczony). Błędy reguł biblioteki są wypisywane jako komunikaty, a menu działa dalej.
 */
public class BookMenu {

    /** Konsola, z której menu czyta i na którą pisze. */
    private final ConsoleIO io;

    /** Serwis książek. */
    private final BookService bookService;

    /**
     * Tworzy menu korzystające z podanej konsoli i serwisu.
     *
     * @param io          konsola, nie może być {@code null}
     * @param bookService serwis książek, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś argument jest {@code null}
     */
    public BookMenu(ConsoleIO io, BookService bookService) {
        if (io == null || bookService == null) {
            throw new IllegalArgumentException("Konsola i serwis nie mogą być puste");
        }
        this.io = io;
        this.bookService = bookService;
    }

    /**
     * Wyświetla menu i obsługuje wybory użytkownika, aż wybierze powrót.
     */
    public void run() {
        while (true) {
            io.println();
            io.println("--- Katalog książek ---");
            io.println("1. Pokaż katalog");
            io.println("2. Szukaj książki");
            io.println("3. Dodaj książkę");
            io.println("4. Dodaj egzemplarze");
            io.println("5. Edytuj dane książki");
            io.println("6. Usuń egzemplarz");
            io.println("0. Powrót");
            int choice = io.readInt("Wybór: ", 0, 6);
            switch (choice) {
                case 1 -> MenuSupport.runSafely(io, this::showCatalog);
                case 2 -> MenuSupport.runSafely(io, this::search);
                case 3 -> MenuSupport.runSafely(io, this::addBook);
                case 4 -> MenuSupport.runSafely(io, this::addCopies);
                case 5 -> MenuSupport.runSafely(io, this::editBook);
                case 6 -> MenuSupport.runSafely(io, this::deleteCopy);
                case 0 -> {
                    return;
                }
            }
        }
    }

    /**
     * Pokazuje cały katalog z możliwością podejrzenia egzemplarzy.
     */
    private void showCatalog() {
        browse(bookService.getCatalog(), "Katalog jest pusty.");
    }

    /**
     * Wyszukuje książki po frazie i pokazuje wyniki z możliwością podejrzenia egzemplarzy.
     */
    private void search() {
        String query = io.readLine("Fraza: ");
        browse(bookService.search(query), "Nic nie znaleziono.");
    }

    /**
     * Dodaje nową książkę w podanej liczbie egzemplarzy.
     */
    private void addBook() {
        String title = io.readLine("Tytuł: ");
        String author = io.readLine("Autor: ");
        int year = io.readInt("Rok wydania (" + Book.MIN_PUBLICATION_YEAR + "-" + currentYear() + "): ",
                Book.MIN_PUBLICATION_YEAR, currentYear());
        String isbn = io.readLine("ISBN: ");
        Genre genre = pickGenre(null);
        int copies = io.readInt("Liczba egzemplarzy (1-" + BookService.MAX_COPIES_PER_OPERATION + "): ",
                1, BookService.MAX_COPIES_PER_OPERATION);

        List<Book> added = bookService.addBook(title, author, year, isbn, genre, copies);
        io.println("Dodano " + added.size() + " egz. książki \"" + title + "\".");
    }

    /**
     * Dodaje kolejne egzemplarze wybranej książki.
     */
    private void addCopies() {
        Optional<BookSummary> selected = selectTitle("Numer książki");
        if (selected.isEmpty()) {
            return;
        }
        BookSummary book = selected.get();
        int count = io.readInt("Ile egzemplarzy dodać (1-" + BookService.MAX_COPIES_PER_OPERATION + "): ",
                1, BookService.MAX_COPIES_PER_OPERATION);

        List<Book> added = bookService.addCopies(book.isbn(), count);
        io.println("Dodano " + added.size() + " egz. książki \"" + book.title() + "\".");
    }

    /**
     * Zmienia dane wybranej książki (we wszystkich jej egzemplarzach). Enter zostawia dotychczasową wartość.
     */
    private void editBook() {
        Optional<BookSummary> selected = selectTitle("Numer książki");
        if (selected.isEmpty()) {
            return;
        }
        BookSummary book = selected.get();
        io.println("Zmiana danych dotyczy wszystkich egzemplarzy. Enter zostawia wartość w nawiasie.");
        String title = io.readLineOrDefault("Tytuł", book.title());
        String author = io.readLineOrDefault("Autor", book.author());
        int year = io.readIntOrDefault("Rok wydania", Book.MIN_PUBLICATION_YEAR, currentYear(),
                book.publicationYear());
        Genre genre = pickGenre(book.genre());

        int updated = bookService.updateBookDetails(book.isbn(), title, author, year, genre);
        io.println("Zaktualizowano dane " + updated + " egz.");
    }

    /**
     * Usuwa wybrany egzemplarz po potwierdzeniu. Egzemplarz wypożyczony nie może zostać usunięty.
     */
    private void deleteCopy() {
        Optional<BookSummary> selected = selectTitle("Numer książki");
        if (selected.isEmpty()) {
            return;
        }
        BookSummary book = selected.get();
        List<Book> copies = bookService.getCopies(book.isbn());
        printCopies(copies);
        int number = io.readInt("Numer egzemplarza do usunięcia (0 = anuluj): ", 0, copies.size());
        if (number == 0) {
            return;
        }
        Book copy = copies.get(number - 1);
        if (!io.confirm("Usunąć egzemplarz [" + copy.getShortId() + "] książki \"" + book.title() + "\"")) {
            io.println("Anulowano.");
            return;
        }
        bookService.deleteCopy(copy.getId());
        io.println("Usunięto egzemplarz.");
    }

    /**
     * Pokazuje tabelę tytułów i pozwala podejrzeć egzemplarze wybranej pozycji, aż do wybrania powrotu.
     *
     * @param summaries    tytuły do pokazania
     * @param emptyMessage komunikat wypisywany, gdy lista jest pusta
     */
    private void browse(List<BookSummary> summaries, String emptyMessage) {
        if (summaries.isEmpty()) {
            io.println(emptyMessage);
            return;
        }
        while (true) {
            printTable(summaries);
            int number = io.readInt("Numer pozycji, aby zobaczyć egzemplarze (0 = powrót): ", 0, summaries.size());
            if (number == 0) {
                return;
            }
            showCopies(summaries.get(number - 1));
        }
    }

    /**
     * Pokazuje dane książki i listę jej egzemplarzy.
     *
     * @param book podsumowanie wybranej książki
     */
    private void showCopies(BookSummary book) {
        io.println();
        io.println("Książka: " + book.title());
        io.println("Autor: " + book.author() + ", rok: " + book.publicationYear()
                + ", ISBN: " + book.isbn() + ", gatunek: " + book.genre().getDisplayName());
        printCopies(bookService.getCopies(book.isbn()));
        io.println();
    }

    /**
     * Wypisuje numerowaną listę egzemplarzy z krótkim identyfikatorem i stanem.
     *
     * @param copies egzemplarze jednej książki
     */
    private void printCopies(List<Book> copies) {
        io.println("Egzemplarze:");
        for (int i = 0; i < copies.size(); i++) {
            Book copy = copies.get(i);
            String state = bookService.isCopyAvailable(copy.getId()) ? "wolny" : "wypożyczony";
            io.printf("  %d. [%s] %s%n", i + 1, copy.getShortId(), state);
        }
    }

    /**
     * Pokazuje katalog i prosi o wybór jednej pozycji.
     *
     * @param prompt początek zachęty, np. "Numer książki"
     * @return wybrana pozycja albo pusty wynik, gdy katalog jest pusty lub użytkownik anulował
     */
    private Optional<BookSummary> selectTitle(String prompt) {
        List<BookSummary> catalog = bookService.getCatalog();
        if (catalog.isEmpty()) {
            io.println("Katalog jest pusty.");
            return Optional.empty();
        }
        printTable(catalog);
        int number = io.readInt(prompt + " (0 = anuluj): ", 0, catalog.size());
        return number == 0 ? Optional.empty() : Optional.of(catalog.get(number - 1));
    }

    /**
     * Wypisuje tabelę z numerowanymi tytułami.
     *
     * @param summaries tytuły do wypisania
     */
    private void printTable(List<BookSummary> summaries) {
        io.println();
        io.printf("%-4s %-30s %-22s %-5s %-14s %s%n", "Lp.", "Tytuł", "Autor", "Rok", "ISBN", "Wolne/razem");
        io.println("-".repeat(88));
        for (int i = 0; i < summaries.size(); i++) {
            BookSummary book = summaries.get(i);
            io.printf("%-4s %-30s %-22s %-5d %-14s %d/%d%n",
                    (i + 1) + ".",
                    TextFormat.shorten(book.title(), 30),
                    TextFormat.shorten(book.author(), 22),
                    book.publicationYear(),
                    book.isbn(),
                    book.availableCopies(),
                    book.totalCopies());
        }
    }

    /**
     * Wypisuje listę gatunków i prosi o wybór numeru. Przy edycji Enter zostawia dotychczasowy gatunek.
     *
     * @param current dotychczasowy gatunek lub {@code null} przy dodawaniu nowej książki
     * @return wybrany gatunek
     */
    private Genre pickGenre(Genre current) {
        Genre[] genres = Genre.values();
        for (int i = 0; i < genres.length; i++) {
            io.printf("  %d. %s%n", i + 1, genres[i].getDisplayName());
        }
        int number = current == null
                ? io.readInt("Gatunek (numer): ", 1, genres.length)
                : io.readIntOrDefault("Gatunek (numer)", 1, genres.length, current.ordinal() + 1);
        return genres[number - 1];
    }

    /**
     * Zwraca bieżący rok, używany jako górna granica roku wydania.
     *
     * @return bieżący rok kalendarzowy
     */
    private static int currentYear() {
        return Year.now().getValue();
    }
}
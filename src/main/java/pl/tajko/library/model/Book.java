package pl.tajko.library.model;

import pl.tajko.library.validation.IsbnValidator;

import java.time.Year;
import java.util.UUID;

/**
 * Pojedynczy egzemplarz książki w bibliotece.
 * <p>
 * Każdy fizyczny egzemplarz ma własny, unikalny identyfikator {@link UUID}.
 * Dwa egzemplarze tego samego tytułu mają więc różne {@code id}, ale ten sam numer ISBN.
 * Informacja o tym, czy egzemplarz jest wypożyczony, nie jest przechowywana w tej klasie:
 * wynika z istnienia aktywnego wypożyczenia ({@code Loan}).
 * <p>
 * Dwie książki są równe, gdy mają ten sam identyfikator.
 */
public class Book {

    /** Najwcześniejszy dopuszczalny rok wydania (początek druku w Europie). */
    public static final int MIN_PUBLICATION_YEAR = 1450;

    /** Długość skróconego identyfikatora wyświetlanego użytkownikowi. */
    private static final int SHORT_ID_LENGTH = 8;

    /** Unikalny identyfikator egzemplarza, niezmienny po utworzeniu. */
    private final UUID id;

    /** Tytuł książki. */
    private String title;

    /** Autor książki. */
    private String author;

    /** Rok wydania. */
    private int publicationYear;

    /** Znormalizowany numer ISBN (bez myślników i spacji). */
    private String isbn;

    /** Gatunek książki. */
    private Genre genre;

    /**
     * Tworzy nowy egzemplarz książki z automatycznie wygenerowanym identyfikatorem.
     *
     * @param title           tytuł, nie może być pusty
     * @param author          autor, nie może być pusty
     * @param publicationYear rok wydania, od {@link #MIN_PUBLICATION_YEAR} do bieżącego roku
     * @param isbn            poprawny numer ISBN-10 lub ISBN-13 (myślniki są dozwolone)
     * @param genre           gatunek, nie może być {@code null}
     * @throws IllegalArgumentException jeśli którykolwiek argument jest niepoprawny
     */
    public Book(String title, String author, int publicationYear, String isbn, Genre genre) {
        this(UUID.randomUUID(), title, author, publicationYear, isbn, genre);
    }

    /**
     * Tworzy egzemplarz książki z podanym identyfikatorem.
     * Używany przy odczycie zapisanych danych, gdy identyfikator już istnieje.
     *
     * @param id              istniejący identyfikator, nie może być {@code null}
     * @param title           tytuł, nie może być pusty
     * @param author          autor, nie może być pusty
     * @param publicationYear rok wydania, od {@link #MIN_PUBLICATION_YEAR} do bieżącego roku
     * @param isbn            poprawny numer ISBN-10 lub ISBN-13
     * @param genre           gatunek, nie może być {@code null}
     * @throws IllegalArgumentException jeśli którykolwiek argument jest niepoprawny
     */
    public Book(UUID id, String title, String author, int publicationYear, String isbn, Genre genre) {
        if (id == null) {
            throw new IllegalArgumentException("Identyfikator nie może być pusty");
        }
        this.id = id;
        this.title = requireText(title, "Tytuł");
        this.author = requireText(author, "Autor");
        this.publicationYear = requireValidYear(publicationYear);
        this.isbn = requireValidIsbn(isbn);
        this.genre = requireGenre(genre);
    }

    /**
     * Zwraca unikalny identyfikator egzemplarza.
     *
     * @return identyfikator UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Zwraca skrócony identyfikator (pierwsze 8 znaków UUID) do wyświetlania w konsoli.
     *
     * @return skrócony identyfikator, np. "3f2a9c1e"
     */
    public String getShortId() {
        return id.toString().substring(0, SHORT_ID_LENGTH);
    }

    /**
     * Zwraca tytuł książki.
     *
     * @return tytuł
     */
    public String getTitle() {
        return title;
    }

    /**
     * Ustawia nowy tytuł. Wartość jest przycinana z białych znaków na początku i końcu.
     *
     * @param title nowy tytuł, nie może być pusty
     * @throws IllegalArgumentException jeśli tytuł jest pusty
     */
    public void setTitle(String title) {
        this.title = requireText(title, "Tytuł");
    }

    /**
     * Zwraca autora książki.
     *
     * @return autor
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Ustawia nowego autora. Wartość jest przycinana z białych znaków na początku i końcu.
     *
     * @param author nowy autor, nie może być pusty
     * @throws IllegalArgumentException jeśli autor jest pusty
     */
    public void setAuthor(String author) {
        this.author = requireText(author, "Autor");
    }

    /**
     * Zwraca rok wydania.
     *
     * @return rok wydania
     */
    public int getPublicationYear() {
        return publicationYear;
    }

    /**
     * Ustawia nowy rok wydania.
     *
     * @param publicationYear rok od {@link #MIN_PUBLICATION_YEAR} do bieżącego roku
     * @throws IllegalArgumentException jeśli rok jest poza dopuszczalnym zakresem
     */
    public void setPublicationYear(int publicationYear) {
        this.publicationYear = requireValidYear(publicationYear);
    }

    /**
     * Zwraca znormalizowany numer ISBN (same cyfry, ewentualnie X na końcu).
     *
     * @return numer ISBN bez myślników
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Ustawia nowy numer ISBN. Numer jest sprawdzany i normalizowany.
     *
     * @param isbn poprawny numer ISBN-10 lub ISBN-13
     * @throws IllegalArgumentException jeśli numer jest niepoprawny
     */
    public void setIsbn(String isbn) {
        this.isbn = requireValidIsbn(isbn);
    }

    /**
     * Zwraca gatunek książki.
     *
     * @return gatunek
     */
    public Genre getGenre() {
        return genre;
    }

    /**
     * Ustawia nowy gatunek.
     *
     * @param genre nowy gatunek, nie może być {@code null}
     * @throws IllegalArgumentException jeśli gatunek jest {@code null}
     */
    public void setGenre(Genre genre) {
        this.genre = requireGenre(genre);
    }

    /**
     * Porównuje książki na podstawie identyfikatora.
     *
     * @param o porównywany obiekt
     * @return {@code true}, jeśli obiekt jest książką o tym samym identyfikatorze
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book other)) {
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
     * Zwraca czytelny opis książki, przydatny w logach i podczas debugowania.
     *
     * @return tekst z wszystkimi polami książki
     */
    @Override
    public String toString() {
        return "Book{id=" + id
                + ", title='" + title + '\''
                + ", author='" + author + '\''
                + ", publicationYear=" + publicationYear
                + ", isbn='" + isbn + '\''
                + ", genre=" + genre
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
     * Sprawdza, że rok mieści się w dopuszczalnym zakresie.
     *
     * @param year sprawdzany rok
     * @return ten sam rok, jeśli jest poprawny
     * @throws IllegalArgumentException jeśli rok jest poza zakresem
     */
    private static int requireValidYear(int year) {
        int maxYear = Year.now().getValue();
        if (year < MIN_PUBLICATION_YEAR || year > maxYear) {
            throw new IllegalArgumentException(
                    "Rok wydania musi mieścić się w zakresie " + MIN_PUBLICATION_YEAR + "-" + maxYear);
        }
        return year;
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
     * Sprawdza, że gatunek został podany.
     *
     * @param genre sprawdzany gatunek
     * @return ten sam gatunek
     * @throws IllegalArgumentException jeśli gatunek jest {@code null}
     */
    private static Genre requireGenre(Genre genre) {
        if (genre == null) {
            throw new IllegalArgumentException("Gatunek nie może być pusty");
        }
        return genre;
    }
}
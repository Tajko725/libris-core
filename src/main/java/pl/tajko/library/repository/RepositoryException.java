package pl.tajko.library.repository;

/**
 * Wyjątek zgłaszany, gdy odczyt lub zapis danych w repozytorium się nie powiedzie,
 * na przykład przy braku dostępu do pliku albo uszkodzonej zawartości.
 * <p>
 * Jest wyjątkiem niekontrolowanym ({@link RuntimeException}), więc nie trzeba go
 * deklarować w sygnaturach metod.
 */
public class RepositoryException extends RuntimeException {

    /**
     * Tworzy wyjątek z opisem błędu i jego przyczyną.
     *
     * @param message opis błędu dla użytkownika lub logów
     * @param cause   pierwotna przyczyna błędu
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Tworzy wyjątek z samym opisem błędu.
     *
     * @param message opis błędu dla użytkownika lub logów
     */
    public RepositoryException(String message) {
        super(message);
    }
}
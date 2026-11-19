package pl.tajko.library.exception;

/**
 * Wyjątek zgłaszany, gdy szukany obiekt (książka, czytelnik, wypożyczenie) nie istnieje.
 */
public class EntityNotFoundException extends LibraryException {

    /**
     * Tworzy wyjątek z opisem tego, czego nie znaleziono.
     *
     * @param message opis błędu czytelny dla użytkownika
     */
    public EntityNotFoundException(String message) {
        super(message);
    }
}
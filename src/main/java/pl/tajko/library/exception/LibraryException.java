package pl.tajko.library.exception;

/**
 * Bazowy wyjątek błędów logiki biblioteki.
 * <p>
 * Jest wyjątkiem niekontrolowanym ({@link RuntimeException}). Podklasy opisują konkretny rodzaj problemu,
 * dzięki czemu warstwa interfejsu może je łapać osobno i pokazywać użytkownikowi czytelny komunikat.
 */
public class LibraryException extends RuntimeException {

    /**
     * Tworzy wyjątek z opisem błędu.
     *
     * @param message opis błędu czytelny dla użytkownika
     */
    public LibraryException(String message) {
        super(message);
    }

    /**
     * Tworzy wyjątek z opisem błędu i jego przyczyną.
     *
     * @param message opis błędu czytelny dla użytkownika
     * @param cause   pierwotna przyczyna błędu
     */
    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
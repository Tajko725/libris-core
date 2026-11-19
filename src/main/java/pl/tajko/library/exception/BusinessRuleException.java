package pl.tajko.library.exception;

/**
 * Wyjątek zgłaszany, gdy operacja łamie regułę biznesową biblioteki,
 * na przykład próba usunięcia wypożyczonej książki.
 */
public class BusinessRuleException extends LibraryException {

    /**
     * Tworzy wyjątek z opisem naruszonej reguły.
     *
     * @param message opis błędu czytelny dla użytkownika
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
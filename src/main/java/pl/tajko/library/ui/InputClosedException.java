package pl.tajko.library.ui;

/**
 * Zgłaszany, gdy strumień wejściowy się skończył (np. użytkownik wcisnął Ctrl+Z / Ctrl+D
 * albo skończyły się dane w teście), więc nie da się już wczytać kolejnej linii.
 * <p>
 * Menu nie łapią tego wyjątku, dzięki czemu program kończy działanie zamiast kręcić się w pętli.
 */
public class InputClosedException extends RuntimeException {

    /**
     * Tworzy wyjątek ze standardowym komunikatem.
     */
    public InputClosedException() {
        super("Koniec danych wejściowych");
    }
}
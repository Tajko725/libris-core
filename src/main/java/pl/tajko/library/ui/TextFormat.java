package pl.tajko.library.ui;

/**
 * Pomocnicze funkcje do ładnego wypisywania tekstu w tabelach.
 */
public final class TextFormat {

    /** Znaki doklejane na końcu skróconego tekstu. */
    private static final String ELLIPSIS = "...";

    /**
     * Klasa narzędziowa, nie tworzy się jej obiektów.
     */
    private TextFormat() {
    }

    /**
     * Skraca tekst do podanej długości, doklejając "..." gdy trzeba było coś uciąć.
     *
     * @param text      tekst do skrócenia, {@code null} traktowany jest jak pusty tekst
     * @param maxLength największa dozwolona długość wyniku, co najmniej 4
     * @return tekst nie dłuższy niż {@code maxLength}
     * @throws IllegalArgumentException jeśli {@code maxLength} jest mniejsze od 4
     */
    public static String shorten(String text, int maxLength) {
        if (maxLength < ELLIPSIS.length() + 1) {
            throw new IllegalArgumentException("Maksymalna długość musi wynosić co najmniej 4");
        }
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
    }
}
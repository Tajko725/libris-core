package pl.tajko.library.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizacja tekstu na potrzeby wyszukiwania: małe litery i brak polskich znaków diakrytycznych.
 */
public final class TextNormalizer {

    /** Wzorzec znaków łączących (akcenty), które zostają po rozkładzie Unicode. */
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    /** Prywatny konstruktor: klasa zawiera tylko metody statyczne i nie powinna być tworzona. */
    private TextNormalizer() {
    }

    /**
     * Zamienia tekst na małe litery, usuwa znaki diakrytyczne (także ł → l) i obcina białe znaki na brzegach.
     * Przykład: "  Wiedźmin ŁÓDŹ " daje "wiedzmin lodz".
     *
     * @param text tekst do znormalizowania, może być {@code null}
     * @return znormalizowany tekst lub pusty tekst, jeśli przekazano {@code null}
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.trim().toLowerCase(Locale.ROOT).replace('ł', 'l');
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("");
    }
}
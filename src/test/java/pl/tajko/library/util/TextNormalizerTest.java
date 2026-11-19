package pl.tajko.library.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testy jednostkowe klasy {@link TextNormalizer}.
 */
@DisplayName("TextNormalizer")
class TextNormalizerTest {

    /**
     * Polskie litery i wielkość liter powinny zostać ujednolicone.
     *
     * @param input    tekst wejściowy
     * @param expected oczekiwany wynik
     */
    @ParameterizedTest(name = "\"{0}\" daje \"{1}\"")
    @CsvSource({
            "Wiedźmin, wiedzmin",
            "ŁÓDŹ, lodz",
            "Zażółć gęślą jaźń, zazolc gesla jazn",
            "Lalka, lalka",
            "'  spacje na brzegach  ', spacje na brzegach"
    })
    void shouldNormalizeText(String input, String expected) {
        assertEquals(expected, TextNormalizer.normalize(input));
    }

    /**
     * Wartość {@code null} powinna dać pusty tekst.
     */
    @Test
    @DisplayName("null daje pusty tekst")
    void shouldReturnEmptyStringForNull() {
        assertEquals("", TextNormalizer.normalize(null));
    }
}
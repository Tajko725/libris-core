package pl.tajko.library.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testy jednostkowe klasy {@link TextFormat}.
 */
@DisplayName("TextFormat")
class TextFormatTest {

    /**
     * Krótki tekst powinien pozostać bez zmian.
     */
    @Test
    @DisplayName("nie zmienia tekstu mieszczącego się w limicie")
    void shouldKeepShortText() {
        assertAll(
                () -> assertEquals("Czysty kod", TextFormat.shorten("Czysty kod", 10)),
                () -> assertEquals("", TextFormat.shorten("", 10))
        );
    }

    /**
     * Za długi tekst powinien zostać ucięty z wielokropkiem i mieścić się w limicie.
     */
    @Test
    @DisplayName("skraca zbyt długi tekst do limitu z wielokropkiem")
    void shouldShortenLongText() {
        String result = TextFormat.shorten("Pan Tadeusz, czyli ostatni zajazd na Litwie", 15);

        assertAll(
                () -> assertEquals("Pan Tadeusz,...", result),
                () -> assertEquals(15, result.length())
        );
    }

    /**
     * Brak tekstu powinien dać pusty tekst.
     */
    @Test
    @DisplayName("traktuje null jak pusty tekst")
    void shouldTreatNullAsEmpty() {
        assertEquals("", TextFormat.shorten(null, 10));
    }

    /**
     * Zbyt mały limit nie pozwala zmieścić wielokropka.
     *
     * @param maxLength zbyt mały limit
     */
    @ParameterizedTest(name = "odrzuca limit {0}")
    @ValueSource(ints = {-1, 0, 3})
    void shouldRejectTooSmallLimit(int maxLength) {
        assertThrows(IllegalArgumentException.class, () -> TextFormat.shorten("tekst", maxLength));
    }
}
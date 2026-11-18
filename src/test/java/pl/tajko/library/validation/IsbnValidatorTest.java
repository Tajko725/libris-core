package pl.tajko.library.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy jednostkowe walidatora numerów ISBN.
 */
@DisplayName("IsbnValidator")
class IsbnValidatorTest {

    /**
     * Poprawne numery w różnych zapisach powinny zostać zaakceptowane.
     *
     * @param isbn poprawny numer ISBN
     */
    @ParameterizedTest(name = "akceptuje poprawny numer: {0}")
    @ValueSource(strings = {
            "0306406152",           // ISBN-10
            "0-306-40615-2",        // ISBN-10 z myślnikami
            "080442957X",           // ISBN-10 z cyfrą kontrolną X
            "080442957x",           // ISBN-10 z małą literą x
            "9780306406157",        // ISBN-13
            "978-0-306-40615-7",    // ISBN-13 z myślnikami
            "978 0 13 468599 1"     // ISBN-13 ze spacjami
    })
    void shouldAcceptValidIsbn(String isbn) {
        assertTrue(IsbnValidator.isValid(isbn));
    }

    /**
     * Numery z błędną cyfrą kontrolną, długością lub znakami powinny zostać odrzucone.
     *
     * @param isbn niepoprawny numer ISBN
     */
    @ParameterizedTest(name = "odrzuca niepoprawny numer: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "12345",                // za krótki
            "97803064061570",       // za długi
            "0306406153",           // zła cyfra kontrolna ISBN-10
            "9780306406158",        // zła cyfra kontrolna ISBN-13
            "0306X06152",           // litera X nie na końcu
            "abcdefghij",           // litery zamiast cyfr
            "97803064061X7"         // litera w ISBN-13
    })
    void shouldRejectInvalidIsbn(String isbn) {
        assertFalse(IsbnValidator.isValid(isbn));
    }

    /**
     * Normalizacja powinna usunąć myślniki i spacje oraz zamienić litery na wielkie.
     */
    @Test
    @DisplayName("normalizuje numer: usuwa myślniki i spacje, zamienia x na X")
    void shouldNormalizeIsbn() {
        assertEquals("080442957X", IsbnValidator.normalize(" 0-8044-2957-x "));
    }

    /**
     * Normalizacja wartości {@code null} powinna zwrócić {@code null}.
     */
    @Test
    @DisplayName("normalizacja null zwraca null")
    void shouldReturnNullWhenNormalizingNull() {
        assertNull(IsbnValidator.normalize(null));
    }
}
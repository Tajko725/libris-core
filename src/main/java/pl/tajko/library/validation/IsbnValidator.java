package pl.tajko.library.validation;

import java.util.Locale;

/**
 * Walidator numerów ISBN (International Standard Book Number).
 * <p>
 * Obsługuje oba formaty: ISBN-10 (ostatni znak może być cyfrą lub literą X)
 * oraz ISBN-13. Poprawność sprawdzana jest na podstawie cyfry kontrolnej.
 * Myślniki i spacje w numerze są ignorowane.
 */
public final class IsbnValidator {

    /** Długość numeru ISBN-10 (bez myślników). */
    private static final int ISBN_10_LENGTH = 10;

    /** Długość numeru ISBN-13 (bez myślników). */
    private static final int ISBN_13_LENGTH = 13;

    /** Prywatny konstruktor: klasa zawiera tylko metody statyczne i nie powinna być tworzona. */
    private IsbnValidator() {
    }

    /**
     * Normalizuje numer ISBN: usuwa spacje i myślniki oraz zamienia litery na wielkie.
     *
     * @param isbn numer ISBN w dowolnym zapisie, może być {@code null}
     * @return znormalizowany numer lub {@code null}, jeśli przekazano {@code null}
     */
    public static String normalize(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Sprawdza, czy podany numer jest poprawnym ISBN-10 lub ISBN-13.
     *
     * @param isbn numer ISBN w dowolnym zapisie, może być {@code null}
     * @return {@code true}, jeśli długość i cyfra kontrolna są poprawne
     */
    public static boolean isValid(String isbn) {
        String normalized = normalize(isbn);
        if (normalized == null) {
            return false;
        }
        return switch (normalized.length()) {
            case ISBN_10_LENGTH -> isValidIsbn10(normalized);
            case ISBN_13_LENGTH -> isValidIsbn13(normalized);
            default -> false;
        };
    }

    /**
     * Sprawdza cyfrę kontrolną ISBN-10 (suma ważona modulo 11).
     *
     * @param isbn znormalizowany numer o długości 10 znaków
     * @return {@code true}, jeśli numer jest poprawny
     */
    private static boolean isValidIsbn10(String isbn) {
        int sum = 0;
        for (int i = 0; i < ISBN_10_LENGTH - 1; i++) {
            char c = isbn.charAt(i);
            if (!isAsciiDigit(c)) {
                return false;
            }
            sum += (c - '0') * (ISBN_10_LENGTH - i);
        }
        char last = isbn.charAt(ISBN_10_LENGTH - 1);
        if (last == 'X') {
            sum += 10;
        } else if (isAsciiDigit(last)) {
            sum += last - '0';
        } else {
            return false;
        }
        return sum % 11 == 0;
    }

    /**
     * Sprawdza cyfrę kontrolną ISBN-13 (wagi 1 i 3 na przemian, modulo 10).
     *
     * @param isbn znormalizowany numer o długości 13 znaków
     * @return {@code true}, jeśli numer jest poprawny
     */
    private static boolean isValidIsbn13(String isbn) {
        int sum = 0;
        for (int i = 0; i < ISBN_13_LENGTH; i++) {
            char c = isbn.charAt(i);
            if (!isAsciiDigit(c)) {
                return false;
            }
            sum += (c - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return sum % 10 == 0;
    }

    /**
     * Sprawdza, czy znak jest cyfrą od 0 do 9 (bez cyfr z innych alfabetów).
     *
     * @param c sprawdzany znak
     * @return {@code true}, jeśli znak to cyfra ASCII
     */
    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
package pl.tajko.library.ui;

import pl.tajko.library.exception.LibraryException;
import pl.tajko.library.repository.RepositoryException;

/**
 * Wspólne narzędzia dla menu konsolowych.
 */
public final class MenuSupport {

    /**
     * Klasa narzędziowa, nie tworzy się jej obiektów.
     */
    private MenuSupport() {
    }

    /**
     * Wykonuje akcję menu i zamienia oczekiwane błędy na czytelny komunikat zamiast zatrzymywać program.
     * <p>
     * Obsługiwane są błędy reguł biblioteki ({@link LibraryException}), niepoprawne dane
     * ({@link IllegalArgumentException}) i błędy plików danych ({@link RepositoryException}).
     * Pozostałe wyjątki (np. {@link InputClosedException} czy błędy programisty) lecą dalej.
     *
     * @param io     konsola, na którą trafia komunikat
     * @param action akcja do wykonania
     */
    public static void runSafely(ConsoleIO io, Runnable action) {
        try {
            action.run();
        } catch (RepositoryException e) {
            io.printError("problem z plikiem danych: " + e.getMessage());
        } catch (LibraryException | IllegalArgumentException e) {
            io.printError(e.getMessage());
        }
    }
}
package pl.tajko.library.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Podsumowanie wypożyczenia z tytułem książki i nazwiskiem czytelnika,
 * używane na listach wypożyczeń.
 * <p>
 * To obiekt tylko do odczytu, wyliczany na bieżąco przez serwis. Jeśli książka lub czytelnik
 * zostali usunięci z biblioteki, w ich miejscu pojawia się tekst zastępczy.
 *
 * @param loanId     identyfikator wypożyczenia
 * @param bookId     identyfikator wypożyczonego egzemplarza
 * @param isbn       numer ISBN książki
 * @param bookTitle  tytuł książki
 * @param memberId   identyfikator czytelnika
 * @param memberName imię i nazwisko czytelnika
 * @param loanDate   data wypożyczenia
 * @param dueDate    termin zwrotu
 * @param returnDate data zwrotu lub {@code null}, jeśli książka nie została zwrócona
 * @param overdue    czy wypożyczenie jest po terminie zwrotu (względem dzisiejszej daty)
 */
public record LoanSummary(
        UUID loanId,
        UUID bookId,
        String isbn,
        String bookTitle,
        UUID memberId,
        String memberName,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        boolean overdue) {

    /**
     * Sprawdza, czy książka nie została jeszcze zwrócona.
     *
     * @return {@code true}, jeśli wypożyczenie jest aktywne
     */
    public boolean isActive() {
        return returnDate == null;
    }
}
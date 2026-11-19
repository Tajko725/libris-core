package pl.tajko.library.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Podsumowanie czytelnika wraz z liczbą jego aktualnych wypożyczeń,
 * używane na liście czytelników i w wynikach wyszukiwania.
 * <p>
 * To obiekt tylko do odczytu, wyliczany na bieżąco przez serwis.
 *
 * @param id             identyfikator czytelnika
 * @param firstName      imię
 * @param lastName       nazwisko
 * @param email          adres e-mail
 * @param registeredAt   data rejestracji w bibliotece
 * @param activeLoans    liczba aktywnych (jeszcze niezwróconych) wypożyczeń
 * @param overdueLoans   liczba aktywnych wypożyczeń po terminie zwrotu
 */
public record MemberSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        LocalDate registeredAt,
        int activeLoans,
        int overdueLoans) {

    /**
     * Zwraca imię i nazwisko w jednym tekście, np. do wyświetlania w tabelach.
     *
     * @return tekst w formacie "Imię Nazwisko"
     */
    public String fullName() {
        return firstName + " " + lastName;
    }

    /**
     * Sprawdza, czy czytelnik ma wypożyczenia po terminie zwrotu.
     *
     * @return {@code true}, jeśli liczba przeterminowanych wypożyczeń jest większa od zera
     */
    public boolean hasOverdueLoans() {
        return overdueLoans > 0;
    }
}
package pl.tajko.library.service;

import pl.tajko.library.model.Genre;

/**
 * Podsumowanie wszystkich egzemplarzy tej samej książki (o tym samym numerze ISBN),
 * używane w katalogu i w wynikach wyszukiwania.
 * <p>
 * To obiekt tylko do odczytu, wyliczany na bieżąco przez serwis.
 *
 * @param isbn            znormalizowany numer ISBN wspólny dla wszystkich egzemplarzy
 * @param title           tytuł
 * @param author          autor
 * @param publicationYear rok wydania
 * @param genre           gatunek
 * @param totalCopies     liczba wszystkich egzemplarzy
 * @param availableCopies liczba egzemplarzy dostępnych do wypożyczenia
 */
public record BookSummary(
        String isbn,
        String title,
        String author,
        int publicationYear,
        Genre genre,
        int totalCopies,
        int availableCopies) {

    /**
     * Zwraca liczbę egzemplarzy aktualnie wypożyczonych.
     *
     * @return liczba wszystkich egzemplarzy pomniejszona o dostępne
     */
    public int borrowedCopies() {
        return totalCopies - availableCopies;
    }
}
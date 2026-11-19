package pl.tajko.library.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ogólny interfejs repozytorium: miejsca, w którym przechowywane są obiekty danego typu.
 * <p>
 * Interfejs ukrywa sposób zapisu danych. Serwisy korzystają tylko z tych metod,
 * więc sposób przechowywania (plik JSON, baza danych) można zmienić bez zmian w logice biznesowej.
 *
 * @param <T> typ przechowywanych obiektów
 */
public interface Repository<T> {

    /**
     * Zapisuje obiekt. Jeśli obiekt o tym samym identyfikatorze już istnieje, zastępuje go.
     *
     * @param item zapisywany obiekt, nie może być {@code null}
     * @throws IllegalArgumentException jeśli obiekt jest {@code null}
     * @throws RepositoryException      jeśli zapis się nie powiódł
     */
    void save(T item);

    /**
     * Wyszukuje obiekt po identyfikatorze.
     *
     * @param id identyfikator obiektu
     * @return obiekt lub {@link Optional#empty()}, jeśli nie istnieje
     */
    Optional<T> findById(UUID id);

    /**
     * Zwraca wszystkie obiekty w kolejności ich dodania.
     *
     * @return niemodyfikowalna lista, pusta jeśli nie ma żadnych obiektów
     */
    List<T> findAll();

    /**
     * Usuwa obiekt o podanym identyfikatorze.
     *
     * @param id identyfikator usuwanego obiektu
     * @return {@code true}, jeśli obiekt istniał i został usunięty; {@code false}, jeśli go nie było
     * @throws RepositoryException jeśli zapis zmiany się nie powiódł
     */
    boolean deleteById(UUID id);
}
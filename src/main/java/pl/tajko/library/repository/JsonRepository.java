package pl.tajko.library.repository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Repozytorium przechowujące obiekty w pliku JSON.
 * <p>
 * Przy tworzeniu wczytuje cały plik do pamięci, a po każdej zmianie ({@link #save}, {@link #deleteById})
 * zapisuje całą zawartość z powrotem. Zapis jest bezpieczny: dane trafiają najpierw do pliku
 * tymczasowego, który następnie zastępuje właściwy plik. Przerwanie zapisu nie uszkadza więc
 * istniejącego pliku z danymi.
 * <p>
 * Jeśli plik nie istnieje, repozytorium startuje puste, a plik (razem z katalogiem) powstaje przy pierwszym zapisie.
 * Jeśli plik jest uszkodzony albo zawiera niepoprawne dane, konstruktor zgłasza {@link RepositoryException},
 * zamiast po cichu zaczynać od pustych danych, ponieważ następny zapis skasowałby wtedy stare dane.
 * <p>
 * Obiekty zwracane z repozytorium są tymi samymi instancjami, które są w pamięci.
 * Po zmianie obiektu (np. przez setter) trzeba wywołać {@link #save}, aby zmiana trafiła do pliku.
 * Klasa nie jest bezpieczna wątkowo.
 *
 * @param <T> typ przechowywanych obiektów
 */
public class JsonRepository<T> implements Repository<T> {

    /** Rozszerzenie pliku tymczasowego używanego przy zapisie. */
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    /** Ścieżka do pliku z danymi. */
    private final Path file;

    /** Klasa przechowywanych obiektów, potrzebna Jacksonowi do odczytu. */
    private final Class<T> type;

    /** Funkcja zwracająca identyfikator obiektu. */
    private final Function<T, UUID> idExtractor;

    /** Mapper Jacksona zapisujący czytelny, wcięty JSON. */
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /** Obiekty w pamięci, w kolejności dodania. */
    private final Map<UUID, T> items = new LinkedHashMap<>();

    /**
     * Tworzy repozytorium i wczytuje dane z pliku, jeśli plik istnieje.
     *
     * @param file        ścieżka do pliku JSON, nie może być {@code null}
     * @param type        klasa przechowywanych obiektów, nie może być {@code null}
     * @param idExtractor funkcja zwracająca identyfikator obiektu, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś argument jest {@code null}
     * @throws RepositoryException      jeśli plik istnieje, ale nie da się go odczytać lub jest niepoprawny
     */
    public JsonRepository(Path file, Class<T> type, Function<T, UUID> idExtractor) {
        if (file == null || type == null || idExtractor == null) {
            throw new IllegalArgumentException("Ścieżka, typ i funkcja identyfikatora nie mogą być puste");
        }
        this.file = file;
        this.type = type;
        this.idExtractor = idExtractor;
        load();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Jeśli zapis do pliku się nie powiedzie, stan w pamięci wraca do poprzedniego.
     */
    @Override
    public void save(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Zapisywany obiekt nie może być pusty");
        }
        UUID id = idExtractor.apply(item);
        T previous = items.put(id, item);
        try {
            persist();
        } catch (RepositoryException e) {
            if (previous == null) {
                items.remove(id);
            } else {
                items.put(id, previous);
            }
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<T> findById(UUID id) {
        return Optional.ofNullable(items.get(id));
    }

    /** {@inheritDoc} */
    @Override
    public List<T> findAll() {
        return List.copyOf(items.values());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Jeśli zapis do pliku się nie powiedzie, obiekt wraca do pamięci.
     */
    @Override
    public boolean deleteById(UUID id) {
        T removed = items.remove(id);
        if (removed == null) {
            return false;
        }
        try {
            persist();
        } catch (RepositoryException e) {
            items.put(id, removed);
            throw e;
        }
        return true;
    }

    /**
     * Wczytuje obiekty z pliku do pamięci. Brak pliku oznacza puste repozytorium.
     *
     * @throws RepositoryException jeśli plik nie da się odczytać, ma niepoprawny format,
     *                             zawiera niepoprawne dane lub powtórzone identyfikatory
     */
    private void load() {
        if (Files.notExists(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
            List<T> loaded = mapper.readValue(in, listType);
            for (T item : loaded) {
                if (item == null) {
                    throw new RepositoryException("Plik " + file + " zawiera pusty element");
                }
                UUID id = idExtractor.apply(item);
                if (items.putIfAbsent(id, item) != null) {
                    throw new RepositoryException("Plik " + file + " zawiera powtórzony identyfikator " + id);
                }
            }
        } catch (IOException | JacksonException e) {
            throw new RepositoryException(
                    "Nie można odczytać pliku " + file + ". Plik jest uszkodzony lub zawiera niepoprawne dane.", e);
        }
    }

    /**
     * Zapisuje całą zawartość pamięci do pliku w sposób bezpieczny:
     * najpierw do pliku tymczasowego, potem podmiana właściwego pliku.
     *
     * @throws RepositoryException jeśli zapis się nie powiódł
     */
    private void persist() {
        Path tempFile = file.resolveSibling(file.getFileName() + TEMP_FILE_SUFFIX);
        try {
            Path directory = file.toAbsolutePath().getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                mapper.writeValue(out, new ArrayList<>(items.values()));
            }
            replaceFile(tempFile);
        } catch (IOException | JacksonException e) {
            throw new RepositoryException("Nie można zapisać pliku " + file, e);
        }
    }

    /**
     * Podmienia plik z danymi na plik tymczasowy, atomowo jeśli system plików to obsługuje.
     *
     * @param tempFile plik tymczasowy z nową zawartością
     * @throws IOException jeśli podmiana się nie powiodła
     */
    private void replaceFile(Path tempFile) throws IOException {
        try {
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
package pl.tajko.library;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.tajko.library.repository.RepositoryException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy jednostkowe klasy {@link LibraryContext}, na prawdziwych plikach w folderze tymczasowym.
 */
@DisplayName("LibraryContext")
class LibraryContextTest {

    /** Stały zegar używany w testach. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Folder tymczasowy tworzony przez JUnit i sprzątany po teście. */
    @TempDir
    Path tempDir;

    /**
     * Konstruktor powinien odrzucać puste argumenty.
     */
    @Test
    @DisplayName("odrzuca puste argumenty")
    void shouldRejectNullArguments() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new LibraryContext(null, CLOCK)),
                () -> assertThrows(IllegalArgumentException.class, () -> new LibraryContext(tempDir, null))
        );
    }

    /**
     * Brakujący folder (także zagnieżdżony) powinien zostać utworzony.
     */
    @Test
    @DisplayName("tworzy brakujący folder z danymi")
    void shouldCreateDataDirectory() {
        Path nested = tempDir.resolve("a").resolve("b").resolve("data");

        LibraryContext context = new LibraryContext(nested, CLOCK);

        assertAll(
                () -> assertTrue(Files.isDirectory(nested)),
                () -> assertEquals(nested, context.dataDir())
        );
    }

    /**
     * Dane zapisane przez jeden kontekst powinny być widoczne po "ponownym uruchomieniu".
     */
    @Test
    @DisplayName("zapisuje dane do plików i odczytuje je w nowym kontekście")
    void shouldPersistDataBetweenContexts() {
        LibraryContext first = new LibraryContext(tempDir, CLOCK);
        first.memberService().addMember("Jan", "Kowalski", "jan@example.com");

        LibraryContext second = new LibraryContext(tempDir, CLOCK);

        assertAll(
                () -> assertTrue(Files.exists(tempDir.resolve(LibraryContext.MEMBERS_FILE))),
                () -> assertEquals(1, second.memberService().getMembers().size()),
                () -> assertTrue(second.loanService().getActiveLoans().isEmpty())
        );
    }

    /**
     * Uszkodzony plik danych powinien dać czytelny błąd repozytorium.
     *
     * @throws IOException jeśli nie uda się zapisać pliku testowego
     */
    @Test
    @DisplayName("zgłasza RepositoryException dla uszkodzonego pliku")
    void shouldFailForCorruptedFile() throws IOException {
        Files.writeString(tempDir.resolve(LibraryContext.BOOKS_FILE), "to nie jest JSON");

        assertThrows(RepositoryException.class,
                () -> new LibraryContext(tempDir, CLOCK).bookService().getCatalog());
    }
}
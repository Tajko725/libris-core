package pl.tajko.library.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.exception.EntityNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy jednostkowe klasy {@link MenuSupport}.
 */
@DisplayName("MenuSupport")
class MenuSupportTest {

    /** Bufor na wyjście testowanej konsoli. */
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    /** Konsola bez danych wejściowych, potrzebna tylko do wypisywania błędów. */
    private final ConsoleIO io =
            new ConsoleIO(new ByteArrayInputStream(new byte[0]), new PrintStream(output, true, UTF_8));

    /**
     * Dostarcza wyjątki, które menu ma zamieniać na komunikat.
     *
     * @return strumień wyjątków z komunikatem "powód"
     */
    static Stream<RuntimeException> handledExceptions() {
        return Stream.of(
                new EntityNotFoundException("powód"),
                new BusinessRuleException("powód"),
                new IllegalArgumentException("powód"));
    }

    /**
     * Poprawna akcja powinna się wykonać bez żadnego komunikatu.
     */
    @Test
    @DisplayName("wykonuje akcję i nic nie wypisuje, gdy nie ma błędu")
    void shouldRunActionWithoutOutput() {
        AtomicBoolean executed = new AtomicBoolean(false);

        MenuSupport.runSafely(io, () -> executed.set(true));

        assertAll(
                () -> assertTrue(executed.get()),
                () -> assertEquals("", output.toString(UTF_8))
        );
    }

    /**
     * Oczekiwane błędy powinny zamienić się w komunikat zamiast przerywać program.
     *
     * @param exception wyjątek zgłoszony przez akcję
     */
    @ParameterizedTest(name = "łapie {0}")
    @MethodSource("handledExceptions")
    void shouldPrintMessageForExpectedErrors(RuntimeException exception) {
        MenuSupport.runSafely(io, () -> {
            throw exception;
        });

        assertEquals("Błąd: powód" + System.lineSeparator(), output.toString(UTF_8));
    }

    /**
     * Błędy programisty nie powinny być ukrywane.
     */
    @Test
    @DisplayName("nie łapie nieoczekiwanych wyjątków")
    void shouldNotCatchUnexpectedExceptions() {
        assertThrows(NullPointerException.class, () -> MenuSupport.runSafely(io, () -> {
            throw new NullPointerException("błąd programisty");
        }));
    }

    /**
     * Koniec wejścia musi przerwać program, a nie zostać zamieniony na komunikat.
     */
    @Test
    @DisplayName("przepuszcza InputClosedException")
    void shouldNotCatchInputClosed() {
        assertThrows(InputClosedException.class, () -> MenuSupport.runSafely(io, () -> {
            throw new InputClosedException();
        }));
    }
}
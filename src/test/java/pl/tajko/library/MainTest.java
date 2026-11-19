package pl.tajko.library;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testy jednostkowe wyboru folderu danych w klasie {@link Main}.
 */
@DisplayName("Main")
class MainTest {

    /**
     * Bez argumentów powinien być użyty folder domyślny.
     */
    @Test
    @DisplayName("bez argumentów używa folderu domyślnego")
    void shouldUseDefaultDirectoryWithoutArguments() {
        assertEquals(Path.of("data"), Main.resolveDataDir(new String[0]));
    }

    /**
     * Pierwszy argument powinien wskazywać folder danych.
     */
    @Test
    @DisplayName("używa folderu podanego jako pierwszy argument")
    void shouldUseFirstArgument() {
        assertEquals(Path.of("moje-dane"), Main.resolveDataDir(new String[]{" moje-dane ", "ignorowany"}));
    }

    /**
     * Pusty argument powinien być potraktowany jak brak argumentu.
     *
     * @param argument pusty lub złożony ze spacji argument
     */
    @ParameterizedTest(name = "pusty argument [{0}] oznacza folder domyślny")
    @ValueSource(strings = {"", " ", "\t"})
    void shouldFallBackForBlankArgument(String argument) {
        assertEquals(Path.of("data"), Main.resolveDataDir(new String[]{argument}));
    }
}
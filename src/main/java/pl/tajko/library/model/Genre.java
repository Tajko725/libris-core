package pl.tajko.library.model;

/**
 * Gatunek literacki książki.
 */
public enum Genre {

    /** Powieść. */
    NOVEL("Powieść"),

    /** Fantastyka. */
    FANTASY("Fantastyka"),

    /** Science fiction. */
    SCIENCE_FICTION("Science fiction"),

    /** Kryminał i sensacja. */
    CRIME("Kryminał"),

    /** Historia. */
    HISTORY("Historia"),

    /** Biografia i autobiografia. */
    BIOGRAPHY("Biografia"),

    /** Literatura popularnonaukowa i naukowa. */
    SCIENCE("Nauka"),

    /** Literatura dziecięca i młodzieżowa. */
    CHILDREN("Dla dzieci"),

    /** Pozostałe gatunki. */
    OTHER("Inne");

    /** Nazwa gatunku wyświetlana użytkownikowi w konsoli. */
    private final String displayName;

    /**
     * Tworzy gatunek z polską nazwą do wyświetlania.
     *
     * @param displayName nazwa gatunku widoczna dla użytkownika
     */
    Genre(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Zwraca polską nazwę gatunku do wyświetlania w interfejsie.
     *
     * @return nazwa gatunku, np. "Fantastyka"
     */
    public String getDisplayName() {
        return displayName;
    }
}
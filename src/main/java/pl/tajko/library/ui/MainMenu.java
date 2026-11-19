package pl.tajko.library.ui;

/**
 * Menu główne aplikacji: wybór działu (katalog, czytelnicy) albo wyjście.
 */
public class MainMenu {

    /** Konsola, z której menu czyta i na którą pisze. */
    private final ConsoleIO io;

    /** Menu katalogu książek. */
    private final BookMenu bookMenu;

    /** Menu czytelników. */
    private final MemberMenu memberMenu;

    /**
     * Tworzy menu główne z podanymi podmenu.
     *
     * @param io         konsola, nie może być {@code null}
     * @param bookMenu   menu katalogu książek, nie może być {@code null}
     * @param memberMenu menu czytelników, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś argument jest {@code null}
     */
    public MainMenu(ConsoleIO io, BookMenu bookMenu, MemberMenu memberMenu) {
        if (io == null || bookMenu == null || memberMenu == null) {
            throw new IllegalArgumentException("Konsola i podmenu nie mogą być puste");
        }
        this.io = io;
        this.bookMenu = bookMenu;
        this.memberMenu = memberMenu;
    }

    /**
     * Wyświetla menu główne i obsługuje wybory użytkownika, aż wybierze wyjście.
     */
    public void run() {
        while (true) {
            io.println();
            io.println("=== Libris: menu główne ===");
            io.println("1. Katalog książek");
            io.println("2. Czytelnicy");
            io.println("0. Wyjście");
            int choice = io.readInt("Wybór: ", 0, 2);
            switch (choice) {
                case 1 -> bookMenu.run();
                case 2 -> memberMenu.run();
                case 0 -> {
                    io.println("Do zobaczenia!");
                    return;
                }
            }
        }
    }
}
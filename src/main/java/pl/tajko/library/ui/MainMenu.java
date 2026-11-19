package pl.tajko.library.ui;

import pl.tajko.library.demo.DemoDataLoader;

/**
 * Menu główne aplikacji: wybór działu (katalog, czytelnicy, wypożyczenia), dane przykładowe albo wyjście.
 */
public class MainMenu {

    /** Konsola, z której menu czyta i na którą pisze. */
    private final ConsoleIO io;

    /** Menu katalogu książek. */
    private final BookMenu bookMenu;

    /** Menu czytelników. */
    private final MemberMenu memberMenu;

    /** Menu wypożyczeń. */
    private final LoanMenu loanMenu;

    /** Loader danych przykładowych. */
    private final DemoDataLoader demoDataLoader;

    /**
     * Tworzy menu główne z podanymi podmenu.
     *
     * @param io             konsola, nie może być {@code null}
     * @param bookMenu       menu katalogu książek, nie może być {@code null}
     * @param memberMenu     menu czytelników, nie może być {@code null}
     * @param loanMenu       menu wypożyczeń, nie może być {@code null}
     * @param demoDataLoader loader danych przykładowych, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś argument jest {@code null}
     */
    public MainMenu(ConsoleIO io, BookMenu bookMenu, MemberMenu memberMenu, LoanMenu loanMenu,
                    DemoDataLoader demoDataLoader) {
        if (io == null || bookMenu == null || memberMenu == null || loanMenu == null || demoDataLoader == null) {
            throw new IllegalArgumentException("Konsola, podmenu i loader danych nie mogą być puste");
        }
        this.io = io;
        this.bookMenu = bookMenu;
        this.memberMenu = memberMenu;
        this.loanMenu = loanMenu;
        this.demoDataLoader = demoDataLoader;
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
            io.println("3. Wypożyczenia");
            io.println("4. Załaduj dane przykładowe");
            io.println("0. Wyjście");
            int choice = io.readInt("Wybór: ", 0, 4);
            switch (choice) {
                case 1 -> bookMenu.run();
                case 2 -> memberMenu.run();
                case 3 -> loanMenu.run();
                case 4 -> MenuSupport.runSafely(io, this::loadDemoData);
                case 0 -> {
                    io.println("Do zobaczenia!");
                    return;
                }
            }
        }
    }

    /**
     * Ładuje dane przykładowe (tylko do pustej biblioteki) i wypisuje podsumowanie.
     */
    private void loadDemoData() {
        DemoDataLoader.DemoSummary summary = demoDataLoader.load();
        io.println("Załadowano dane przykładowe: " + summary.titles() + " tytułów (" + summary.copies()
                + " egz.), " + summary.members() + " czytelników, " + summary.loans()
                + " wypożyczeń (w tym jedno po terminie i jedno zwrócone).");
    }
}
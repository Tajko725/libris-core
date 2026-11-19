package pl.tajko.library;

import pl.tajko.library.demo.DemoDataLoader;
import pl.tajko.library.repository.RepositoryException;
import pl.tajko.library.ui.*;

import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Punkt wejścia aplikacji konsolowej Libris.
 * <p>
 * Folder z danymi można podać jako pierwszy argument. Bez argumentu program używa folderu
 * {@value #DEFAULT_DATA_DIR} obok miejsca uruchomienia.
 */
public final class Main {

    /** Domyślny folder z plikami danych. */
    static final String DEFAULT_DATA_DIR = "data";

    /**
     * Klasa startowa, nie tworzy się jej obiektów.
     */
    private Main() {
    }

    /**
     * Uruchamia aplikację.
     *
     * @param args opcjonalnie: folder z plikami danych
     */
    public static void main(String[] args) {
        ConsoleIO io = new ConsoleIO(System.in, System.out, inputCharset());
        Path dataDir = resolveDataDir(args);
        try {
            Clock clock = Clock.systemDefaultZone();
            LibraryContext context = new LibraryContext(dataDir, clock);
            io.println("Folder z danymi: " + dataDir.toAbsolutePath());
            MainMenu mainMenu = new MainMenu(io,
                    new BookMenu(io, context.bookService()),
                    new MemberMenu(io, context.memberService()),
                    new LoanMenu(io, context.loanService(), context.memberService(), context.bookService()),
                    new DemoDataLoader(context.bookService(), context.memberService(), context.loanService(),
                            context.memberRepository(), context.loanRepository(), clock));
            mainMenu.run();
        } catch (InputClosedException e) {
            io.println();
            io.println("Koniec danych wejściowych, kończę program.");
        } catch (RepositoryException | UncheckedIOException e) {
            io.printError("nie można uruchomić programu: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Ustala folder z danymi na podstawie argumentów programu.
     *
     * @param args argumenty programu
     * @return folder podany jako pierwszy argument lub domyślny, jeśli argumentu brak albo jest pusty
     */
    static Path resolveDataDir(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return Path.of(args[0].strip());
        }
        return Path.of(DEFAULT_DATA_DIR);
    }

    /**
     * Ustala kodowanie, w którym konsola przekazuje znaki wejściowe (ważne dla polskich liter).
     *
     * @return kodowanie z właściwości {@code stdin.encoding} lub domyślne kodowanie systemu
     */
    private static Charset inputCharset() {
        String name = System.getProperty("stdin.encoding");
        if (name != null) {
            try {
                return Charset.forName(name);
            } catch (IllegalArgumentException e) {
                // nieznane kodowanie, więc używamy domyślnego poniżej
            }
        }
        return Charset.defaultCharset();
    }
}
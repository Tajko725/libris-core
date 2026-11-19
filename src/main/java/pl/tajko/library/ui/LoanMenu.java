package pl.tajko.library.ui;

import pl.tajko.library.model.Loan;
import pl.tajko.library.service.BookService;
import pl.tajko.library.service.BookSummary;
import pl.tajko.library.service.LoanService;
import pl.tajko.library.service.LoanSummary;
import pl.tajko.library.service.MemberService;
import pl.tajko.library.service.MemberSummary;

import java.util.List;

/**
 * Menu konsolowe wypożyczeń i zwrotów.
 * <p>
 * Wypożyczanie: wybieram czytelnika z listy, potem tytuł z katalogu (egzemplarz wybiera serwis).
 * Zwrot: wybieram czytelnika z niezwróconymi książkami, potem jedno z jego wypożyczeń.
 * Listy pokazują status tekstem: "w terminie", "PO TERMINIE" albo "zwrócona (data)".
 * Błędy reguł biblioteki są wypisywane jako komunikaty, a menu działa dalej.
 */
public class LoanMenu {

    /** Konsola, z której menu czyta i na którą pisze. */
    private final ConsoleIO io;

    /** Serwis wypożyczeń. */
    private final LoanService loanService;

    /** Serwis czytelników, potrzebny do wyboru czytelnika. */
    private final MemberService memberService;

    /** Serwis książek, potrzebny do wyboru tytułu. */
    private final BookService bookService;

    /**
     * Tworzy menu korzystające z podanej konsoli i serwisów.
     *
     * @param io            konsola, nie może być {@code null}
     * @param loanService   serwis wypożyczeń, nie może być {@code null}
     * @param memberService serwis czytelników, nie może być {@code null}
     * @param bookService   serwis książek, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś argument jest {@code null}
     */
    public LoanMenu(ConsoleIO io, LoanService loanService, MemberService memberService, BookService bookService) {
        if (io == null || loanService == null || memberService == null || bookService == null) {
            throw new IllegalArgumentException("Konsola i serwisy nie mogą być puste");
        }
        this.io = io;
        this.loanService = loanService;
        this.memberService = memberService;
        this.bookService = bookService;
    }

    /**
     * Wyświetla menu i obsługuje wybory użytkownika, aż wybierze powrót.
     */
    public void run() {
        while (true) {
            io.println();
            io.println("--- Wypożyczenia ---");
            io.println("1. Wypożycz książkę");
            io.println("2. Zwróć książkę");
            io.println("3. Aktywne wypożyczenia");
            io.println("4. Wypożyczenia po terminie");
            io.println("5. Historia wypożyczeń");
            io.println("0. Powrót");
            int choice = io.readInt("Wybór: ", 0, 5);
            switch (choice) {
                case 1 -> MenuSupport.runSafely(io, this::borrow);
                case 2 -> MenuSupport.runSafely(io, this::returnBook);
                case 3 -> MenuSupport.runSafely(io, this::showActive);
                case 4 -> MenuSupport.runSafely(io, this::showOverdue);
                case 5 -> MenuSupport.runSafely(io, this::showHistory);
                case 0 -> {
                    return;
                }
            }
        }
    }

    /**
     * Wypożycza książkę: wybór czytelnika, potem tytułu z katalogu.
     */
    private void borrow() {
        List<MemberSummary> members = memberService.getMembers();
        if (members.isEmpty()) {
            io.println("Brak czytelników.");
            return;
        }
        printMembers(members);
        int memberNumber = io.readInt("Numer czytelnika (0 = anuluj): ", 0, members.size());
        if (memberNumber == 0) {
            return;
        }
        MemberSummary member = members.get(memberNumber - 1);

        List<BookSummary> catalog = bookService.getCatalog();
        if (catalog.isEmpty()) {
            io.println("Katalog jest pusty.");
            return;
        }
        printCatalog(catalog);
        int bookNumber = io.readInt("Numer książki (0 = anuluj): ", 0, catalog.size());
        if (bookNumber == 0) {
            return;
        }
        BookSummary book = catalog.get(bookNumber - 1);

        Loan loan = loanService.borrow(member.id(), book.isbn());
        io.println("Wypożyczono \"" + book.title() + "\" czytelnikowi " + member.fullName()
                + ". Termin zwrotu: " + loan.getDueDate() + ".");
    }

    /**
     * Zwraca książkę: wybór czytelnika z niezwróconymi książkami, potem jego wypożyczenia.
     */
    private void returnBook() {
        List<MemberSummary> borrowers = memberService.getMembers().stream()
                .filter(member -> member.activeLoans() > 0)
                .toList();
        if (borrowers.isEmpty()) {
            io.println("Nikt nie ma wypożyczonych książek.");
            return;
        }
        printMembers(borrowers);
        int memberNumber = io.readInt("Numer czytelnika (0 = anuluj): ", 0, borrowers.size());
        if (memberNumber == 0) {
            return;
        }
        MemberSummary member = borrowers.get(memberNumber - 1);

        List<LoanSummary> loans = loanService.getMemberLoans(member.id());
        if (loans.isEmpty()) {
            io.println("Ten czytelnik nie ma wypożyczonych książek.");
            return;
        }
        printLoans(loans);
        int loanNumber = io.readInt("Numer wypożyczenia do zwrotu (0 = anuluj): ", 0, loans.size());
        if (loanNumber == 0) {
            return;
        }
        LoanSummary loan = loans.get(loanNumber - 1);

        loanService.returnLoan(loan.loanId());
        io.println("Zwrócono \"" + loan.bookTitle() + "\"" + (loan.overdue() ? " (zwrot po terminie)." : "."));
    }

    /**
     * Pokazuje aktywne wypożyczenia.
     */
    private void showActive() {
        show(loanService.getActiveLoans(), "Brak aktywnych wypożyczeń.");
    }

    /**
     * Pokazuje wypożyczenia po terminie zwrotu.
     */
    private void showOverdue() {
        show(loanService.getOverdueLoans(), "Brak wypożyczeń po terminie.");
    }

    /**
     * Pokazuje pełną historię wypożyczeń.
     */
    private void showHistory() {
        show(loanService.getHistory(), "Brak wypożyczeń w historii.");
    }

    /**
     * Pokazuje tabelę wypożyczeń albo komunikat, gdy lista jest pusta.
     *
     * @param loans        wypożyczenia do pokazania
     * @param emptyMessage komunikat wypisywany, gdy lista jest pusta
     */
    private void show(List<LoanSummary> loans, String emptyMessage) {
        if (loans.isEmpty()) {
            io.println(emptyMessage);
            return;
        }
        printLoans(loans);
    }

    /**
     * Wypisuje tabelę z numerowanymi czytelnikami i liczbą ich wypożyczeń.
     *
     * @param members czytelnicy do wypisania
     */
    private void printMembers(List<MemberSummary> members) {
        io.println();
        io.printf("%-4s %-28s %s%n", "Lp.", "Czytelnik", "Wypożyczenia");
        io.println("-".repeat(60));
        for (int i = 0; i < members.size(); i++) {
            MemberSummary member = members.get(i);
            String loans = member.hasOverdueLoans()
                    ? member.activeLoans() + " (po terminie: " + member.overdueLoans() + ")"
                    : String.valueOf(member.activeLoans());
            io.printf("%-4s %-28s %s%n", (i + 1) + ".", TextFormat.shorten(member.fullName(), 28), loans);
        }
    }

    /**
     * Wypisuje tabelę z numerowanymi tytułami i liczbą wolnych egzemplarzy.
     *
     * @param catalog tytuły do wypisania
     */
    private void printCatalog(List<BookSummary> catalog) {
        io.println();
        io.printf("%-4s %-30s %-22s %s%n", "Lp.", "Tytuł", "Autor", "Wolne/razem");
        io.println("-".repeat(70));
        for (int i = 0; i < catalog.size(); i++) {
            BookSummary book = catalog.get(i);
            io.printf("%-4s %-30s %-22s %d/%d%n",
                    (i + 1) + ".",
                    TextFormat.shorten(book.title(), 30),
                    TextFormat.shorten(book.author(), 22),
                    book.availableCopies(),
                    book.totalCopies());
        }
    }

    /**
     * Wypisuje tabelę z numerowanymi wypożyczeniami i ich statusem.
     *
     * @param loans wypożyczenia do wypisania
     */
    private void printLoans(List<LoanSummary> loans) {
        io.println();
        io.printf("%-4s %-28s %-22s %-11s %-11s %s%n", "Lp.", "Książka", "Czytelnik", "Od", "Termin", "Status");
        io.println("-".repeat(98));
        for (int i = 0; i < loans.size(); i++) {
            LoanSummary loan = loans.get(i);
            io.printf("%-4s %-28s %-22s %-11s %-11s %s%n",
                    (i + 1) + ".",
                    TextFormat.shorten(loan.bookTitle(), 28),
                    TextFormat.shorten(loan.memberName(), 22),
                    loan.loanDate(),
                    loan.dueDate(),
                    status(loan));
        }
    }

    /**
     * Zamienia stan wypożyczenia na tekst do tabeli.
     *
     * @param loan wypożyczenie
     * @return "zwrócona (data)", "PO TERMINIE" albo "w terminie"
     */
    private static String status(LoanSummary loan) {
        if (loan.returnDate() != null) {
            return "zwrócona " + loan.returnDate();
        }
        return loan.overdue() ? "PO TERMINIE" : "w terminie";
    }
}
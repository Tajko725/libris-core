package pl.tajko.library.ui;

import pl.tajko.library.model.Member;
import pl.tajko.library.service.MemberService;
import pl.tajko.library.service.MemberSummary;

import java.util.List;
import java.util.Optional;

/**
 * Menu konsolowe czytelników: lista, wyszukiwanie, dodawanie, edycja i usuwanie.
 * <p>
 * Błędy reguł biblioteki (np. zajęty adres e-mail albo usuwanie czytelnika z niezwróconymi książkami)
 * są wypisywane jako komunikaty, a menu działa dalej.
 */
public class MemberMenu {

    /** Konsola, z której menu czyta i na którą pisze. */
    private final ConsoleIO io;

    /** Serwis czytelników. */
    private final MemberService memberService;

    /**
     * Tworzy menu korzystające z podanej konsoli i serwisu.
     *
     * @param io            konsola, nie może być {@code null}
     * @param memberService serwis czytelników, nie może być {@code null}
     * @throws IllegalArgumentException jeśli któryś argument jest {@code null}
     */
    public MemberMenu(ConsoleIO io, MemberService memberService) {
        if (io == null || memberService == null) {
            throw new IllegalArgumentException("Konsola i serwis nie mogą być puste");
        }
        this.io = io;
        this.memberService = memberService;
    }

    /**
     * Wyświetla menu i obsługuje wybory użytkownika, aż wybierze powrót.
     */
    public void run() {
        while (true) {
            io.println();
            io.println("--- Czytelnicy ---");
            io.println("1. Pokaż czytelników");
            io.println("2. Szukaj czytelnika");
            io.println("3. Dodaj czytelnika");
            io.println("4. Edytuj czytelnika");
            io.println("5. Usuń czytelnika");
            io.println("0. Powrót");
            int choice = io.readInt("Wybór: ", 0, 5);
            switch (choice) {
                case 1 -> MenuSupport.runSafely(io, this::showMembers);
                case 2 -> MenuSupport.runSafely(io, this::search);
                case 3 -> MenuSupport.runSafely(io, this::addMember);
                case 4 -> MenuSupport.runSafely(io, this::editMember);
                case 5 -> MenuSupport.runSafely(io, this::deleteMember);
                case 0 -> {
                    return;
                }
            }
        }
    }

    /**
     * Pokazuje wszystkich czytelników.
     */
    private void showMembers() {
        List<MemberSummary> members = memberService.getMembers();
        if (members.isEmpty()) {
            io.println("Brak czytelników.");
            return;
        }
        printTable(members);
    }

    /**
     * Wyszukuje czytelników po frazie i pokazuje wyniki.
     */
    private void search() {
        String query = io.readLine("Fraza: ");
        List<MemberSummary> found = memberService.search(query);
        if (found.isEmpty()) {
            io.println("Nic nie znaleziono.");
            return;
        }
        printTable(found);
    }

    /**
     * Dodaje nowego czytelnika.
     */
    private void addMember() {
        String firstName = io.readLine("Imię: ");
        String lastName = io.readLine("Nazwisko: ");
        String email = io.readLine("E-mail: ");

        Member member = memberService.addMember(firstName, lastName, email);
        io.println("Dodano czytelnika " + member.getFullName() + ".");
    }

    /**
     * Zmienia dane wybranego czytelnika. Enter zostawia dotychczasową wartość.
     */
    private void editMember() {
        Optional<MemberSummary> selected = selectMember("Numer czytelnika");
        if (selected.isEmpty()) {
            return;
        }
        MemberSummary current = selected.get();
        io.println("Enter zostawia wartość w nawiasie.");
        String firstName = io.readLineOrDefault("Imię", current.firstName());
        String lastName = io.readLineOrDefault("Nazwisko", current.lastName());
        String email = io.readLineOrDefault("E-mail", current.email());

        Member updated = memberService.updateMember(current.id(), firstName, lastName, email);
        io.println("Zaktualizowano dane czytelnika " + updated.getFullName() + ".");
    }

    /**
     * Usuwa wybranego czytelnika po potwierdzeniu. Czytelnik z niezwróconymi książkami nie może zostać usunięty.
     */
    private void deleteMember() {
        Optional<MemberSummary> selected = selectMember("Numer czytelnika");
        if (selected.isEmpty()) {
            return;
        }
        MemberSummary member = selected.get();
        if (!io.confirm("Usunąć czytelnika " + member.fullName())) {
            io.println("Anulowano.");
            return;
        }
        memberService.deleteMember(member.id());
        io.println("Usunięto czytelnika " + member.fullName() + ".");
    }

    /**
     * Pokazuje listę czytelników i prosi o wybór jednego z nich.
     *
     * @param prompt początek zachęty, np. "Numer czytelnika"
     * @return wybrany czytelnik albo pusty wynik, gdy nie ma czytelników lub użytkownik anulował
     */
    private Optional<MemberSummary> selectMember(String prompt) {
        List<MemberSummary> members = memberService.getMembers();
        if (members.isEmpty()) {
            io.println("Brak czytelników.");
            return Optional.empty();
        }
        printTable(members);
        int number = io.readInt(prompt + " (0 = anuluj): ", 0, members.size());
        return number == 0 ? Optional.empty() : Optional.of(members.get(number - 1));
    }

    /**
     * Wypisuje tabelę z numerowanymi czytelnikami.
     *
     * @param members czytelnicy do wypisania
     */
    private void printTable(List<MemberSummary> members) {
        io.println();
        io.printf("%-4s %-28s %-36s %-11s %s%n", "Lp.", "Czytelnik", "E-mail", "Od", "Wypożyczenia");
        io.println("-".repeat(94));
        for (int i = 0; i < members.size(); i++) {
            MemberSummary member = members.get(i);
            String loans = member.hasOverdueLoans()
                    ? member.activeLoans() + " (po terminie: " + member.overdueLoans() + ")"
                    : String.valueOf(member.activeLoans());
            io.printf("%-4s %-28s %-36s %-11s %s%n",
                    (i + 1) + ".",
                    TextFormat.shorten(member.fullName(), 28),
                    TextFormat.shorten(member.email(), 36),
                    member.registeredAt(),
                    loans);
        }
    }
}
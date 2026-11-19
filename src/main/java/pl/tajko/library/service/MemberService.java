package pl.tajko.library.service;

import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.exception.EntityNotFoundException;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;
import pl.tajko.library.repository.Repository;
import pl.tajko.library.util.TextNormalizer;

import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serwis zarządzający czytelnikami biblioteki.
 * <p>
 * Pilnuje, żeby adres e-mail był unikalny wśród czytelników, oraz blokuje usunięcie czytelnika,
 * który ma aktywne wypożyczenia. Zwrócone wypożyczenia pozostają w historii z identyfikatorem
 * usuniętego czytelnika.
 * <p>
 * Zależności serwis dostaje przez konstruktor. Zegar ({@link Clock}) jest wstrzykiwany po to,
 * aby w testach można było użyć stałej daty.
 */
public class MemberService {

    /** Porównywacz tekstów według polskiego alfabetu (ł po l, ż po z). */
    private static final Collator POLISH_COLLATOR = Collator.getInstance(Locale.of("pl", "PL"));

    /** Kolejność na liście: alfabetycznie po nazwisku, potem po imieniu. */
    private static final Comparator<MemberSummary> MEMBER_ORDER = Comparator
            .comparing(MemberSummary::lastName, POLISH_COLLATOR)
            .thenComparing(MemberSummary::firstName, POLISH_COLLATOR);

    /** Repozytorium czytelników. */
    private final Repository<Member> memberRepository;

    /** Repozytorium wypożyczeń, używane do liczenia wypożyczeń i blokady usuwania. */
    private final Repository<Loan> loanRepository;

    /** Zegar, z którego serwis odczytuje dzisiejszą datę. */
    private final Clock clock;

    /**
     * Tworzy serwis korzystający z podanych zależności.
     *
     * @param memberRepository repozytorium czytelników, nie może być {@code null}
     * @param loanRepository   repozytorium wypożyczeń, nie może być {@code null}
     * @param clock            zegar, nie może być {@code null}
     * @throws IllegalArgumentException jeśli którakolwiek zależność jest {@code null}
     */
    public MemberService(Repository<Member> memberRepository, Repository<Loan> loanRepository, Clock clock) {
        if (memberRepository == null || loanRepository == null || clock == null) {
            throw new IllegalArgumentException("Repozytoria i zegar nie mogą być puste");
        }
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    /**
     * Rejestruje nowego czytelnika z dzisiejszą datą rejestracji.
     *
     * @param firstName imię
     * @param lastName  nazwisko
     * @param email     adres e-mail, unikalny wśród czytelników
     * @return zapisany czytelnik
     * @throws IllegalArgumentException jeśli dane są niepoprawne
     * @throws BusinessRuleException    jeśli inny czytelnik ma już ten adres e-mail
     */
    public Member addMember(String firstName, String lastName, String email) {
        Member member = new Member(firstName, lastName, email, LocalDate.now(clock));
        requireEmailNotUsed(member.getEmail(), null);
        memberRepository.save(member);
        return member;
    }

    /**
     * Zmienia imię, nazwisko i adres e-mail czytelnika. Data rejestracji pozostaje bez zmian.
     * Nowe dane są sprawdzane przed zmianą czytelnika.
     *
     * @param memberId  identyfikator czytelnika
     * @param firstName nowe imię
     * @param lastName  nowe nazwisko
     * @param email     nowy adres e-mail, unikalny wśród pozostałych czytelników
     * @return zaktualizowany czytelnik
     * @throws EntityNotFoundException  jeśli czytelnik nie istnieje
     * @throws IllegalArgumentException jeśli nowe dane są niepoprawne
     * @throws BusinessRuleException    jeśli inny czytelnik ma już ten adres e-mail
     */
    public Member updateMember(UUID memberId, String firstName, String lastName, String email) {
        Member member = requireMember(memberId);
        // Obiekt pomocniczy: jego konstruktor sprawdza nowe dane, zanim zmienimy czytelnika.
        Member validated = new Member(firstName, lastName, email, member.getRegisteredAt());
        requireEmailNotUsed(validated.getEmail(), member.getId());
        member.setFirstName(validated.getFirstName());
        member.setLastName(validated.getLastName());
        member.setEmail(validated.getEmail());
        memberRepository.save(member);
        return member;
    }

    /**
     * Usuwa czytelnika. Czytelnik z aktywnymi wypożyczeniami nie może zostać usunięty.
     * Zwrócone wypożyczenia pozostają w historii.
     *
     * @param memberId identyfikator czytelnika
     * @throws EntityNotFoundException jeśli czytelnik nie istnieje
     * @throws BusinessRuleException   jeśli czytelnik ma niezwrócone książki
     */
    public void deleteMember(UUID memberId) {
        Member member = requireMember(memberId);
        if (hasActiveLoan(member.getId())) {
            throw new BusinessRuleException(
                    "Czytelnik " + member.getFullName() + " ma niezwrócone książki i nie można go usunąć");
        }
        memberRepository.deleteById(member.getId());
    }

    /**
     * Zwraca czytelnika o podanym identyfikatorze.
     *
     * @param memberId identyfikator czytelnika
     * @return znaleziony czytelnik
     * @throws EntityNotFoundException jeśli czytelnik nie istnieje
     */
    public Member getMember(UUID memberId) {
        return requireMember(memberId);
    }

    /**
     * Zwraca listę czytelników z liczbą aktywnych i przeterminowanych wypożyczeń.
     * Lista jest posortowana według nazwiska, a potem imienia (polski alfabet).
     * Przeterminowanie liczone jest względem dzisiejszej daty z zegara serwisu.
     *
     * @return lista podsumowań, pusta jeśli nie ma czytelników
     */
    public List<MemberSummary> getMembers() {
        LocalDate today = LocalDate.now(clock);
        Map<UUID, List<Loan>> activeLoansByMember = loanRepository.findAll().stream()
                .filter(Loan::isActive)
                .collect(Collectors.groupingBy(Loan::getMemberId));
        return memberRepository.findAll().stream()
                .map(member -> toSummary(member, activeLoansByMember.getOrDefault(member.getId(), List.of()), today))
                .sorted(MEMBER_ORDER)
                .toList();
    }

    /**
     * Wyszukuje czytelników po fragmencie imienia, nazwiska (także "imię nazwisko") lub adresu e-mail.
     * Wielkość liter i polskie znaki diakrytyczne są ignorowane.
     *
     * @param query szukana fraza, nie może być pusta
     * @return pasujące podsumowania, puste jeśli nic nie pasuje
     * @throws IllegalArgumentException jeśli fraza jest pusta
     */
    public List<MemberSummary> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Fraza wyszukiwania nie może być pusta");
        }
        String text = TextNormalizer.normalize(query);
        return getMembers().stream()
                .filter(summary -> matches(summary, text))
                .toList();
    }

    /**
     * Sprawdza, czy podsumowanie pasuje do szukanej frazy.
     *
     * @param summary sprawdzane podsumowanie
     * @param text    znormalizowana szukana fraza
     * @return {@code true}, jeśli imię, nazwisko, "imię nazwisko", "nazwisko imię" lub e-mail zawiera frazę
     */
    private static boolean matches(MemberSummary summary, String text) {
        String firstName = TextNormalizer.normalize(summary.firstName());
        String lastName = TextNormalizer.normalize(summary.lastName());
        return firstName.contains(text)
                || lastName.contains(text)
                || (firstName + " " + lastName).contains(text)
                || (lastName + " " + firstName).contains(text)
                || TextNormalizer.normalize(summary.email()).contains(text);
    }

    /**
     * Buduje podsumowanie czytelnika na podstawie jego aktywnych wypożyczeń.
     *
     * @param member       czytelnik
     * @param activeLoans  aktywne wypożyczenia tego czytelnika
     * @param today        dzisiejsza data użyta do sprawdzenia przeterminowania
     * @return podsumowanie z liczbą wypożyczeń aktywnych i przeterminowanych
     */
    private static MemberSummary toSummary(Member member, List<Loan> activeLoans, LocalDate today) {
        int overdue = (int) activeLoans.stream().filter(loan -> loan.isOverdue(today)).count();
        return new MemberSummary(member.getId(), member.getFirstName(), member.getLastName(),
                member.getEmail(), member.getRegisteredAt(), activeLoans.size(), overdue);
    }

    /**
     * Sprawdza, że żaden inny czytelnik nie używa podanego adresu e-mail.
     *
     * @param email            znormalizowany adres e-mail
     * @param excludedMemberId identyfikator czytelnika pomijanego w porównaniu (przy edycji) lub {@code null}
     * @throws BusinessRuleException jeśli adres jest już używany
     */
    private void requireEmailNotUsed(String email, UUID excludedMemberId) {
        boolean used = memberRepository.findAll().stream()
                .anyMatch(other -> !other.getId().equals(excludedMemberId) && other.getEmail().equals(email));
        if (used) {
            throw new BusinessRuleException("Adres e-mail " + email + " jest już używany przez innego czytelnika");
        }
    }

    /**
     * Zwraca czytelnika o podanym identyfikatorze lub zgłasza błąd, jeśli nie istnieje.
     *
     * @param memberId identyfikator czytelnika
     * @return znaleziony czytelnik
     * @throws EntityNotFoundException jeśli czytelnik nie istnieje
     */
    private Member requireMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono czytelnika o identyfikatorze " + memberId));
    }

    /**
     * Sprawdza, czy czytelnik ma aktywne wypożyczenie.
     *
     * @param memberId identyfikator czytelnika
     * @return {@code true}, jeśli czytelnik ma niezwróconą książkę
     */
    private boolean hasActiveLoan(UUID memberId) {
        return loanRepository.findAll().stream()
                .anyMatch(loan -> loan.isActive() && loan.getMemberId().equals(memberId));
    }
}
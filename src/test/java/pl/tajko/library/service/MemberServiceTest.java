package pl.tajko.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tajko.library.exception.BusinessRuleException;
import pl.tajko.library.exception.EntityNotFoundException;
import pl.tajko.library.model.Loan;
import pl.tajko.library.model.Member;
import pl.tajko.library.repository.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testy jednostkowe klasy {@link MemberService}.
 * <p>
 * Repozytoria są zastąpione atrapami Mockito, a zegar ma stałą datę (2026-04-15),
 * więc testy dają zawsze ten sam wynik. Ustawienia atrap ({@code when}) są w samych testach,
 * ponieważ ścisłe stuby Mockito zgłaszają błąd, gdy ustawione zachowanie nie zostanie użyte.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

    /** Dzisiejsza data w testach. */
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 15);

    /** Stały zegar ustawiony na {@link #TODAY} (w strefie Europe/Warsaw). */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));

    /** Atrapa repozytorium czytelników. */
    @Mock
    private Repository<Member> memberRepository;

    /** Atrapa repozytorium wypożyczeń. */
    @Mock
    private Repository<Loan> loanRepository;

    /** Testowany serwis. */
    private MemberService service;

    /**
     * Tworzy serwis z atrapami i stałym zegarem przed każdym testem.
     */
    @BeforeEach
    void setUp() {
        service = new MemberService(memberRepository, loanRepository, CLOCK);
    }

    /**
     * Konstruktor powinien odrzucać puste zależności.
     */
    @Test
    @DisplayName("konstruktor odrzuca puste repozytoria i zegar")
    void shouldRejectNullDependencies() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MemberService(null, loanRepository, CLOCK)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MemberService(memberRepository, null, CLOCK)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MemberService(memberRepository, loanRepository, null))
        );
    }

    /**
     * Testy rejestracji czytelnika.
     */
    @Nested
    @DisplayName("addMember")
    class AddMemberTests {

        /**
         * Nowy czytelnik powinien dostać datę rejestracji z zegara i zostać zapisany.
         */
        @Test
        @DisplayName("zapisuje czytelnika z dzisiejszą datą rejestracji")
        void shouldSaveMemberWithTodayAsRegistrationDate() {
            Member result = service.addMember("Jan", "Kowalski", "Jan.Kowalski@Example.com");

            ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals(result, captor.getValue()),
                    () -> assertEquals("Jan", result.getFirstName()),
                    () -> assertEquals("Kowalski", result.getLastName()),
                    () -> assertEquals("jan.kowalski@example.com", result.getEmail()),
                    () -> assertEquals(TODAY, result.getRegisteredAt())
            );
        }

        /**
         * Adres e-mail używany już przez innego czytelnika (bez względu na wielkość liter) powinien być odrzucony.
         */
        @Test
        @DisplayName("odrzuca zajęty adres e-mail i niczego nie zapisuje")
        void shouldRejectDuplicateEmail() {
            when(memberRepository.findAll()).thenReturn(List.of(member("Anna", "Nowak", "jan@example.com")));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class,
                            () -> service.addMember("Jan", "Kowalski", "JAN@Example.com")),
                    () -> verify(memberRepository, never()).save(any(Member.class))
            );
        }

        /**
         * Niepoprawne dane powinny być odrzucone przed jakąkolwiek operacją na repozytorium.
         *
         * @param email niepoprawny adres e-mail
         */
        @ParameterizedTest(name = "odrzuca adres e-mail: [{0}]")
        @ValueSource(strings = {"", " ", "abc", "a@b", "a@@b.pl"})
        void shouldRejectInvalidEmail(String email) {
            assertThrows(IllegalArgumentException.class, () -> service.addMember("Jan", "Kowalski", email));

            verifyNoInteractions(memberRepository);
        }

        /**
         * Puste imię lub nazwisko powinno być odrzucone przed jakąkolwiek operacją na repozytorium.
         */
        @Test
        @DisplayName("odrzuca puste imię i nazwisko")
        void shouldRejectBlankNames() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> service.addMember(" ", "Kowalski", "jan@example.com")),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> service.addMember("Jan", null, "jan@example.com")),
                    () -> verifyNoInteractions(memberRepository)
            );
        }
    }

    /**
     * Testy edycji czytelnika.
     */
    @Nested
    @DisplayName("updateMember")
    class UpdateMemberTests {

        /**
         * Poprawne dane powinny zostać zapisane, a data rejestracji pozostać bez zmian.
         */
        @Test
        @DisplayName("zmienia dane czytelnika i zapisuje go")
        void shouldUpdateMember() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            LocalDate registeredAt = member.getRegisteredAt();
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(memberRepository.findAll()).thenReturn(List.of(member));

            Member result = service.updateMember(member.getId(), " Piotr ", "Zieliński", "Piotr@Example.com");

            assertAll(
                    () -> assertEquals(member, result),
                    () -> assertEquals("Piotr", member.getFirstName()),
                    () -> assertEquals("Zieliński", member.getLastName()),
                    () -> assertEquals("piotr@example.com", member.getEmail()),
                    () -> assertEquals(registeredAt, member.getRegisteredAt()),
                    () -> verify(memberRepository).save(member)
            );
        }

        /**
         * Pozostawienie własnego adresu e-mail nie powinno być traktowane jako duplikat.
         */
        @Test
        @DisplayName("pozwala zachować własny adres e-mail")
        void shouldAllowKeepingOwnEmail() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(memberRepository.findAll()).thenReturn(List.of(member));

            service.updateMember(member.getId(), "Janusz", "Kowalski", "jan@example.com");

            assertAll(
                    () -> assertEquals("Janusz", member.getFirstName()),
                    () -> verify(memberRepository).save(member)
            );
        }

        /**
         * Adres e-mail innego czytelnika powinien być odrzucony, a dane czytelnika pozostać bez zmian.
         */
        @Test
        @DisplayName("odrzuca adres e-mail innego czytelnika i niczego nie zmienia")
        void shouldRejectEmailOfAnotherMember() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            Member other = member("Anna", "Nowak", "anna@example.com");
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(memberRepository.findAll()).thenReturn(List.of(member, other));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class,
                            () -> service.updateMember(member.getId(), "Jan", "Kowalski", "ANNA@example.com")),
                    () -> assertEquals("jan@example.com", member.getEmail()),
                    () -> verify(memberRepository, never()).save(any(Member.class))
            );
        }

        /**
         * Niepoprawne nowe dane powinny zostać odrzucone przed zmianą czytelnika.
         */
        @Test
        @DisplayName("niepoprawne dane nie zmieniają czytelnika")
        void shouldNotChangeMemberWhenNewDataIsInvalid() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> service.updateMember(member.getId(), "Piotr", " ", "piotr@example.com")),
                    () -> assertEquals("Jan", member.getFirstName()),
                    () -> assertEquals("jan@example.com", member.getEmail()),
                    () -> verify(memberRepository, never()).save(any(Member.class))
            );
        }

        /**
         * Nieistniejący czytelnik powinien zgłosić błąd "nie znaleziono".
         */
        @Test
        @DisplayName("zgłasza błąd dla nieistniejącego czytelnika")
        void shouldFailForUnknownMember() {
            UUID id = UUID.randomUUID();
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(EntityNotFoundException.class,
                            () -> service.updateMember(id, "Jan", "Kowalski", "jan@example.com")),
                    () -> verify(memberRepository, never()).save(any(Member.class))
            );
        }
    }

    /**
     * Testy usuwania czytelnika.
     */
    @Nested
    @DisplayName("deleteMember")
    class DeleteMemberTests {

        /**
         * Czytelnik bez wypożyczeń powinien zostać usunięty.
         */
        @Test
        @DisplayName("usuwa czytelnika bez wypożyczeń")
        void shouldDeleteMemberWithoutLoans() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

            service.deleteMember(member.getId());

            verify(memberRepository).deleteById(member.getId());
        }

        /**
         * Zwrócone wypożyczenia tego czytelnika i aktywne wypożyczenia innych osób nie powinny blokować usunięcia.
         */
        @Test
        @DisplayName("pozwala usunąć czytelnika, który ma tylko zwrócone wypożyczenia")
        void shouldDeleteMemberWithOnlyReturnedLoans() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            Member other = member("Anna", "Nowak", "anna@example.com");
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(loanRepository.findAll()).thenReturn(
                    List.of(returnedLoan(member), activeLoan(other, LocalDate.of(2026, 4, 1))));

            service.deleteMember(member.getId());

            verify(memberRepository).deleteById(member.getId());
        }

        /**
         * Czytelnik z aktywnym wypożyczeniem nie może zostać usunięty.
         */
        @Test
        @DisplayName("odrzuca usunięcie czytelnika z niezwróconą książką")
        void shouldRejectDeletingMemberWithActiveLoan() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(loanRepository.findAll()).thenReturn(List.of(activeLoan(member, LocalDate.of(2026, 4, 1))));

            assertAll(
                    () -> assertThrows(BusinessRuleException.class, () -> service.deleteMember(member.getId())),
                    () -> verify(memberRepository, never()).deleteById(any(UUID.class))
            );
        }

        /**
         * Nieistniejący czytelnik powinien zgłosić błąd i nie sprawdzać wypożyczeń.
         */
        @Test
        @DisplayName("zgłasza błąd dla nieistniejącego czytelnika")
        void shouldFailForUnknownMember() {
            UUID id = UUID.randomUUID();
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(EntityNotFoundException.class, () -> service.deleteMember(id)),
                    () -> verify(memberRepository, never()).deleteById(any(UUID.class)),
                    () -> verifyNoInteractions(loanRepository)
            );
        }
    }

    /**
     * Testy pobierania pojedynczego czytelnika.
     */
    @Nested
    @DisplayName("getMember")
    class GetMemberTests {

        /**
         * Istniejący czytelnik powinien zostać zwrócony.
         */
        @Test
        @DisplayName("zwraca istniejącego czytelnika")
        void shouldReturnExistingMember() {
            Member member = member("Jan", "Kowalski", "jan@example.com");
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

            assertEquals(member, service.getMember(member.getId()));
        }

        /**
         * Nieistniejący czytelnik powinien zgłosić błąd.
         */
        @Test
        @DisplayName("zgłasza błąd dla nieistniejącego czytelnika")
        void shouldFailForUnknownMember() {
            UUID id = UUID.randomUUID();
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.getMember(id));
        }
    }

    /**
     * Testy listy czytelników.
     */
    @Nested
    @DisplayName("getMembers")
    class GetMembersTests {

        /**
         * Brak czytelników powinien dać pustą listę.
         */
        @Test
        @DisplayName("zwraca pustą listę, gdy nie ma czytelników")
        void shouldReturnEmptyList() {
            assertTrue(service.getMembers().isEmpty());
        }

        /**
         * Lista powinna zawierać poprawne liczby wypożyczeń i być posortowana po nazwisku (polski alfabet).
         */
        @Test
        @DisplayName("liczy wypożyczenia aktywne i przeterminowane oraz sortuje po nazwisku")
        void shouldCountLoansAndSortByLastName() {
            Member nowak = member("Anna", "Nowak", "anna@example.com");
            Member kowalski = member("Jan", "Kowalski", "jan@example.com");
            Member lukasiewicz = member("Piotr", "Łukasiewicz", "piotr@example.com");
            when(memberRepository.findAll()).thenReturn(List.of(nowak, kowalski, lukasiewicz));
            when(loanRepository.findAll()).thenReturn(List.of(
                    activeLoan(kowalski, LocalDate.of(2026, 3, 1)),   // termin 2026-03-31: po terminie
                    activeLoan(kowalski, LocalDate.of(2026, 4, 10)),  // termin 2026-05-10: w terminie
                    returnedLoan(kowalski),                           // zwrócone: nie liczy się
                    activeLoan(lukasiewicz, LocalDate.of(2026, 4, 1))));

            List<MemberSummary> result = service.getMembers();

            assertAll(
                    () -> assertEquals(List.of("Kowalski", "Łukasiewicz", "Nowak"),
                            result.stream().map(MemberSummary::lastName).toList()),
                    () -> assertEquals(2, result.get(0).activeLoans()),
                    () -> assertEquals(1, result.get(0).overdueLoans()),
                    () -> assertTrue(result.get(0).hasOverdueLoans()),
                    () -> assertEquals(1, result.get(1).activeLoans()),
                    () -> assertEquals(0, result.get(1).overdueLoans()),
                    () -> assertFalse(result.get(1).hasOverdueLoans()),
                    () -> assertEquals(0, result.get(2).activeLoans()),
                    () -> assertEquals("Jan Kowalski", result.get(0).fullName())
            );
        }
    }

    /**
     * Testy wyszukiwania czytelników.
     */
    @Nested
    @DisplayName("search")
    class SearchTests {

        /**
         * Ustawia w atrapie repozytorium trzech czytelników.
         * Wywoływana tylko w testach, które naprawdę odczytują czytelników.
         */
        private void stubMembers() {
            when(memberRepository.findAll()).thenReturn(List.of(
                    member("Jan", "Kowalski", "jan.kowalski@example.com"),
                    member("Anna", "Nowak", "anna@example.org"),
                    member("Łukasz", "Wiśniewski", "lukasz@example.com")));
        }

        /**
         * Wyszukiwanie po imieniu powinno ignorować polskie znaki i wielkość liter.
         */
        @Test
        @DisplayName("znajduje po imieniu bez względu na polskie znaki i wielkość liter")
        void shouldFindByFirstNameIgnoringDiacritics() {
            stubMembers();

            List<MemberSummary> result = service.search("LUKASZ");

            assertEquals(List.of("Łukasz Wiśniewski"), result.stream().map(MemberSummary::fullName).toList());
        }

        /**
         * Wyszukiwanie po nazwisku powinno działać na fragmencie.
         */
        @Test
        @DisplayName("znajduje po fragmencie nazwiska")
        void shouldFindByLastNameFragment() {
            stubMembers();

            List<MemberSummary> result = service.search("wisniew");

            assertEquals(List.of("Łukasz Wiśniewski"), result.stream().map(MemberSummary::fullName).toList());
        }

        /**
         * Wyszukiwanie po "imię nazwisko" powinno działać.
         */
        @Test
        @DisplayName("znajduje po pełnej nazwie (imię nazwisko)")
        void shouldFindByFullName() {
            stubMembers();

            List<MemberSummary> result = service.search("jan kow");

            assertEquals(List.of("Jan Kowalski"), result.stream().map(MemberSummary::fullName).toList());
        }

        /**
         * Wyszukiwanie po fragmencie adresu e-mail powinno zwrócić wszystkich pasujących, posortowanych po nazwisku.
         */
        @Test
        @DisplayName("znajduje po adresie e-mail i sortuje wyniki po nazwisku")
        void shouldFindByEmail() {
            stubMembers();

            List<MemberSummary> result = service.search("example.com");

            assertEquals(List.of("Jan Kowalski", "Łukasz Wiśniewski"),
                    result.stream().map(MemberSummary::fullName).toList());
        }

        /**
         * Brak dopasowań powinien dać pustą listę.
         */
        @Test
        @DisplayName("zwraca pustą listę, gdy nic nie pasuje")
        void shouldReturnEmptyListWhenNothingMatches() {
            stubMembers();

            assertTrue(service.search("nikt taki").isEmpty());
        }

        /**
         * Pusta fraza powinna być odrzucona jeszcze przed odczytem czytelników.
         *
         * @param query niepoprawna fraza
         */
        @ParameterizedTest(name = "odrzuca frazę: [{0}]")
        @ValueSource(strings = {"", " ", "\t"})
        void shouldRejectBlankQuery(String query) {
            assertThrows(IllegalArgumentException.class, () -> service.search(query));

            verifyNoInteractions(memberRepository);
        }
    }

    /**
     * Tworzy poprawnego czytelnika z ustaloną datą rejestracji.
     *
     * @param firstName imię
     * @param lastName  nazwisko
     * @param email     adres e-mail
     * @return nowy czytelnik z losowym identyfikatorem
     */
    private static Member member(String firstName, String lastName, String email) {
        return new Member(firstName, lastName, email, LocalDate.of(2025, 1, 15));
    }

    /**
     * Tworzy aktywne wypożyczenie dla czytelnika.
     *
     * @param member   czytelnik wypożyczający
     * @param loanDate data wypożyczenia (termin zwrotu to 30 dni później)
     * @return aktywne wypożyczenie
     */
    private static Loan activeLoan(Member member, LocalDate loanDate) {
        return new Loan(UUID.randomUUID(), member.getId(), loanDate);
    }

    /**
     * Tworzy zwrócone wypożyczenie dla czytelnika.
     *
     * @param member czytelnik, który wypożyczył i zwrócił książkę
     * @return zwrócone wypożyczenie
     */
    private static Loan returnedLoan(Member member) {
        Loan loan = new Loan(UUID.randomUUID(), member.getId(), LocalDate.of(2026, 2, 1));
        loan.markAsReturned(LocalDate.of(2026, 2, 10));
        return loan;
    }
}
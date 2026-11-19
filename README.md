# Libris Core

Libris, etap 2/3: aplikacja konsolowa do zarządzania biblioteką, napisana obiektowo i zapisująca dane do plików JSON.

## Ścieżka nauki

| Etap | Projekt | Zakres | Status |
|------|---------|--------|--------|
| 1/3 | [libris-cli](https://github.com/Tajko725/libris-cli) | Podstawy Javy, konsola, dane w pamięci | gotowy (v1.0.0) |
| **2/3** | **libris-core** (ten projekt) | Programowanie obiektowe, warstwy, zapis do JSON | w trakcie |
| 3/3 | libris-api | Spring Boot, REST API, baza PostgreSQL | planowany |

## Technologie

| Technologia | Wersja |
|-------------|--------|
| Java (JDK) | 25 |
| Maven | 3 (wbudowany w IntelliJ IDEA) |
| Jackson (`tools.jackson.core:jackson-databind`) | 3.2.4 |
| JUnit Jupiter | 6.1.3 |
| maven-compiler-plugin | 3.14.0 |
| maven-surefire-plugin | 3.5.5 |
| maven-shade-plugin | 3.6.2 |
| Mockito | 5.24.0 |
| maven-dependency-plugin | 3.8.1 |

## Postęp

- [x] Konfiguracja Maven (Java 25, Jackson 3.2.4, JUnit 6.1.3)
- [x] Model `Book` (UUID, ISBN, gatunek) z walidacją i testami jednostkowymi
- [x] Modele `Member` (czytelnik) i `Loan` (wypożyczenie) z walidacją i testami jednostkowymi
- [x] Repozytorium zapisujące dane do plików JSON z bezpiecznym zapisem i testami
- [x] `BookService`: egzemplarze, katalog z liczbą sztuk, wyszukiwanie, testy z Mockito
- [x] `MemberService`: czytelnicy, unikalny e-mail, lista z liczbą wypożyczeń, wyszukiwanie, testy z Mockito
- [x] `LoanService`: wypożyczanie po ISBN, zwroty, limity, listy wypożyczeń, testy z Mockito
- [ ] Menu konsolowe
- [ ] Wydanie 1.0.0 z gotowym plikiem JAR

## Model danych

Każdy obiekt `Book` to jeden fizyczny egzemplarz książki z własnym identyfikatorem UUID.
Kilka egzemplarzy tego samego tytułu rozpoznaję po wspólnym numerze ISBN.
Informacji o dostępności nie trzymam w książce: wynika z istnienia aktywnego wypożyczenia (`Loan`).

Wypożyczenie (`Loan`) przechowuje tylko identyfikatory książki i czytelnika oraz daty: wypożyczenia, terminu zwrotu (30 dni) i zwrotu.
Wypożyczenie jest aktywne, dopóki nie ma daty zwrotu.

## Przechowywanie danych

Dane zapisuję w trzech plikach JSON w katalogu `data/`, który powstaje w miejscu uruchomienia aplikacji:
`books.json`, `members.json` i `loans.json`.
Po każdej zmianie zapisuję dane najpierw do pliku tymczasowego, a dopiero potem podmieniam właściwy plik,
dzięki czemu przerwanie programu w trakcie zapisu nie uszkadza danych.
Gdy plik jest uszkodzony, aplikacja zgłasza błąd zamiast zaczynać od pustych danych.
Katalog `data/` dodaję do `.gitignore`, więc dane nie trafiają do repozytorium.

## Obsługa błędów

Błędy logiki biblioteki zgłaszam własnymi wyjątkami z bazą `LibraryException`:
`EntityNotFoundException` (brak książki lub czytelnika) i `BusinessRuleException` (naruszenie reguły, np. usunięcie wypożyczonej książki).
Niepoprawne dane wejściowe zgłaszam jako `IllegalArgumentException`.

## Czas w serwisach

Serwisy dostają zegar (`java.time.Clock`) przez konstruktor, więc w testach używam stałej daty,
a w aplikacji zegara systemowego. Dzięki temu wyniki testów nie zależą od dnia ich uruchomienia.

## Zasady wypożyczeń

- Wypożyczam po numerze ISBN, a program sam wybiera wolny egzemplarz.
- Termin zwrotu to 30 dni od dnia wypożyczenia.
- Czytelnik może mieć najwyżej 5 aktywnych wypożyczeń i tylko jeden egzemplarz danej książki naraz.
- Czytelnik z książką po terminie zwrotu nie może wypożyczać kolejnych, dopóki jej nie odda.
- Zwrot rejestruję po identyfikatorze wypożyczenia. Prolongaty na razie nie ma.

## Planowana architektura

- `model` – klasy domenowe (`Book`, `Member`, `Loan`),
- `repository` – interfejs repozytorium i implementacje zapisujące do JSON,
- `service` – logika biznesowa (`BookService`, `MemberService`, `LoanService`),
- `ui` – menu konsolowe,
- `Main` – punkt startowy aplikacji.

## Uruchomienie

Projekt jest na etapie konfiguracji, więc aplikacja nie jest jeszcze gotowa do uruchomienia.
Budowanie projektu: `mvn clean package` albo w IntelliJ: okno **Maven** → **Lifecycle** → **package**.

## Organizacja pracy

Pracuję według GitFlow: `main` zawiera tylko wydania, `develop` to gałąź robocza,
a każda funkcja powstaje w osobnej gałęzi `feature/*` i trafia do `develop` przez Pull Request.
Commity piszę według Conventional Commits (`feat:`, `docs:`, `chore:`).
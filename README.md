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

## Postęp

- [x] Konfiguracja Maven (Java 25, Jackson 3.2.4, JUnit 6.1.3)
- [x] Model `Book` (UUID, ISBN, gatunek) z walidacją i testami jednostkowymi
- [x] Modele `Member` (czytelnik) i `Loan` (wypożyczenie) z walidacją i testami jednostkowymi
- [ ] Repozytorium zapisujące dane do JSON
- [ ] Serwisy: `BookService`, `MemberService`, `LoanService`
- [ ] Menu konsolowe
- [ ] Wydanie 1.0.0 z gotowym plikiem JAR

## Model danych

Każdy obiekt `Book` to jeden fizyczny egzemplarz książki z własnym identyfikatorem UUID.
Kilka egzemplarzy tego samego tytułu rozpoznaję po wspólnym numerze ISBN.
Informacji o dostępności nie trzymam w książce: wynika z istnienia aktywnego wypożyczenia (`Loan`).

Wypożyczenie (`Loan`) przechowuje tylko identyfikatory książki i czytelnika oraz daty: wypożyczenia, terminu zwrotu (30 dni) i zwrotu.
Wypożyczenie jest aktywne, dopóki nie ma daty zwrotu.

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
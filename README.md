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
- [x] Interfejs konsolowy, fundament: `ConsoleIO`, `LibraryContext`, folder danych jako argument
- [x] Interfejs konsolowy: menu główne, katalog książek (z podglądem egzemplarzy) i czytelnicy
- [x] Interfejs konsolowy: wypożyczenia i zwroty, dane przykładowe
- [ ] Menu konsolowe
- [ ] Wydanie 1.0.0 z gotowym plikiem JAR
- [x] Wydanie 1.0.0: plik JAR, tag `v1.0.0` i GitHub Release

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

## Pobranie i uruchomienie

Gotowy plik JAR jest w [najnowszym wydaniu](https://github.com/Tajko725/libris-core/releases/latest).
Do uruchomienia wystarczy zainstalowana Java 25:

    java -jar libris-core-1.0.0.jar

Przy pierwszym uruchomieniu wybieram w menu „Załaduj dane przykładowe”, żeby od razu zobaczyć, jak działa program.

## Uruchomienie

Projekt jest na etapie konfiguracji, więc aplikacja nie jest jeszcze gotowa do uruchomienia.
Budowanie projektu: `mvn clean package` albo w IntelliJ: okno **Maven** → **Lifecycle** → **package**.

### Budowanie ze źródeł

W IntelliJ: **Maven** → **Lifecycle** → **clean**, potem **package**. Gotowy plik jest w
`target/libris-core-1.0.0.jar`. Z linii poleceń (gdy Maven jest w PATH): `mvn clean package`.

### Menu programu

- **Katalog książek**: katalog zgrupowany po ISBN (wolne/razem), podgląd egzemplarzy z krótkim identyfikatorem,
  wyszukiwanie, dodawanie książek i egzemplarzy, edycja danych, usuwanie egzemplarza.
- **Czytelnicy**: lista z liczbą wypożyczeń, wyszukiwanie, dodawanie, edycja i usuwanie.
- **Wypożyczenia**: wypożyczanie (czytelnik, potem tytuł), zwroty, listy aktywnych, po terminie i historia.
- **Załaduj dane przykładowe**: 12 tytułów, 5 czytelników i 6 wypożyczeń (w tym jedno po terminie).
  Działa tylko, gdy biblioteka jest pusta.

### Folder z danymi

Program zapisuje dane w plikach JSON (`books.json`, `members.json`, `loans.json`).
Domyślnie używa folderu `data` obok miejsca uruchomienia. Inny folder podaję jako pierwszy argument:

    java -jar libris-core-1.0.0.jar C:\moje-dane

Brakujący folder tworzę automatycznie.

## Organizacja pracy

Pracuję według GitFlow: `main` zawiera tylko wydania, `develop` to gałąź robocza,
a każda funkcja powstaje w osobnej gałęzi `feature/*` i trafia do `develop` przez Pull Request.
Commity piszę według Conventional Commits (`feat:`, `docs:`, `chore:`).

## Licencja

Projekt jest dostępny na licencji MIT (plik `LICENSE`).
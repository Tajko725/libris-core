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
| Jackson (`tools.jackson.core:jackson-databind`) | 3.1.3 |
| JUnit Jupiter | 6.1.3 |
| maven-compiler-plugin | 3.14.0 |
| maven-surefire-plugin | 3.5.5 |
| maven-shade-plugin | 3.6.2 |

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
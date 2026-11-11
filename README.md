# Cross-Platform Programming Tools — Java labs

[![Java CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml/badge.svg?branch=main-java)](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml)

Laboratory works for the course "Cross-Platform Programming Tools",
Lviv Polytechnic National University. Author: Fedorchuk I. V., group KI-31.

**Variant 20 — gym memberships.** Every lab improves the same console program:
it reads memberships from a CSV file, skips invalid rows with a reason,
calculates the report indicators with the Stream API and writes the report
to the console and to a file.

## Labs

| Lab | Topic | Code | Pull request |
|---|---|---|---|
| 1 | Java project setup and basic data processing | [v1.0.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.0.0) | [#5](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/5) |
| 2 | Classes, records and encapsulation | [v1.1.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.1.0) | [#6](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/6) |
| 3 | Inheritance, interfaces and polymorphism | [v1.2.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.2.0) | [#12](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/12) |
| — | Cleanup of labs 1–3 | [v1.2.1](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.2.1) | [#21](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/21) |
| 4 | Stream API | v1.3.0 | this branch |
| 5 | Generic repository, annotations, reflection, CSV export | planned | — |

Lab reports are in [`REPORT.md`](REPORT.md) (latest lab) and [`docs/reports`](docs/reports) (earlier labs).

## Input format

UTF-8 text, one membership per line, fields separated by `;`:

```text
client;plan;months;visits;price;startDate
```

| Field | Type | Rule |
|---|---|---|
| `client` | text | not blank |
| `plan` | text | not blank, tariff name |
| `months` | whole number | from 1; at most 12 for an annual membership |
| `visits` | whole number | from 1 |
| `price` | decimal number | not negative |
| `startDate` | date `yyyy-mm-dd` | first day of the membership; it ends `months` later |

A row with the plan `Monthly` or a one-month term becomes a monthly membership,
any other row becomes an annual one. An invalid row is skipped with its line number and reason;
the other rows are still processed.

Example ([`data/input.csv`](data/input.csv) also contains one row for every kind of invalid data):

```text
Іван Федорчук;Безліміт;12;45;1500.50;2026-01-15
Петро Коваль;Ранковий;3;10;500.00;2026-08-01
Олег Савчук;Monthly;1;12;650.00;2026-10-20
```

## Build

Requirements: JDK 21. Maven is not needed — the Maven Wrapper downloads it.

```powershell
# Windows
.\mvnw.cmd clean verify
```

```bash
# Linux / macOS
./mvnw clean verify
```

`verify` compiles the code, runs the JUnit 5 tests, builds the executable JAR
`target/gym-membership-1.3.0.jar` and checks the code with SpotBugs.
GitHub Actions runs the same command on Ubuntu, Windows and macOS and publishes the JAR as an artifact.

## Run

```bash
java -jar target/gym-membership-1.3.0.jar [options]
```

| Option | Meaning |
|---|---|
| `--input <path>` | CSV file to read (default `data/input.csv`) |
| `--output <path>` | report file to write (default `out/report.txt`) |
| `--version` | print the version and exit |
| `--help` | print the usage and exit |

Exit codes: `0` success, `1` a file cannot be read or written, `2` invalid options.

Example output for the sample data:

```text
Skipped line 3: months is not a whole number: 'помилка'
Skipped line 5: plan must not be blank
Skipped line 6: expected 6 fields, got 1
Skipped line 7: visits must be at least 1, got -5
Skipped line 8: expected 6 fields, got 5
Skipped line 11: startDate is not a date (yyyy-mm-dd): '2026-13-01'
--- REPORT ---
Valid records: 7
Total revenue: 5450.50 UAH
Longest membership: 12 months
Average visits: 17.57
Unique clients: 6
Highest visit cost: 75.00 UAH
Total visit cost: 373.63 UAH

--- STREAM QUERIES (report date 2026-11-01) ---
Active memberships: 5 (Іван Федорчук, Марія Іванівна, Олег Савчук, Анна Литвин, Ірина Бондар)
Clients: Іван Федорчук, Петро Коваль, Марія Іванівна, Олег Савчук, Анна Литвин, Ірина Бондар, Марія Іванівна
Visits by kind: Monthly=30, Annual=93
Visit statistics: count=7, sum=123, min=6, max=45, average=17.57
Top 5 by visits: Іван Федорчук (45), Марія Іванівна (20), Марія Іванівна (18), Анна Литвин (12), Олег Савчук (12)
Search "Анна Литвин": found, 12 visits, 2026-10-05 to 2026-11-05
Report saved to out/report.txt
```

The program writes UTF-8. If Ukrainian letters look broken in a Windows console,
switch the console to UTF-8 once with `chcp 65001`.

## Report

| Indicator | How it is calculated |
|---|---|
| Valid records | number of rows that passed validation |
| Total revenue | `mapToDouble(getPrice).sum()` |
| Longest membership | `mapToInt(getMonths).max()` |
| Average visits | `mapToInt(getVisits).average()` |
| Unique clients | `distinct().count()` — same subtype and client count once |
| Highest / total visit cost | `mapToDouble(visitCost)` — each subtype applies its own formula |

Stream queries of variant 20 (class `MembershipReport`). A membership is active when
`startDate <= 2026-11-01 < startDate + months`; the date is fixed so the report is reproducible.

| Query | Stream operation | Result |
|---|---|---|
| Active memberships | `filter` | memberships active on the report date |
| Clients | `map` | client of every membership, in file order |
| Visits by kind | `groupingBy` + `summingInt` into `EnumMap` | total visits of monthly and annual memberships |
| Visit statistics | `summarizingInt` | count, sum, min, max, average |
| Top 5 by visits | `sorted` + `limit` | most visits first; equal visits by client name |
| Search by client | `filter` + `findFirst` | `Optional` — first membership of the client or nothing |

## Project structure

```text
src/main/java/ua/lpnu/kzp/
├── Main.java                 entry point: reads, reports, writes
├── CommandLine.java          command-line options
├── Membership.java           base type: shared fields, validation, fromCsv
├── MonthlyMembership.java    subtype with its own visit cost rule
├── AnnualMembership.java     subtype with its own visit cost rule
├── MembershipKind.java       membership category
├── MembershipSummary.java    record with the Lab 1 indicators
└── MembershipReport.java     Stream API queries of Lab 4
src/test/java/ua/lpnu/kzp/    JUnit 5 tests
data/input.csv                sample data
ai/                           AI assistant roles
docs/                         lab reports and images
```

## AI usage

AI assistants were used as consultants. The roles are described in [`ai/`](ai);
what was accepted, rejected and written by the author is recorded in each lab report.

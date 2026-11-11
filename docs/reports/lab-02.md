# Lab 2 — Classes, records and encapsulation

## 1. Topic, number and variant

| | |
|---|---|
| Lab | 2 — Class model of the subject area |
| Variant | 20 — gym memberships (unchanged) |
| Author | Fedorchuk I. V., group KI-31 |
| Development OS | Windows 11 |
| CI | GitHub Actions: Ubuntu, Windows, macOS |
| Code state | tag [`v1.1.0`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.1.0), pull request [#6](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/6) |

## 2. Goal

Replace the string row and the parallel variables of Lab 1 with a class whose objects cannot hold
invalid data, and keep the report of Lab 1 unchanged.

Result: an immutable entity class `Membership` with validation in the constructor, a static factory
`fromCsv`, a `toString` formatted with `Locale.ROOT`, and the record `MembershipSummary` with the
report indicators.

## 3. State before and after

| Lab 1 | Lab 2 |
|---|---|
| `String[] fields` and local variables `months`, `visits`, `price` | object `Membership` with `private final` fields |
| checks spread over the loop in `main` | invariants checked once, in the constructor |
| parsing inside `main` | `Membership.fromCsv(line)` |
| four counters passed around separately | record `MembershipSummary(validCount, averageVisits, totalRevenue, maxMonths)` |
| errors printed immediately | errors collected with the line number and printed after the report |

## 4. Entity class

| Element | Implementation |
|---|---|
| Fields | `private final String client, plan; int months, visits; double price` |
| Invariants | client and plan not blank; months, visits and price not negative |
| Constructor | checks every invariant and throws `IllegalArgumentException` with the reason |
| Accessors | `getClient()`, `getPlan()`, `getMonths()`, `getVisits()`, `getPrice()`; no setters |
| `toString` | `String.format(Locale.ROOT, …)`, so the price always has a dot |

Without setters an object that passed the constructor stays valid for its whole life.

## 5. Factory method and record

`fromCsv(String line)` splits the row with `split(";", -1)`, checks that there are exactly five fields,
parses the numbers and calls the constructor. `NumberFormatException` is turned into
`IllegalArgumentException`, so `Main` catches one exception type per row.

`MembershipSummary` is a record: Java generates the constructor, accessors, `equals`, `hashCode`
and `toString`. Its compact constructor rejects negative and non-finite values.

## 6. Testing

At `v1.1.0` (`MembershipTest`):

- valid object keeps its values;
- negative price is rejected;
- a `fromCsv` test was started but had no assertion yet.

The missing checks were completed in the cleanup `v1.2.1` (Issues #16 and #19): `MembershipCsvTest`
covers a valid row, wrong field count, non-numeric values, broken invariants and `toString` under a
locale that uses a comma; `MembershipSummaryTest` covers accessors, equality and invalid components.
The Lab 1 smoke test still passes.

## 7. Infrastructure

The Maven Wrapper, JUnit 5, SpotBugs, the Shade JAR and the CI matrix were carried over from Lab 1
without changes. `mvnw verify` passes locally and on Ubuntu, Windows and macOS:
[Actions → Java CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml).

## 8. GitHub Issues and pull request

| Change | Check |
|---|---|
| `Membership`, `fromCsv`, `MembershipSummary`, new `Main` loop — PR [#6](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/6) | green CI, same report as Lab 1 |

Lab 2 was done in one pull request without separate Issues. This gap was not repeated:
Lab 3 and later work are split into Issues.

## 9. Behaviour comparison

The same input file gives the same report in Lab 1 and Lab 2:

| Indicator | Lab 1 | Lab 2 |
|---|---|---|
| Valid records | 3 | 3 |
| Total revenue | 2900.50 | 2900.50 |
| Longest membership | 12 | 12 |
| Average visits | 25.00 | 25.00 |

The row `Олена Петрівна;Разовий;помилка;1;150.00` is still skipped with its line number.

## 10. Academic integrity

- **Tool:** Google Gemini, used as a "Senior DevOps mentor" consultant.
- **AI contribution:** explanations of encapsulation and object state, skeletons of the class and
  the tests, code review.
- **Author's contribution:** validation logic, collection handling (`ArrayList`), fixes of the
  SpotBugs findings, the tests, commits and merging.
- **Later corrections (v1.2.1, Claude by Anthropic):** Lab 3 had removed `fromCsv`, the record and the
  shared fields; they were restored in PR #21 (Issues #15, #16), and the empty `fromCsv` test was
  replaced by real tests (Issue #19).
- I understand every line of the code and can explain it.

## 11. Answers to the control questions

1. A **class** keeps the values of one membership together and guarantees they are valid;
   parallel variables and string arrays can get out of sync and accept anything.
2. **`private`** hides the field from other classes; **`final`** allows assigning it only once, in the constructor.
3. **Encapsulation** means the state is changed only through the class's own code, so no other code
   can put the object into an invalid state.
4. **Invariants** are checked in the constructor because it is the only way to create an object;
   an object that exists is always valid.
5. **`Objects.requireNonNull`** rejects only `null`; **`isBlank`** also rejects `""` and whitespace.
6. **Without setters** no code can change a valid object into an invalid one later.
7. A **record** gets a canonical constructor, accessors named after the components, `equals`,
   `hashCode` and `toString`.
8. A **record** is a transparent carrier of data with all fields final; an entity class can hide
   its fields, add behaviour and choose its own equality.
9. A record stores a **reference** to a mutable list; the list itself can still be changed from outside
   unless it is copied with `List.copyOf`.
10. **`fromCsv`** keeps the knowledge of the CSV format next to the class and gives the factory a clear name.
11. The parsing error of **one row** is caught in the reading loop, so the remaining rows are still processed.
12. A **wrong record** must not reach the list: every later calculation would include invalid data.
13. **`toString`** is a readable description for people and logs; the CSV format is a contract for programs
    and must not depend on how an object prints itself.
14. **`Locale.ROOT`** gives the same number format (a dot) on every system, so the report and tests are reproducible.
15. **AAA** — Arrange (prepare data), Act (call the code), Assert (check the result).
16. Blank client or plan, negative months, visits and price.
17. Run the same input with the old and the new version and compare the **reports**; keep the old tests passing.
18. The **previous tests** prove that the earlier behaviour still works after the change.
19. `pom.xml`, the Maven Wrapper, `.gitignore`, the CI workflow, Issue forms and the PR template.
20. The constructor of `Membership`: it is the only way to create an object, and it refuses invalid values,
    so the rest of the program never checks them again.

## 12. Conclusions

The data of one membership became an object that is valid by construction, and the report indicators
became a record. This class model was the starting point of Lab 3, where the entity became a base
type with subtypes.

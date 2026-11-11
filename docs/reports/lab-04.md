# Lab 4 — Stream API

## 1. Topic, number and variant

| | |
|---|---|
| Lab | 4 — Data processing with the Stream API |
| Variant | 20 — gym memberships (unchanged) |
| Author | Fedorchuk I. V., group KI-31 |
| Development OS | Windows 11 |
| CI | GitHub Actions: Ubuntu, Windows, macOS |
| Code state | tag [`v1.3.0`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.3.0), branch `lab-04-java`, pull request [#28](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/28) |

## 2. Goal

Replace the loops that calculate the report with Stream API pipelines without changing the report,
and add the five queries and the `Optional` search of variant 20.

Result: every report indicator is calculated by a stream; the class `MembershipReport` contains six
pure queries; a test proves that the loops of Lab 3 and the streams give equal results.

## 3. State before and after

| Indicator | Lab 3 (loop or collection) | Lab 4 (stream) |
|---|---|---|
| Total revenue | `totalRevenue += m.getPrice()` | `mapToDouble(Membership::getPrice).sum()` |
| Longest membership | `maxMonths = Math.max(maxMonths, m.getMonths())` | `mapToInt(Membership::getMonths).max().orElse(0)` |
| Average visits | `totalVisits += …`, then `count == 0 ? 0.0 : (double) totalVisits / count` | `mapToInt(Membership::getVisits).average().orElse(0.0)` |
| Total visit cost | `totalVisitCost += m.visitCost()` | `mapToDouble(Membership::visitCost).sum()` |
| Highest visit cost | `maxVisitCost = Math.max(maxVisitCost, cost)` | `mapToDouble(Membership::visitCost).max().orElse(0.0)` |
| Unique clients | `new HashSet<>(memberships).size()` | `distinct().count()` |

Reading and validating the CSV stays a loop: every invalid row must be reported with its line number,
and the order of the messages matters.

## 4. Data contract

The model of labs 2–3 is kept: abstract `Membership`, subtypes `MonthlyMembership` and
`AnnualMembership`, enum `MembershipKind`. Variant 20 defines a membership by client, kind, start,
end and visits, and the filter selects active memberships, so a start date was added (Issue #22):

```text
client;plan;months;visits;price;startDate
Олег Савчук;Monthly;1;12;650.00;2026-10-20
```

| Rule | Implementation |
|---|---|
| `startDate` is an ISO date | `LocalDate.parse`; an invalid date is reported as `startDate is not a date (yyyy-mm-dd): '2026-13-01'` |
| End date | `getEndDate() = startDate.plusMonths(months)`, exclusive |
| Active on a day | `isActiveOn(date)`: `startDate <= date < endDate` |
| Report date | `MembershipReport.REPORT_DATE = 2026-11-01`, fixed instead of `LocalDate.now()` so results are reproducible |

The control data set (`data/input.csv`) has 7 valid and 6 invalid rows. The valid rows cover both
kinds, a duplicate (two annual memberships of Марія Іванівна), equal visits (Анна and Олег, 12 each),
a membership ending on the report date (Петро) and one starting on it (Ірина).

## 5. Queries

All queries are in `MembershipReport`. Each opens a new stream over the list, does not change the list
and does not print anything.

| Query | Source | Intermediate operations | Terminal operation | Result type |
|---|---|---|---|---|
| `activeMemberships(list, date)` | `list.stream()` | `filter(m -> m.isActiveOn(date))` | `toList()` | `List<Membership>` |
| `clientNames(list)` | `list.stream()` | `map(Membership::getClient)` | `toList()` | `List<String>` |
| `visitsByKind(list)` | `list.stream()` | — | `collect(groupingBy(getKind, EnumMap, summingInt(getVisits)))` | `Map<MembershipKind, Integer>` |
| `visitStatistics(list)` | `list.stream()` | — | `collect(summarizingInt(getVisits))` | `IntSummaryStatistics` |
| `topByVisits(list, n)` | `list.stream()` | `sorted(BY_VISITS_DESC_THEN_CLIENT)`, `limit(n)` | `toList()` | `List<Membership>` |
| `findByClient(list, name)` | `list.stream()` | `filter(m -> m.getClient().equals(name))` | `findFirst()` | `Optional<Membership>` |

`activeMemberships` needs a lambda, not a method reference: `isActiveOn` takes the date, and the lambda
captures it from the method parameter.

## 6. Grouping and statistics

- **Key:** `MembershipKind`. The map is an `EnumMap`, so the keys always follow the enum order
  (`MONTHLY`, then `ANNUAL`). A kind without memberships has no entry.
- **Aggregate:** the downstream collector `summingInt(Membership::getVisits)` sums the visits in each group.
  Control data: `Monthly=30, Annual=93`.
- **Statistics:** count, sum, minimum, maximum and average in one pass. Control data:
  `count=7, sum=123, min=6, max=45, average=17.57`.
- **Empty input:** count, sum and average are `0`, while min and max are `Integer.MAX_VALUE` and
  `Integer.MIN_VALUE`, so the report prints `no data` instead of them.
- **Number format:** `String.format(Locale.ROOT, …)` with two decimals.

## 7. Comparator

```java
Comparator.comparingInt(Membership::getVisits)   // main key: visits
        .reversed()                              // only this key is descending
        .thenComparing(Membership::getClient);   // equal visits: client name ascending
```

`reversed()` stands before `thenComparing`, so it turns around only the visits. `sorted` runs before
`limit`: first all memberships are ordered, then the best five are kept. Control data:
`Іван Федорчук (45), Марія Іванівна (20), Марія Іванівна (18), Анна Литвин (12), Олег Савчук (12)` —
Анна comes before Олег by the second key; Петро (10) and Ірина (6) are cut off.
`n = 0` gives an empty list, a too large `n` gives all memberships, a negative `n` is rejected.

## 8. Optional

The search key is the exact client name. `findByClient` returns the first membership of the client in
file order (for the duplicate Марія Іванівна this is the one with 20 visits) or `Optional.empty()`.
The report turns the result into text without `null` checks:

```java
findByClient(memberships, "Анна Литвин")
        .map(m -> String.format(Locale.ROOT, "found, %d visits, %s to %s", …))
        .orElse("not found");
```

## 9. Equivalence

`LoopStreamEquivalenceTest` keeps the loops of `v1.2.1` as the reference and compares them with the
streams on the control data and on an empty list:

| Calculation | Loop | Stream | Result on the control data |
|---|---|---|---|
| Valid records | `memberships.size()` | `memberships.size()` | 7 = 7 |
| Total revenue | `+=` in a loop | `mapToDouble(...).sum()` | 5450.50 = 5450.50 |
| Longest membership | `Math.max` in a loop | `mapToInt(...).max()` | 12 = 12 |
| Average visits | sum / count | `average()` | 17.57 = 17.57 |
| Unique clients | `HashSet.size()` | `distinct().count()` | 6 = 6 |
| Highest visit cost | `Math.max` in a loop | `mapToDouble(...).max()` | 75.00 = 75.00 |
| Total visit cost | `+=` in a loop | `mapToDouble(...).sum()` | 373.63 = 373.63 |

The control output was recorded before the loops were replaced and did not change after each step.
Decimal results are compared with a tolerance of `1e-9`, because `DoubleStream.sum` may add numbers
in a different order than a loop.

## 10. Testing

`mvnw verify`: **74 tests, 0 failures**, SpotBugs: 0 bugs. New and changed tests of this lab:

| Test class | What it proves |
|---|---|
| `MembershipReportTest` (14) | every query on the control data; filter boundary (start day included, end day excluded); groups in enum order; second sort key; N = 0 / 100 / −1; first match for a duplicate; empty `Optional`; every query on an empty list; input list not modified; `null` arguments |
| `LoopStreamEquivalenceTest` (5) | loops of Lab 3 and streams give equal indicators; the report keeps the control values |
| `MembershipTest` (+3) | end date, `isActiveOn` on every boundary, `null` start date |
| `MembershipCsvTest` (+4) | date parsing, invalid month, non-ISO date, empty date |
| `MainTest` (+1) | the queries section in the report; an empty file gives `0`, `none` and `no data` |
| `SampleDataTest` | the control file: 7 valid rows, 6 skipped rows with their reasons, the indicators |

All tests of labs 1–3 still pass.

## 11. Infrastructure

```powershell
.\mvnw.cmd clean test       # compile and run the tests
.\mvnw.cmd clean verify     # + executable JAR + SpotBugs
java -jar target\gym-membership-1.3.0.jar
java -jar target\gym-membership-1.3.0.jar --input data\input.csv --output out\report.txt
```

Report of the control data:

```text
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
```

CI: [Actions → Java CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml)
runs `verify` on Ubuntu, Windows and macOS and publishes `gym-membership-1.3.0.jar` as an artifact of each run.

## 12. GitHub Issues and pull request

| Issue | Change | Check |
|---|---|---|
| [#22](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/22) | start date, `getEndDate`, `isActiveOn`, 6-field CSV, new control data | `MembershipTest`, `MembershipCsvTest`, `SampleDataTest` |
| [#23](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/23) | `activeMemberships` (filter), `clientNames` (map) | `MembershipReportTest` |
| [#24](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/24) | `visitsByKind` (groupingBy), `visitStatistics` (summarizingInt) | `MembershipReportTest` |
| [#25](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/25) | `topByVisits` (sorted + limit), `findByClient` (Optional) | `MembershipReportTest` |
| [#26](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/26) | loops → streams, `distinct`, queries section of the report | `LoopStreamEquivalenceTest`, unchanged control output |
| [#27](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/27) | Javadoc, README, reports of labs 1–4, version 1.3.0 | green CI on three operating systems |

## 13. Comparison of implementations

**Became clearer:**
- each indicator is one short pipeline that reads as a sentence: "prices of all memberships, summed";
- there are no accumulator variables that can be initialised or updated wrongly;
- the empty case is explicit: `max()` and `average()` return `OptionalInt` / `OptionalDouble`,
  so `orElse` must be written, while in a loop the check was easy to forget;
- the queries are pure functions of their input and are easy to test separately.

**Not to be judged by line count:**
- the stream version passes over the list several times (once per indicator), while the loop needed one
  pass; for this data size the difference is negligible, and readability matters more;
- a single loop can be faster for huge data and needs no knowledge of collectors;
- reading the CSV stayed a loop on purpose: error reporting per line is clearer there.

## 14. Academic integrity

- **Tool:** Claude (Anthropic), in the roles Manager (Issues #22–#27), Developer (explanations,
  skeletons), Validator (control data with boundary cases), Documenter (README, reports) and
  Reviewer (review of every change before commit).
- **Prompts:** theory of streams on the code of this project; plan of the lab by the requirements;
  review of my code; finishing the remaining parts.
- **Accepted:** the start date column with an exclusive end date; a fixed report date; the control data
  set; separate pipelines instead of one loop; a test with the old loops as the reference.
- **Rejected:** the draft definition "active = at least 5 visits" (does not match the variant);
  `Membership::isActiveOn` as a method reference (impossible — the date is needed); IDE auto-imports
  `java.lang.reflect.Member` and `java.nio.channels.MembershipKey`.
- **Fixed mistakes:** an unused import left in `Main`, `mapToDouble` used for integer visits,
  formatting and Javadoc typos found in review.
- **Own contribution:** `MembershipSummary.from` and the visit cost and unique client calculations in
  `formatReport` rewritten as streams; the `formatQueries` skeleton; branches, Issues, commits and the
  pull request. Claude wrote the date support, `MembershipReport`, the tests, the end of `formatQueries`
  and the documentation; I reviewed every file before committing.
- I walked through the program from `main` to the report and can explain every pipeline and every line.

## 15. Answers to the control questions

1. A **collection** stores data; a **stream** is a one-time pipeline over data that stores nothing and
   does not change its source.
2. A **pipeline** has a source (`list.stream()`), zero or more intermediate operations (`filter`, `map`,
   `sorted`, `limit`) and one terminal operation (`toList`, `sum`, `collect`).
3. **Intermediate operations are lazy:** they only describe the work; nothing runs until the terminal
   operation asks for the result.
4. **`filter`** keeps the elements for which a `Predicate<T>` returns `true`.
5. **`map`** turns each element into any object (`Stream<R>`); **`mapToDouble`** turns it into a `double`
   and gives a `DoubleStream` with `sum`, `average`, `max`.
6. **`distinct`** removes duplicates; the result depends on `equals` and `hashCode` of the elements.
7. **`sorted().limit(N)`** orders all elements and keeps the best N; **`limit(N).sorted()`** keeps the
   first N in source order and sorts only them.
8. The **terminal operation** starts the pipeline and produces the result; after it the stream is used up.
9. **`Collectors.joining`** joins strings with a separator, prefix and suffix — for example the client
   names in the report.
10. **`groupingBy`** takes a key for each element and puts the element into the group of that key in a map.
11. A **downstream collector** in the third argument, for example `summingInt(Membership::getVisits)`,
    turns each group into a sum.
12. **`DoubleSummaryStatistics`** contains count, sum, min, max and average.
13. For an **empty stream** count, sum and average are 0; for `DoubleSummaryStatistics` min is `Infinity`
    and max is `-Infinity` (`Integer.MAX_VALUE` and `MIN_VALUE` for `IntSummaryStatistics`).
14. A **compound comparator** starts with `Comparator.comparing…(main key)` and adds more keys with `thenComparing`.
15. **`reversed()`** turns around everything built before it; placed right after the main key it reverses
    only that key, and the second key stays ascending.
16. **`Optional`** makes "nothing found" part of the return type, so the caller must handle it and cannot
    forget a `null` check.
17. **`orElse(x)`** returns the value or `x`; **`orElseThrow()`** returns the value or throws;
    **`ifPresent(action)`** runs the action only when there is a value.
18. **Adding to an outside list in `forEach`** hides the result in a side effect, breaks with parallel
    streams and makes the code harder to test; `toList()` or `collect` express the result directly.
19. During a **loop-to-stream refactoring** check the same results on the same data, the empty input,
    duplicates, boundaries, order of the result, and that the input is not modified.
20. A **parallel stream** pays off only for large amounts of independent, CPU-heavy work without shared
    state; for small lists like ours it is slower because of the coordination cost.

## 16. Conclusions

Every report indicator is now calculated by a stream, the six queries of variant 20 work on the control
data and on empty input, and the report of Lab 3 is unchanged. The model with dates and the pure query
class are the base for Lab 5, where the memberships are stored in a generic repository and exported to CSV.

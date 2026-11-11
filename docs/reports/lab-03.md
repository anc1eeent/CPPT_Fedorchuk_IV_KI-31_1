# Lab 3 — Inheritance, interfaces and polymorphism

## 1. Topic, number and variant

| | |
|---|---|
| Lab | 3 — Type hierarchy of the subject area |
| Variant | 20 — gym memberships (unchanged) |
| Author | Fedorchuk I. V., group KI-31 |
| Development OS | Windows 11 |
| CI | GitHub Actions: Ubuntu, Windows, macOS |
| Code state | tag [`v1.2.0`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.2.0), pull request [#12](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/12); completed in [`v1.2.1`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.2.1), pull request [#21](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/21) |

## 2. Goal

Turn the single entity class into a hierarchy: shared data and rules in a base type, different behaviour
in subtypes, a category as an enum, logical equality, and processing of different subtypes through
the base type without branching on the type.

## 3. State before and after

| Lab 2 | Lab 3 (`v1.2.1`) |
|---|---|
| one `final class Membership` | `abstract class Membership` + `MonthlyMembership`, `AnnualMembership` |
| no category | `enum MembershipKind { MONTHLY, ANNUAL }` |
| default object identity | `equals`/`hashCode` by subtype and client |
| only Lab 1 indicators | Lab 1 indicators + polymorphic visit cost + unique clients |

## 4. Base type

`Membership` is abstract: a membership without a kind cannot exist.

| Element | Content |
|---|---|
| Shared fields | `client`, `plan`, `months`, `visits`, `price`, `kind` — all `private final` |
| Invariants | client and plan not blank; months and visits at least 1 (they are divisors); price finite and not negative |
| Constructor | `protected`; checks run as arguments of `this(...)` before the object exists, so SpotBugs has no `CT_CONSTRUCTOR_THROW` |
| Abstract method | `double visitCost()` — every subtype has its own formula |
| Factory | `fromCsv(line)` creates the matching subtype |

## 5. Subtypes

| Subtype | Own rule | `visitCost()` |
|---|---|---|
| `MonthlyMembership` | — | `price / visits` |
| `AnnualMembership` | at most `MAX_MONTHS = 12` months | `(price / months) / max(1, visits / months)` |

A row becomes monthly when its plan is `Monthly` or its term is one month; otherwise it is annual.
Both subtypes are `final` and call `super(...)` first, so the shared checks always run.

## 6. enum

`MembershipKind` has the constants `MONTHLY` and `ANNUAL` with a `label()` for reports.
Each subtype passes its constant to the base constructor, so `getKind()` tells the category without
`instanceof`. The enum later becomes the grouping key of Lab 4.

## 7. equals and hashCode

Two memberships are equal when they have **the same subtype and the same client**:
`getClass() == other.getClass() && client.equals(other.client)`. `hashCode` uses the same fields,
`Objects.hash(getClass(), client)`. Both methods are `final`, so a subtype cannot break the contract.
A monthly and an annual membership of one client are different objects.

The check in a collection: `new HashSet<>(memberships).size()` gives the number of unique clients
in the report; the tests put two equal memberships into a `HashSet` and get one entry.

## 8. Polymorphism

```java
for (Membership membership : memberships) {
    double cost = membership.visitCost();   // the JVM calls the method of the real subtype
    totalVisitCost += cost;
    maxVisitCost = Math.max(maxVisitCost, cost);
}
```

The report code works with the base type only. Adding a new subtype does not change it; only the new
class, an enum constant and the factory condition are added.

## 9. Testing

At `v1.2.0`: 7 tests — creation, negative price, blank client, different visit cost of the subtypes,
one `HashSet` entry for equal objects, inequality of different subtypes.

At `v1.2.1`: 47 tests. Added: exact visit cost values (`150.0` and `100.0`), `fromCsv` for both subtypes
and nine kinds of invalid rows, the 12-month limit, `MembershipKind` order and labels, the record,
command-line options and the sample file with every kind of invalid row.

## 10. Infrastructure

```powershell
.\mvnw.cmd clean verify                           # 47 tests, SpotBugs: 0 bugs
java -jar target\gym-membership-1.2.1.jar --help
java -jar target\gym-membership-1.2.1.jar --input data\input.csv --output out\report.txt
```

CI: [Actions → Java CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml),
Ubuntu, Windows and macOS; the JAR of each run is published as an artifact.

## 11. GitHub Issues and pull requests

| Issue | Change | Check |
|---|---|---|
| [#7](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/7) | abstract base class `Membership` | PR #12, CI |
| [#8](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/8) | enum `MembershipKind` | PR #12, CI |
| [#9](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/9) | subtypes `MonthlyMembership`, `AnnualMembership` | PR #12, CI |
| [#10](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/10) | `equals` and `hashCode` | `HashSet` tests |
| [#11](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/11) | polymorphic `Main` and tests | PR #12, CI |
| [#13](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/13)–[#20](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/20) | cleanup: shared fields, `fromCsv`, record, Lab 1 indicators, CLI options, standards files, English docs | PR #21, 47 tests, CI |

## 12. Behaviour comparison

At `v1.2.0` the report showed only the Lab 3 indicators, and the Lab 1 indicators were lost.
The cleanup restored them (Issue #17), so the report of `v1.2.1` is compatible with both earlier labs:

| Indicator | Lab 1 | Lab 3 (`v1.2.0`) | `v1.2.1` |
|---|---|---|---|
| Valid records | 3 | 3 | 3 |
| Total revenue | 2900.50 | — | 2900.50 |
| Longest membership | 12 | — | 12 |
| Average visits | 25.00 | — | 25.00 |
| Unique clients | — | 3 | 3 |
| Highest visit cost | — | 55.56 | 55.56 |
| Total visit cost | — | 147.24 | 147.24 |

`SampleDataTest` and `MembershipSummaryTest` check these values automatically.

## 13. Academic integrity

- **Tools:** Google Gemini during the original work (`v1.2.0`); Claude (Anthropic) during the cleanup (`v1.2.1`).
- **Roles:** Developer (explanations, skeletons), Reviewer (review against the lab requirements),
  Validator (edge cases), Documenter (README, reports).
- **Gemini contribution:** explanations of dynamic dispatch and of `equals`/`hashCode` in hash tables,
  skeletons of the subtypes, a fix for the SpotBugs constructor warning.
- **Claude contribution:** review of the repository against the requirements of labs 1–3, the list of
  Issues #13–#20, the new base class, `CommandLine`, the tests and the English documentation.
- **Author's contribution:** the visit cost formulas, the Git workflow, the version read from the
  manifest, the subtypes on top of the new base class, `fromCsv` with its parsing helpers,
  `toString`, restoring the record from Git history, reviewing every change before committing.
- **Rejected suggestions:** rewriting the history of already merged lab branches (fixed forward
  instead, with tags on the old states); an IDE auto-import that made `Membership` extend
  `java.nio.channels.MembershipKey`.
- **Fixed mistakes:** visits truncated by integer division in the constructor of `AnnualMembership`;
  `plan` parsed but never stored; `[cite: 1]` leftovers of the AI tool in comments; a test whose two
  subtypes had equal costs.
- I walked through the whole program flow and can explain every line.

## 14. Answers to the control questions

1. The **base type** keeps the shared data and rules of every membership in one place, so they are not duplicated.
2. **`extends`** inherits a class (one only); **`implements`** promises the methods of an interface (several are allowed).
3. An **abstract** class cannot be instantiated and can declare methods without a body that every subtype must implement.
4. **`super(...)`** runs the constructor of the base class first, so the shared fields are set and validated.
5. **`@Override`** makes the compiler check that the method really overrides a method of the parent.
6. **Polymorphism** means one call (`visitCost()`) runs the code of the object's real type; no `if` on the type is needed.
7. An **interface** describes a capability shared by unrelated types; an **abstract class** shares state and code between related types.
8. "**Is a kind of**" is inheritance (an annual membership is a membership); **composition** is "has a" (a report has a list of memberships).
9. An **enum with fields and methods** gives every constant its own data, like a label for the report.
10. Every enum constant exists **only once**, so `==` compares them safely.
11. **equals/hashCode:** equal objects must have equal hash codes; `equals` is reflexive, symmetric, transitive and consistent.
12. If a field used in `hashCode` **changes** after the object is put into a `HashMap`, the map looks in the wrong bucket and cannot find it.
13. **Different subtypes** describe different memberships, so `getClass()` is part of equality even for the same client.
14. A **polymorphic collection** (`List<Membership>`) stores objects of any subtype; each call runs the method of the real subtype.
15. Tests that create both subtypes with known data and check **exact, different** `visitCost()` values.
16. Tests that add two equal objects to a `HashSet` (one entry) and two different subtypes (two entries).
17. **`sealed`** limits which classes may extend a type; **`permits`** lists them. The hierarchy is then closed and known to the compiler.
18. A **`switch` on subtypes** duplicates logic that already lives in the subtypes and must be changed for every new subtype.
19. The tests of labs 1–2 and the report values `3 / 2900.50 / 12 / 25.00`.
20. `membership.visitCost()` in the report loop: the same line gives the monthly and the annual formula without any check of the type.

## 15. Conclusions

The model now consists of an abstract base type, two subtypes with their own visit cost, an enum
category and logical equality. The report keeps the indicators of labs 1–3. This hierarchy and its
loops were the starting point of Lab 4, where the loops are replaced by the Stream API.

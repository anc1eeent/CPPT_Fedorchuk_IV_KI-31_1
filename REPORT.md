# Lab 7 — Object-oriented data processing in Python

## 1. Topic, number and variant

| | |
|---|---|
| Lab | 7 — Object-oriented data processing: classes, properties, dataclasses, subtypes |
| Variant | 20 — gym memberships |
| Author | Fedorchuk I. V., group KI-31 |
| Development OS | Windows 11, Python 3.13.15 |
| CI | GitHub Actions: Ubuntu, Windows, macOS, Python 3.11 |
| Version | `1.2.0`, tag `py-v1.2.0`, branch `lab-07-python`, pull request [#57](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/57) |

Requirements of variant 20 and where they are implemented:

| Part | Requirement | Implementation |
|---|---|---|
| Base class | `Membership` | `models.Membership` (abstract, `ABC`) |
| Subtypes | `MonthlyMembership`, `AnnualMembership` | `models.py` |
| Shared operation | `visit_cost()` by tariff | a different formula in every subtype |
| Summary | memberships that end | `summaries.ExpirySummary`, `@dataclass(frozen=True)` |
| CSV fields | `customer,kind,start,end,visits` (unchanged) | `loader.REQUIRED_COLUMNS` |
| Reference date | `2026-11-01`, option `--date` (unchanged) | `cli.DEFAULT_DATE` |

## 2. State before and after

| | Lab 6 (dictionaries) | Lab 7 (objects) |
|---|---|---|
| One record | `dict` described by `TypedDict` | instance of `MonthlyMembership` or `AnnualMembership` |
| Access to a field | `record["visits"]` | `record.visits` |
| Value rules | `if` checks in `loader.py` | `@property` setters and `__init__` of the class |
| Kind of membership | string `"monthly"` / `"annual"` | the class of the object; `kind` is a class constant |
| Behaviour | only outside functions | methods: `months`, `is_active_on`, `visit_cost` |
| Choosing a formula | would need `if record["kind"] == ...` | polymorphism: every subtype has its own `visit_cost` |
| Summary value | — | frozen `ExpirySummary` |
| Comparison | dictionaries are equal by all keys | `__eq__`: same subtype and same values |
| Report | 10 lines | the same 10 lines, then 5 new lines |

The loader is the only place where a row becomes an object:

```python
KINDS: dict[str, type[Membership]] = {
    AnnualMembership.kind: AnnualMembership,
    MonthlyMembership.kind: MonthlyMembership,
}
...
return KINDS[kind](customer, start, end, visits)
```

The dictionary chooses the class by the `kind` column, so no `if` on the kind is needed.

## 3. Model

`Membership(ABC)` is the base class; it cannot be created because `visit_cost` is abstract.

| Member | Kind | Rule |
|---|---|---|
| `customer` | property with setter | not blank, stored without spaces around it |
| `visits` | property with setter | `int`, 0 or more; `1.5`, `"12"` and `-1` raise `ValueError` |
| `start`, `end` | read-only properties | `end` must be after `start` (checked in `__init__`) |
| `months` | read-only computed property | calendar months from `start` to `end` |
| `kind` | `ClassVar[str]` | `"monthly"` or `"annual"`, the value of the CSV column |
| `is_active_on(day)` | method | `start <= day < end` |
| `visit_cost()` | abstract method | every subtype has its own formula |

The setters write to the inner fields `_customer`, `_visits`; `__init__` assigns through the
properties (`self.visits = visits`), so the same check works when the object is created and changed.

Special methods:

| Method | Result for Олег Савчук |
|---|---|
| `__str__` | `Олег Савчук: monthly, 2026-10-20 to 2026-11-20, 12 visits` |
| `__repr__` | `MonthlyMembership(customer='Олег Савчук', start=datetime.date(2026, 10, 20), end=datetime.date(2026, 11, 20), visits=12)` |
| `__eq__` | equal only to an object of the same subtype with the same four values; `NotImplemented` for other types |
| `__hash__` | hash of the subtype, `start` and `end`, the values that never change, so a changed name or visit count does not break a set |

Order: `leader` uses `key=(-visits, customer)` and `most_expensive_visit` uses
`key=(-visit_cost(), customer)`; a tie goes to the alphabetically first name. A `key` is clearer
here than `__lt__`, because memberships have no single natural order.

## 4. Dataclass

```python
@dataclass(frozen=True)
class ExpirySummary:
    reference_date: date
    expired: int
    ending_soon: int
    next_end: date | None
```

`summarize_expiry(records, reference_date, window=EXPIRY_WINDOW)` builds it:

| Field | Meaning | Control data |
|---|---|---|
| `expired` | memberships with `end <= reference_date` | 2 (Коваль, Петро; Марія Іванівна 2025) |
| `ending_soon` | active memberships that end within 30 days (inclusive) | 3 (Олег, Анна, Ірина: 30 days exactly) |
| `next_end` | the nearest end of an active membership | `2026-11-05` |

`@dataclass` generates `__init__`, `__repr__` and `__eq__`; `frozen=True` forbids changing a field
(`FrozenInstanceError`) and adds `__hash__`. A summary is a result: once computed it must not change.

## 5. Subtypes and protocol

| Subtype | `kind` | Tariff | `visit_cost()` |
|---|---|---|---|
| `MonthlyMembership` | `monthly` | 600 UAH for every month | `600 * months / max(1, visits)` |
| `AnnualMembership` | `annual` | 5400 UAH for the whole contract | `5400 / max(1, visits)` |

The monthly price depends on the length, the annual one does not. With 0 visits the price of the
whole period is the price of one visit (no division by zero).

| Customer | Subtype | Months | Visits | `visit_cost()` |
|---|---|---|---|---|
| Іван Федорчук | annual | 12 | 45 | 120.00 |
| Коваль, Петро | annual | 3 | 10 | 540.00 |
| Марія Іванівна | annual | 6 | 20 | 270.00 |
| Олег Савчук | monthly | 1 | 12 | 50.00 |
| Анна Литвин | monthly | 1 | 12 | 50.00 |
| Ірина Бондар | monthly | 1 | 6 | 100.00 |
| Марія Іванівна | annual | 6 | 18 | 300.00 |
| Олена Петрівна | monthly | 1 | 0 | 600.00 |

The shared behaviour is the protocol `VisitCostSource` (a property `customer` and a method
`visit_cost()`). `average_visit_cost` and `most_expensive_visit` accept any object that has them:
they call `item.visit_cost()` and Python runs the formula of the real class. There is no
`isinstance` and no check of `kind` in `analysis.py` or `summaries.py`. A test passes a small class
`Priced` that does not inherit `Membership` and works with the same functions (duck typing).

## 6. Modules

```text
src/gym_membership/
├── models.py       Membership, MonthlyMembership, AnnualMembership; no files, no CLI
├── summaries.py    VisitCostSource, average_visit_cost, most_expensive_visit, ExpirySummary
├── loader.py       CSV → objects; text to types, ValueError of the class → DataError with the line
├── analysis.py     Lab 6 indicators, now with attributes; works with both subtypes
├── reporting.py    report text and UTF-8 file
├── errors.py       DataError with the line and the field
└── cli.py          argparse, exit codes 0/1/2 (unchanged)
```

`models.py` does not read files, `loader.py` does not build the report, `analysis.py` does not
depend on the CLI. Importing a module runs no program: `cli.main` runs only from the console command
or `python -m gym_membership.cli`.

## 7. Testing

`python -m pytest`: **97 passed**.

| File | Tests | What they prove |
|---|---|---|
| `test_models.py` | 23 | values stored and stripped; abstract base; customer setter; visits setter: 0, 1, 12, 10 000 accepted, −1, 1.5, "12" rejected, also in `__init__`; read-only dates; end after start; months; active from start to end; exact `str` and `repr`; equality, hash and set; other subtype or other value is not equal; `NotImplemented` |
| `test_summaries.py` | 21 | subtypes and their `kind`; both formulas; zero visits; cost of every control record; same values of two subtypes are not equal; average and most expensive visit, tie, empty input; protocol with an object that is not a `Membership`; any iterable; expiry summary of the control data, window end inclusive, own window, empty input; frozen dataclass, generated `repr` and hash |
| `test_loader.py` | 18 | control data become objects of the right subtype; comma in quotes; empty file; header only; byte order mark; missing column; invalid rows; rule of the class becomes `DataError` with the line and keeps the `ValueError`; bad number and date; line numbers |
| `test_analysis.py` | 10 | the Lab 6 indicators on objects; input not changed |
| `test_reporting.py` | 11 | `format_line`; full report; the report starts with the saved Lab 6 report; empty selection; no records; UTF-8 with LF |
| `test_cli.py` | 14 | the saved file = Lab 6 report + new lines; `--date`; broken CSV → 1; rule of the class → 1 with the message; missing input → 1; invalid arguments → 2; version; separate process |

## 8. Report comparison

The full Lab 6 report on `data/input.csv` is saved in `tests/fixtures/lab6_report.txt`
(kept with LF on every system by `.gitattributes`). Two tests compare it byte for byte with the
beginning of the new report: the dictionary model and the object model give the same 10 lines.

```text
Gym membership report                                   ┐
Reference date: 2026-11-01                              │
Records: 8                                              │
Active memberships: 5                                   │
Active customers: Іван Федорчук; Марія Іванівна; ...    │  Lab 6 report,
Visits by kind: annual=93, monthly=30                   │  unchanged
Unique customers: 7                                     │
Customers: Іван Федорчук; Ірина Бондар; ...             │
Average visits: 15.38                                   │
Leader: Іван Федорчук (45 visits)                       ┘
Average visit cost: 253.75                              ┐
Most expensive visit: Олена Петрівна (600.00)           │  added by the
Expired: 2                                              │  object model
Ending within 30 days: 3                                │
Next end: 2026-11-05                                    ┘
```

The CSV format, the CLI arguments and the exit codes did not change. The only difference in error
messages: the rules "end after start" and "visits from 0" moved to the class, so their text now
comes from the class: `line 2: visits must be a whole number from 0, got -3`.

## 9. Infrastructure

```powershell
.\.venv\Scripts\python.exe -m ruff check .            # All checks passed!
.\.venv\Scripts\python.exe -m ruff format --check .   # files already formatted
.\.venv\Scripts\python.exe -m pytest                  # 97 passed
.\.venv\Scripts\python.exe -m build --wheel           # dist/gym_membership_py-1.2.0-py3-none-any.whl

python -m venv .venv-wheel
.\.venv-wheel\Scripts\python.exe -m pip install --no-deps .\dist\gym_membership_py-1.2.0-py3-none-any.whl
.\.venv-wheel\Scripts\gym-membership.exe data\input.csv reports\wheel-summary.txt
```

[Python CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml) is the
same as in Lab 6: on Ubuntu, Windows and macOS with Python 3.11 it runs ruff, pytest, the wheel
build, the installed CLI on `data/input.csv`, and uploads the wheel as `wheel-<os>-py3.11-<run>`.

## 10. GitHub Issues and pull request

| Issue | Change | Check |
|---|---|---|
| [#51](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/51) | base class `Membership` with properties and special methods | `test_models.py` |
| [#52](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/52) | `MonthlyMembership`, `AnnualMembership`, `visit_cost()` | `test_summaries.py` (subtypes) |
| [#53](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/53) | `VisitCostSource`, `ExpirySummary` | `test_summaries.py` (protocol, dataclass) |
| [#54](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/54) | loader builds objects, analysis uses attributes, report lines | `test_loader.py`, `test_analysis.py`, `test_reporting.py` |
| [#55](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/55) | comparison with the Lab 6 report, CLI tests, LF fixture | `test_cli.py`, 97 tests |
| [#56](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/56) | README, REPORT, docstrings, version 1.2.0 | green CI on three systems, tag `py-v1.2.0` |

## 11. Academic integrity

- **Tool:** Claude (Anthropic), in the roles Manager (Issues #51–#56), Developer, Validator
  (boundary values of the setters, edge cases), DevOps (line ends in CI), Documenter (README,
  REPORT) and Reviewer.
- **My part:** I wrote the base class `Membership` myself — the properties with setters, `months`,
  `is_active_on`, the abstract `visit_cost`, `__str__`, `__repr__`, `__eq__`, `__hash__` — from the
  tests in `test_models.py`, with explanations and hints from Claude; Claude reviewed it. I changed
  `analysis.py` (written by me in Lab 6) from keys to attributes. I created the Issues and the branch,
  ran every check locally, made the commits and the pull request.
- **Written by Claude at my request:** the subtypes, `summaries.py`, the changes of `loader.py` and
  `reporting.py`, all tests, the Lab 6 report fixture, README and this report.
- **Accepted:** tests of the base class written before the code, as its specification; a dictionary
  `kind → class` in the loader instead of `if`; value rules inside the class; a frozen dataclass for
  the summary; `key` functions instead of `__lt__`; the saved Lab 6 report as the proof of
  compatibility.
- **Rejected:** an annual formula with integer division `visits // months` (hard to explain, the same
  price for different data); `isinstance` checks in the analysis; keeping the old rule checks in the
  loader next to the same checks in the class.
- **Fixed mistakes:** in the setters my first version compared `value.strip() == None`, did not
  `raise` and did not store the stripped value; `months` assigned an undefined name; `__str__`
  returned `print(...)`; `__repr__` had a hard-coded class name and manual date formatting; one line
  had a wrong indent and failed `ruff format --check`. Claude found that the LF fixture would turn
  into CRLF on the Windows runner and fixed it with `.gitattributes`.

## 12. Answers to the control questions

1. **A class** is the description (fields, methods); **an instance** is one object made from it with
   its own values.
2. **`__init__`** fills a new object with its first values; **`self`** is that object.
3. **`@property`** reads like a field but runs code: it checks or computes a value.
4. **A setter writes to `_visits`**, not to `visits`: writing to the property would call the setter
   again without end.
5. **`@dataclass`** generates `__init__`, `__repr__` and `__eq__` from the listed fields.
6. **`frozen=True`** forbids changing fields after creation and makes the object hashable.
7. **`__str__`** is short text for people (`print`); **`__repr__`** is exact text for developers, close
   to the code that creates the object.
8. **`__eq__` with another type** returns `NotImplemented`, so Python can try the other object and
   finally gives `False`.
9. **`key`** is better when there are several useful orders or no natural one, as for memberships;
   `__lt__` fixes one order for the class.
10. **`super()`** calls the method of the parent class, for example its `__init__`, without naming it.
11. **MRO** is the order in which Python looks for a method in the class and its parents; with several
    parents it decides which method runs.
12. **Duck typing:** the code uses what an object can do, not what class it is.
13. **`Protocol`** writes this contract down for type checkers without making classes inherit it.
14. **No subtype checks in analysis:** a new subtype would need changes in every function; with a
    shared method it only needs its own class.
15. **Dictionary → object** happens in the loader, the border between the file and the program.
16. **No CLI on import:** importing a module in a test or another module must not read files or exit.
17. **Tested properties:** setters with correct, zero, boundary and wrong values, read-only fields,
    `str`, `repr`, equality, hash, subtypes, the dataclass and the shared function.
18. **pytest** checks behaviour, **ruff** checks code quality and format, **wheel** checks that the
    package can be installed.
19. **Edge cases:** empty input, header only, zero visits, the end day, the 30-day boundary, equal
    values, wrong types, broken CSV rows.
20. **REPORT declares** the classes, fields, subtypes and the shared operation, the old and the new
    report, the test results, CI and the contribution of AI.

## 13. Conclusions

The memberships of variant 20 are now objects: `Membership` guards its values with properties, two
subtypes compute `visit_cost()` with their own tariffs, `ExpirySummary` keeps the result of the
expiry analysis and cannot be changed. The functions work through the shared protocol without
checking the class. The Lab 6 report is reproduced byte for byte and extended with 5 lines;
97 tests, ruff and the wheel pass on Ubuntu, Windows and macOS. The object model is the base for
the next lab: its own storage of objects and context managers.

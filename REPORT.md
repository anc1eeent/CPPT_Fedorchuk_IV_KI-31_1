# Lab 6 — Data processing in a Python package

## 1. Topic, number and variant

| | |
|---|---|
| Lab | 6 — Data processing: CSV, collections, comprehensions and functions |
| Variant | 20 — gym memberships |
| Author | Fedorchuk I. V., group KI-31 |
| Development OS | Windows 11, Python 3.13.15 |
| CI | GitHub Actions: Ubuntu, Windows, macOS, Python 3.11 |
| Version | `1.1.0`, tag `py-v1.1.0`, branch `lab-06-python`, pull request [#50](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/50) |

Requirements of variant 20:

| Part | Requirement | Implementation |
|---|---|---|
| CSV fields | `customer,kind,start,end,visits` | `loader.REQUIRED_COLUMNS` |
| Selection | membership active on `reference_date` | `active_on`: `start <= reference_date < end` |
| Dictionary | visits by `kind` | `visits_by_kind` |
| Set | customers | `unique_customers` |
| Report | average number of visits and the leader | `average_visits`, `leader` |
| Reference date | `2026-11-01` (ISO), option `--date` | `cli.DEFAULT_DATE` |

## 2. Goal

Build a Python package that reads the CSV of the variant, converts every row to typed values,
computes the indicators with the standard collections and comprehensions, and writes a stable
text report. The package runs as a CLI, is checked with pytest and ruff, and is built into a wheel.

Result: `gym-membership data/input.csv reports/summary.txt` prints and saves the report of section 9
and returns exit code 0; a broken CSV returns 1 with the line and the field of the error.

## 3. CSV format

UTF-8, comma-separated, first line is the header:

```text
customer,kind,start,end,visits
Іван Федорчук,annual,2026-01-15,2027-01-15,45
"Коваль, Петро",annual,2026-08-01,2026-11-01,10
Марія Іванівна,annual,2026-06-10,2026-12-10,20
Олег Савчук,monthly,2026-10-20,2026-11-20,12
Анна Литвин,Monthly,2026-10-05,2026-11-05,12
Ірина Бондар,monthly,2026-11-01,2026-12-01,6
Марія Іванівна,annual,2025-12-01,2026-06-01,18
Олена Петрівна,monthly,2026-11-02,2026-12-02,0
```

| Field | Type | Rule |
|---|---|---|
| `customer` | `str` | not blank |
| `kind` | `str` | `annual` or `monthly` after `strip().lower()` |
| `start` | `date` | ISO `YYYY-MM-DD` |
| `end` | `date` | ISO, later than `start` |
| `visits` | `int` | whole number, not negative |

Required cases of the data:

| Case | Row |
|---|---|
| text with a comma | `"Коваль, Петро"`: quoted, read as one field |
| duplicated value | Марія Іванівна has two memberships; the set holds her once |
| boundary number | Олена Петрівна, `visits = 0`, the smallest allowed value |
| boundary dates | Петро ends and Ірина starts exactly on 2026-11-01 |
| normalisation | `Monthly` of Анна Литвин becomes `monthly` |
| empty selection | `--date 2025-01-01`: no membership is active (section 7) |

Empty input: an empty file and a file with only the header are both valid and give a report with
zeros and `none`.

## 4. Module structure

| Module | Purpose | Public names |
|---|---|---|
| `errors.py` | the error of the input data | `DataError(message, line=, field=)` |
| `loader.py` | reading and checking the CSV, type conversion | `load_memberships`, `parse_row`, `Membership` |
| `analysis.py` | indicators of the variant, no input or output | `active_on`, `visits_by_kind`, `unique_customers`, `average_visits`, `leader` |
| `reporting.py` | report text and file | `format_line`, `format_report`, `write_report` |
| `cli.py` | arguments, the program flow, exit codes | `build_parser`, `main` |

The dependencies go one way: `cli` → `reporting` → `analysis` → `loader` → `errors`. Importing any
module runs no program code: `cli.main()` runs only under `if __name__ == "__main__"` or through
the console command.

## 5. Data conversion

A record is a dictionary. `Membership` is a `TypedDict`: it only tells the editor and the reader
which keys a record has; at run time it is an ordinary `dict`.

```python
{
    "customer": "Іван Федорчук",
    "kind": "annual",
    "start": date(2026, 1, 15),
    "end": date(2027, 1, 15),
    "visits": 45,
}
```

| Structure | Where | Why |
|---|---|---|
| list | records from the loader, `active_on` | keeps the order of the file |
| dictionary | every record, `visits_by_kind` | value by name of the field or the kind |
| set | `unique_customers`, kinds in `visits_by_kind` | removes duplicates |

Comprehensions:

```python
[record for record in records if record["start"] <= reference_date < record["end"]]  # list
{record["customer"] for record in records}  # set
kinds = sorted({record["kind"] for record in records})  # set
{
    kind: sum(record["visits"] for record in records if record["kind"] == kind)  # dictionary
    for kind in kinds
}
[parse_row(row, reader.line_num) for row in reader]  # list, loader
```

Five fields become typed values: `start` and `end` → `date`, `visits` → `int`, `kind` → normalised
text, `customer` → text without surrounding spaces.

## 6. Functions

`reporting.format_line` has a default parameter, `*args` and `**kwargs`:

```python
def format_line(label: str, *values: object, joiner: str = ", ", **pairs: object) -> str:
    parts = [str(value) for value in values] + [f"{key}={value}" for key, value in pairs.items()]
    return f"{label}: {joiner.join(parts) if parts else 'none'}"
```

| Call | Result |
|---|---|
| `format_line("Records", 8)` | `Records: 8` |
| `format_line("Visits by kind", annual=93, monthly=30)` | `Visits by kind: annual=93, monthly=30` |
| `format_line("Customers", "Коваль, Петро", "Анна", joiner="; ")` | `Customers: Коваль, Петро; Анна` |
| `format_line("Leader")` | `Leader: none` |

Inside the function `values` is a tuple of the positional arguments and `pairs` is a dictionary of
the keyword arguments. The default `", "` is a string, which cannot be changed, so it is safe as a
default value.

## 7. Analysis algorithm

| Step | Function | Control data |
|---|---|---|
| selection | `active_on`: list comprehension, `start <= date < end` | 5 of 8; Петро (end = date) out, Ірина (start = date) in |
| aggregation | `visits_by_kind`: sorted set of kinds, then dict comprehension with `sum` | `annual=93, monthly=30` |
| unique values | `unique_customers`: set comprehension | 7 customers from 8 records |
| average | `sum(...) / len(records)`, `0.0` for no records | 123 / 8 = 15.375 → `15.38` |
| leader | `min(records, key=lambda r: (-r["visits"], r["customer"]), default=None)` | Іван Федорчук, 45 |

The leader key is a tuple: the negative number of visits makes `min` choose the most visits, and
for equal visits the second element chooses the alphabetically first customer (Анна before Олег).

Deterministic order:

- active customers keep the order of the file;
- kinds and customers are sorted by Unicode code points, which do not depend on the language of the
  system (that is why `Іван` comes before `Анна`: `І` is U+0406, `А` is U+0410);
- numbers are printed with two decimals.

With `--date 2025-01-01` the selection is empty: `Active memberships: 0`, `Active customers: none`.

## 8. Error handling

Every rule of the data raises `DataError` with the line of the file and the field. A conversion
error keeps the original exception as the cause:

```python
try:
    visits = int(text)
except ValueError as error:
    raise DataError(f"not a whole number: '{text}'", line=line, field="visits") from error
```

| Case | Message |
|---|---|
| missing column | `line 1: missing columns: visits` |
| missing value | `line 2, field 'customer': missing value` |
| bad number | `line 2, field 'visits': not a whole number: 'twelve'` (cause `ValueError`) |
| negative number | `line 2, field 'visits': must not be negative, got -1` |
| bad date | `line 2, field 'start': not a date (YYYY-MM-DD): '05.10.2026'` (cause `ValueError`) |
| unknown kind | `line 2, field 'kind': unknown kind 'weekly', expected one of annual, monthly` |
| end not after start | `line 2, field 'end': end 2026-10-05 is not after start 2026-10-05` |
| extra value | `line 2: too many values` |

There is no bare `except`. The CLI catches only `DataError` and `OSError`:

| Exit code | When | Output |
|---|---|---|
| 0 | success | report and `Report saved to <path>` on stdout |
| 1 | `DataError`, missing or unreadable file | `Error: <message>` on stderr, no report file |
| 2 | invalid arguments (argparse) | usage and the reason on stderr |

## 9. File report

`reporting.write_report` creates the folder and writes the text:

```python
path.parent.mkdir(parents=True, exist_ok=True)
with path.open("w", encoding="utf-8", newline="\n") as file:
    file.write(text)
```

`with` closes the file even when writing fails. The report is UTF-8 with LF line ends on every
system. The CLI refuses an output path equal to the input path, so the input is never changed.

Full report of the control data (`reports/summary.txt`):

```text
Gym membership report
Reference date: 2026-11-01
Records: 8
Active memberships: 5
Active customers: Іван Федорчук; Марія Іванівна; Олег Савчук; Анна Литвин; Ірина Бондар
Visits by kind: annual=93, monthly=30
Unique customers: 7
Customers: Іван Федорчук; Ірина Бондар; Анна Литвин; Коваль, Петро; Марія Іванівна; Олег Савчук; Олена Петрівна
Average visits: 15.38
Leader: Іван Федорчук (45 visits)
```

## 10. Testing

`python -m pytest`: **49 passed**.

| File | Tests | What they prove |
|---|---|---|
| `test_loader.py` | 17 | control data with types; comma in quotes; empty file; header only; byte order mark; missing column; 7 invalid rows (missing values, unknown kind, end before start, negative visits, extra value); bad number and bad date keep the `ValueError`; line numbers; input not changed |
| `test_analysis.py` | 10 | active on the date in file order; start included, end excluded; empty selection; sums by kind in sorted order; empty dictionary; duplicates in the set; average and `0.0`; leader and `None`; tie by name; input not changed |
| `test_reporting.py` | 10 | `format_line` with `*values`, `**pairs`, another joiner and no values; full control report; stable text; empty selection; no records; UTF-8 file with LF; old report replaced |
| `test_cli.py` | 12 | report printed and saved; `--date`; broken CSV → 1 without a report; missing input → 1; five invalid argument sets → 2; `--version`; version equals `pyproject.toml`; `python -m gym_membership.cli` in a separate process |

Tests check behaviour (what a function returns, what the program prints and writes), not the
inner steps, so the code can be rewritten in Lab 7 without changing them.

## 11. Quality and packaging

```powershell
.\.venv\Scripts\python.exe -m ruff check .            # All checks passed!
.\.venv\Scripts\python.exe -m ruff format --check .   # files already formatted
.\.venv\Scripts\python.exe -m pytest                  # 49 passed
.\.venv\Scripts\python.exe -m build --wheel           # dist/gym_membership_py-1.1.0-py3-none-any.whl

python -m venv .venv-wheel
.\.venv-wheel\Scripts\python.exe -m pip install --no-deps .\dist\gym_membership_py-1.1.0-py3-none-any.whl
.\.venv-wheel\Scripts\python.exe -m gym_membership.cli data\input.csv reports\wheel-summary.txt
```

Ruff rules: `E`, `W` (style), `F` (unused and undefined names), `I` (import order), `UP` (modern
syntax), `B` (likely bugs), `SIM` (simpler code); line length 100.

## 12. GitHub Actions

[Python CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml) runs on
`ubuntu-latest`, `windows-latest` and `macos-latest` with Python 3.11: ruff check, ruff format check,
pytest, wheel build, installation of the built wheel, `gym-membership --version`, the report of
`data/input.csv` by the installed CLI, and upload of the wheel as the artifact
`wheel-<os>-py3.11-<run>` of every job. `PYTHONUTF8=1` makes the console of the Windows runner print
Ukrainian text.

## 13. GitHub Issues and pull request

| Issue | Change | Check |
|---|---|---|
| [#44](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/44) | `errors.py`, `loader.py`, `data/input.csv` | `test_loader.py` |
| [#45](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/45) | `analysis.py` | `test_analysis.py` |
| [#46](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/46) | `reporting.py` with `format_line` | `test_reporting.py` |
| [#47](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/47) | `cli.py` | `test_cli.py` |
| [#48](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/48) | test data and tests of every module | 49 tests, ruff |
| [#49](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/issues/49) | README, REPORT, version 1.1.0, CI runs the report | green CI on three systems, tag `py-v1.1.0` |

## 14. Academic integrity

- **Tool:** Claude (Anthropic), in the roles Manager (Issues #44–#49), Developer, Validator (edge
  cases of the CSV), DevOps (CI), Documenter (README, REPORT) and Reviewer.
- **My part:** I wrote `analysis.py` myself — all five functions — from the tests in
  `test_analysis.py`, with explanations and hints from Claude for each function; Claude reviewed it.
  I created the Issues and the branch, ran every check locally, created the CI file, made the
  commits and the pull request.
- **Written by Claude at my request:** `errors.py`, `loader.py`, `reporting.py`, `cli.py`, all tests,
  `data/input.csv`, README and this report.
- **Accepted:** tests of `analysis.py` written before the code, as its specification; `TypedDict`
  for records; names joined with `;`; sorting by code points for a stable report; refusing an output
  equal to the input.
- **Rejected:** sorting names by Ukrainian alphabet with the system locale (the report would differ
  between systems); `split(",")` for CSV.
- **Fixed mistakes:** in `visits_by_kind` my first condition `kind == kind` was always true and gave
  123 for both kinds; in `leader` my first key `-visits` chose the first of two equal records instead
  of the alphabetically first; the import order rule needed `known-first-party` for the package.

## 15. Answers to the control questions

1. **pyproject.toml** describes the package in one file: name, version, Python version, dependencies,
   the console command and the settings of pytest and ruff.
2. **A virtual environment** keeps the packages of this project apart from the system and other
   projects, so the result can be repeated on any computer.
3. **`newline=""`** lets the csv module read line breaks itself, so a quoted value with a line break
   stays one field and `\r\n` is not doubled.
4. **csv.DictReader** gives every value as `str`; missing values are `None`. Dates and numbers must be
   converted by the program.
5. **`split(",")`** splits inside quoted values such as `"Коваль, Петро"` and does not remove the quotes.
6. **A list** keeps order and duplicates, **a dictionary** finds a value by its key, **a set** keeps only
   unique values without order.
7. **A comprehension** is clearer for one short transformation or filter; a loop is better when
   there are several steps, `try/except` or side effects.
8. **`*args`** is a tuple of the extra positional arguments, **`**kwargs`** is a dictionary of the extra
   keyword arguments.
9. **A mutable default** (for example `[]`) is created once and shared by every call, so changes from
   one call appear in the next.
10. **`raise ... from error`** stores the original exception in `__cause__`, and the traceback shows both.
11. **Separate modules** can be tested and changed one by one: the analysis does not need files, the
    report does not need CSV parsing.
12. **`with`** closes the file when the block ends, also when an exception is raised.
13. **A deterministic order** makes the report the same on every run and system, so it can be compared
    and tested byte for byte.
14. **Empty CSV:** an empty file, a file with only the header, and an empty selection; all give a valid
    report with zeros and `none`, not an error.
15. **A behaviour test** checks what the code returns or writes; a test of the implementation checks how
    it does it and breaks when the code is rewritten.
16. **pytest** finds files `test_*.py` or `*_test.py` (here in `tests/`) and functions `test_*` in them.
17. **ruff** checks style, unused and undefined names, import order, outdated syntax, likely bugs, and
    the formatting of the code.
18. **Exit codes:** 0 for success, 1 for invalid data or a file error, 2 for invalid arguments.
19. **Tests, analysis and packaging in CI** check different things: behaviour, code quality, and that
    the checked code can be installed on every system.
20. **REPORT must declare** the CSV header and example, the reference date, the full report, the exit
    codes, the test results and the CI results.

## 16. Conclusions

The package reads and checks the CSV of variant 20 with `csv.DictReader`, keeps the records as
dictionaries, computes the selection, the aggregation, the unique customers, the average and the
leader with comprehensions, and writes a stable UTF-8 report through a CLI with clear exit codes.
49 tests, ruff and the wheel build pass on Ubuntu, Windows and macOS.

Lab 7 rebuilds the records as objects (`Membership` with properties and two subtypes). The loader,
the report format and the tests of this lab stay the base: the new object model must give the same
report.

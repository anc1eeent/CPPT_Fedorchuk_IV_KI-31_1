# Cross-Platform Programming Tools — Python labs

[![Python CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml/badge.svg?branch=main-python)](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml?query=branch%3Amain-python)

Laboratory works for the course "Cross-Platform Programming Tools",
Lviv Polytechnic National University. Author: Fedorchuk I. V., group KI-31.

**Variant 20 — gym memberships.** The Python part continues the Java labs of the
[`main-java`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/main-java) branch.
The package `gym_membership` reads memberships from CSV and turns every row into an object
of a subtype (`MonthlyMembership` or `AnnualMembership`), computes the indicators of the variant
and writes a stable text report.

## Stages

| Stage | Topic | Code | Pull request |
|---|---|---|---|
| 0 | Project infrastructure: pyproject.toml, pytest, ruff, wheel, CLI, CI | [py-v1.0.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/py-v1.0.0) | [#43](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/43) |
| 6 | Data processing: CSV, collections, comprehensions, functions | [py-v1.1.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/py-v1.1.0) | [#50](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/50) |
| 7 | Object model: classes, properties, dataclasses, subtypes, protocol | [py-v1.2.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/py-v1.2.0) | [#57](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/57) |

The report of the latest lab is in [`REPORT.md`](REPORT.md).
The overview of the whole course (Java and Python) is in the [`main`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/main) branch.

## Input format

`data/input.csv`, UTF-8, comma-separated, with a header:

```text
customer,kind,start,end,visits
Іван Федорчук,annual,2026-01-15,2027-01-15,45
"Коваль, Петро",annual,2026-08-01,2026-11-01,10
```

| Field | Attribute of the object | Rule |
|---|---|---|
| `customer` | `customer: str` | not blank; a value with a comma is quoted |
| `kind` | the class: `annual` → `AnnualMembership`, `monthly` → `MonthlyMembership` | any letter case |
| `start` | `start: datetime.date` | ISO `YYYY-MM-DD` |
| `end` | `end: datetime.date` | ISO, later than `start` |
| `visits` | `visits: int` | whole number, 0 or more |

An empty file and a file with only the header are valid and give an empty report.
Any other problem stops the program with `DataError`, which names the line (and the field,
when the loader finds it).

## Object model

| Class | What it adds |
|---|---|
| `Membership` | abstract base: properties with checks, `months`, `is_active_on()`, `str`, `repr`, `==`, `hash` |
| `MonthlyMembership` | `visit_cost()` = 600 UAH × months ÷ visits |
| `AnnualMembership` | `visit_cost()` = 5400 UAH for the contract ÷ visits |
| `ExpirySummary` | frozen dataclass: expired, ending within 30 days, next end |

Zero visits count as one visit in `visit_cost()`. The functions in `summaries.py` work with any
object that has `customer` and `visit_cost()` (protocol `VisitCostSource`).

## Report

```text
gym-membership data/input.csv reports/summary.txt [--date YYYY-MM-DD]
```

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
Average visit cost: 253.75
Most expensive visit: Олена Петрівна (600.00)
Expired: 2
Ending within 30 days: 3
Next end: 2026-11-05
Report saved to reports/summary.txt
```

- A membership is active when `start <= reference date < end`; the default date is `2026-11-01`.
- Active customers keep the order of the file; all customers are sorted by Unicode code points,
  so the order is the same on every system. Names are separated by `;` because a name may contain a comma.
- The first 10 lines are the report of Lab 6 without any change; a test compares them with
  `tests/fixtures/lab6_report.txt` byte for byte.
- A membership has expired when `end <= reference date`; it ends soon when it is active and its end
  is at most 30 days after the reference date.
- The report file is UTF-8 with LF line ends; it never overwrites the input file.

| Exit code | Meaning |
|---|---|
| 0 | the report was printed and saved |
| 1 | the CSV is invalid or a file cannot be read or written; the message is on stderr |
| 2 | invalid arguments: a missing path, a bad date, the output equal to the input |

## Requirements

- Python 3.11 or newer (developed with 3.13, checked in CI with 3.11)
- Git

## Setup

Windows PowerShell:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -e ".[dev]"
```

macOS and Ubuntu:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install --upgrade pip
.venv/bin/python -m pip install -e ".[dev]"
```

## Checks

The commands use the Windows path; on macOS and Ubuntu use `.venv/bin/python`.

| Command | What it checks |
|---|---|
| `.\.venv\Scripts\python.exe -m ruff check .` | errors and style (rules in `pyproject.toml`) |
| `.\.venv\Scripts\python.exe -m ruff format --check .` | formatting |
| `.\.venv\Scripts\python.exe -m pytest` | 97 behaviour tests in `tests/` |
| `.\.venv\Scripts\python.exe -m build --wheel` | the package builds into `dist/*.whl` |

## Wheel in a clean environment

```powershell
python -m venv .venv-wheel
.\.venv-wheel\Scripts\python.exe -m pip install --no-deps .\dist\gym_membership_py-1.2.0-py3-none-any.whl
.\.venv-wheel\Scripts\python.exe -m gym_membership.cli data\input.csv reports\wheel-summary.txt
```

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push and pull request on
`ubuntu-latest`, `windows-latest` and `macos-latest` with Python 3.11: ruff, pytest, the wheel build,
the installed CLI on `data/input.csv`, and the wheel upload as an artifact `wheel-<os>-py<version>-<run>`.

## Project structure

```text
pyproject.toml              package metadata, dependencies, pytest and ruff settings
data/input.csv              control data of variant 20
src/gym_membership/
├── __init__.py             package version from the installed metadata
├── errors.py               DataError with the line and the field
├── models.py               Membership and its two subtypes
├── summaries.py            VisitCostSource protocol, visit costs, ExpirySummary
├── loader.py               csv.DictReader, header check, CSV rows → objects
├── analysis.py             selection, aggregation, unique values, average, leader
├── reporting.py            stable report text and UTF-8 file
└── cli.py                  argparse CLI and exit codes
tests/                      pytest tests of every module
tests/fixtures/             the Lab 6 report for the comparison
.github/workflows/ci.yml    Python CI on three operating systems
```

`.venv*`, `build/`, `dist/`, `reports/`, `*.egg-info/` and the caches are generated and are not stored in Git.

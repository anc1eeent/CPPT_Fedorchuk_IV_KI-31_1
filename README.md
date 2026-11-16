# Cross-Platform Programming Tools — Python labs

[![Python CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml/badge.svg?branch=main-python)](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml?query=branch%3Amain-python)

Laboratory works for the course "Cross-Platform Programming Tools",
Lviv Polytechnic National University. Author: Fedorchuk I. V., group KI-31.

**Variant 20 — gym memberships.** The Python part continues the Java labs of the
[`main-java`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/main-java) branch.
The package `gym_membership` reads memberships from CSV, checks and converts every row,
computes the indicators of the variant with lists, dictionaries, sets and comprehensions
and writes a stable text report.

## Stages

| Stage | Topic | Code | Pull request |
|---|---|---|---|
| 0 | Project infrastructure: pyproject.toml, pytest, ruff, wheel, CLI, CI | [py-v1.0.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/py-v1.0.0) | [#43](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/43) |
| 6 | Data processing: CSV, collections, comprehensions, functions | py-v1.1.0 | this branch |
| 7 | Object model: classes, properties, dataclasses, polymorphism | planned | — |

The report of the latest lab is in [`REPORT.md`](REPORT.md).

## Input format

`data/input.csv`, UTF-8, comma-separated, with a header:

```text
customer,kind,start,end,visits
Іван Федорчук,annual,2026-01-15,2027-01-15,45
"Коваль, Петро",annual,2026-08-01,2026-11-01,10
```

| Field | Type after loading | Rule |
|---|---|---|
| `customer` | `str` | not blank; a value with a comma is quoted |
| `kind` | `str` | `annual` or `monthly`, any letter case |
| `start` | `datetime.date` | ISO `YYYY-MM-DD` |
| `end` | `datetime.date` | ISO, later than `start` |
| `visits` | `int` | whole number, 0 or more |

An empty file and a file with only the header are valid and give an empty report.
Any other problem stops the program with `DataError`, which names the line and the field.

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
Report saved to reports/summary.txt
```

- A membership is active when `start <= reference date < end`; the default date is `2026-11-01`.
- Active customers keep the order of the file; all customers are sorted by Unicode code points,
  so the order is the same on every system. Names are separated by `;` because a name may contain a comma.
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
| `.\.venv\Scripts\python.exe -m pytest` | 49 behaviour tests in `tests/` |
| `.\.venv\Scripts\python.exe -m build --wheel` | the package builds into `dist/*.whl` |

## Wheel in a clean environment

```powershell
python -m venv .venv-wheel
.\.venv-wheel\Scripts\python.exe -m pip install --no-deps .\dist\gym_membership_py-1.1.0-py3-none-any.whl
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
├── loader.py               csv.DictReader, header check, type conversion
├── analysis.py             selection, aggregation, unique values, average, leader
├── reporting.py            stable report text and UTF-8 file
└── cli.py                  argparse CLI and exit codes
tests/                      pytest tests of every module
.github/workflows/ci.yml    Python CI on three operating systems
```

`.venv*`, `build/`, `dist/`, `reports/`, `*.egg-info/` and the caches are generated and are not stored in Git.

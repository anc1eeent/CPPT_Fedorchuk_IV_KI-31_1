# Cross-Platform Programming Tools — Python labs

[![Python CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml/badge.svg?branch=main-python)](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml?query=branch%3Amain-python)

Laboratory works for the course "Cross-Platform Programming Tools",
Lviv Polytechnic National University. Author: Fedorchuk I. V., group KI-31.

**Variant 20 — gym memberships.** The Python part continues the Java labs of the
[`main-java`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/main-java) branch:
the same domain, a Python package `gym_membership` with tests, static checks and a wheel
built on Ubuntu, Windows and macOS.

## Stages

| Stage | Topic | Code | Pull request |
|---|---|---|---|
| 0 | Project infrastructure: pyproject.toml, pytest, ruff, wheel, CLI, CI | py-v1.0.0 | this branch |
| 6 | Data processing: CSV, collections, comprehensions, functions | planned | — |
| 7 | Object model: classes, properties, dataclasses, polymorphism | planned | — |

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

`-e` installs the package in editable mode: changes in `src/` take effect without a reinstall.
`.[dev]` adds the pinned development tools: pytest, ruff and build.

## Checks

The commands below use the Windows path; on macOS and Ubuntu use `.venv/bin/python`.

| Command | What it checks |
|---|---|
| `.\.venv\Scripts\python.exe -m ruff check .` | errors and style (rules in `pyproject.toml`) |
| `.\.venv\Scripts\python.exe -m ruff format --check .` | formatting |
| `.\.venv\Scripts\python.exe -m pytest` | behaviour tests in `tests/` |
| `.\.venv\Scripts\python.exe -m build --wheel` | the package builds into `dist/*.whl` |

## Command-line interface

```text
gym-membership             # prints the ready message
gym-membership --version   # gym-membership 1.0.0
python -m gym_membership.cli --version
```

The version is read from the installed package metadata, so it always equals `project.version`
in `pyproject.toml`.

## Wheel in a clean environment

```powershell
python -m venv .venv-wheel
.\.venv-wheel\Scripts\python.exe -m pip install --no-deps .\dist\gym_membership_py-1.0.0-py3-none-any.whl
.\.venv-wheel\Scripts\gym-membership.exe --version
```

`--no-deps` proves that the wheel holds the package and its metadata and does not rely on the
development environment.

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push and pull request on
`ubuntu-latest`, `windows-latest` and `macos-latest` with Python 3.11:

1. install the project with development tools;
2. `ruff check` and `ruff format --check`;
3. `pytest`;
4. build the wheel, install it over the editable install and run the installed CLI;
5. upload the wheel as an artifact `wheel-<os>-py<version>-<run>`.

## Project structure

```text
pyproject.toml              package metadata, dependencies, pytest and ruff settings
src/gym_membership/
├── __init__.py             package version from the installed metadata
└── cli.py                  argparse CLI with --version
tests/
└── test_cli.py             CLI tests: exit codes, output, version
.github/workflows/ci.yml    Python CI on three operating systems
```

`.venv*`, `build/`, `dist/`, `*.egg-info/` and the caches are generated and are not stored in Git.

# Cross-Platform Programming Tools — laboratory works

[![Java CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml/badge.svg?branch=main-java)](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml?query=branch%3Amain-java)
[![Python CI](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml/badge.svg?branch=main-python)](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/actions/workflows/ci.yml?query=branch%3Amain-python)

Laboratory works for the course "Cross-Platform Programming Tools",
Lviv Polytechnic National University. Author: Fedorchuk I. V., group KI-31.

**Variant 20 — gym memberships.** All labs develop one program: it reads gym memberships from
a CSV file, checks every record, computes the indicators of the variant and writes a report.
Labs 1–5 build it in Java, labs 6–7 build it again in Python.

## Branches

| Branch | Contents |
|---|---|
| `main` | this overview, no code |
| [`main-java`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/main-java) | Java labs 1–5: Maven, JUnit 5, SpotBugs, Java CI |
| [`main-python`](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/main-python) | Python labs 6–7: pyproject.toml, pytest, ruff, wheel, Python CI |

Every lab was made in its own branch from an Issue, merged by a pull request after green CI on
Ubuntu, Windows and macOS, and marked with a tag. The tags keep the exact state of every lab.

## Labs

| Lab | Topic | Code | Pull request | Report |
|---|---|---|---|---|
| 1 | Java project setup and basic data processing | [v1.0.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.0.0) | [#5](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/5) | [lab-01](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/blob/main-java/docs/reports/lab-01.md) |
| 2 | Classes, records and encapsulation | [v1.1.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.1.0) | [#6](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/6) | [lab-02](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/blob/main-java/docs/reports/lab-02.md) |
| 3 | Inheritance, interfaces and polymorphism | [v1.2.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.2.0) | [#12](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/12) | [lab-03](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/blob/main-java/docs/reports/lab-03.md) |
| — | Cleanup of labs 1–3 | [v1.2.1](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.2.1) | [#21](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/21) | — |
| 4 | Stream API | [v1.3.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.3.0) | [#28](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/28) | [lab-04](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/blob/main-java/docs/reports/lab-04.md) |
| 5 | Generic repository, annotations, reflection, CSV export | [v1.4.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/v1.4.0) | [#36](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/36) | [REPORT](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/blob/v1.4.0/REPORT.md) |
| 0 | Python project infrastructure | [py-v1.0.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/py-v1.0.0) | [#43](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/43) | — |
| 6 | Data processing: CSV, collections, comprehensions | [py-v1.1.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/py-v1.1.0) | [#50](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/50) | [REPORT](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/blob/py-v1.1.0/REPORT.md) |
| 7 | Object model: properties, dataclasses, subtypes, protocol | [py-v1.2.0](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/tree/py-v1.2.0) | [#57](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/pull/57) | [REPORT](https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1/blob/py-v1.2.0/REPORT.md) |

## Quick start

Java (JDK 21; the Maven Wrapper downloads Maven):

```bash
git clone https://github.com/anc1eeent/CPPT_Fedorchuk_IV_KI-31_1.git
cd CPPT_Fedorchuk_IV_KI-31_1
git switch main-java
./mvnw clean verify                      # Windows: .\mvnw.cmd clean verify
java -jar target/gym-membership-1.4.0.jar
```

Python (3.11 or newer):

```bash
git switch main-python
python -m venv .venv
.venv/bin/python -m pip install -e ".[dev]"   # Windows: .\.venv\Scripts\python.exe
.venv/bin/gym-membership data/input.csv reports/summary.txt
```

## Tags

`v1.x.y` — Java labs, `py-v1.x.y` — Python labs. The minor number grows with every lab,
the patch number marks a fix of a finished lab.

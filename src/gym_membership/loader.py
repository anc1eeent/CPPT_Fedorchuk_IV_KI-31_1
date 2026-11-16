"""Reading gym memberships from the CSV file of variant 20."""

from __future__ import annotations

import csv
from datetime import date
from pathlib import Path
from typing import Any, TypedDict

from .errors import DataError

REQUIRED_COLUMNS = ("customer", "kind", "start", "end", "visits")
"""Columns that every input file must have, in the documented order."""

KINDS = ("annual", "monthly")
"""Allowed membership kinds after normalisation."""


class Membership(TypedDict):
    """One membership. At run time it is an ordinary dictionary; the class only names its keys."""

    customer: str
    kind: str
    start: date
    end: date
    visits: int


def load_memberships(path: Path) -> list[Membership]:
    """Read and check all memberships of a UTF-8 CSV file.

    An empty file and a file with only the header give an empty list.

    Raises:
        DataError: the header has no required column or a row is invalid.
        OSError: the file cannot be opened.
    """
    with path.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        if reader.fieldnames is None:
            return []
        # A byte order mark or spaces around a name must not hide a column.
        reader.fieldnames = [name.strip().lstrip("﻿") for name in reader.fieldnames]
        missing = [name for name in REQUIRED_COLUMNS if name not in reader.fieldnames]
        if missing:
            raise DataError(f"missing columns: {', '.join(missing)}", line=1)
        # reader.line_num is the line where the current row ends, also for a quoted multiline value.
        return [parse_row(row, reader.line_num) for row in reader]


def parse_row(row: dict[Any, Any], line: int) -> Membership:
    """Convert one CSV row to a membership with typed values.

    Raises:
        DataError: a value is missing, cannot be converted or breaks a rule.
    """
    if None in row:
        raise DataError("too many values", line=line)

    customer = _required(row, "customer", line)
    kind = _required(row, "kind", line).lower()
    if kind not in KINDS:
        raise DataError(
            f"unknown kind '{kind}', expected one of {', '.join(KINDS)}", line=line, field="kind"
        )
    start = _parse_date(_required(row, "start", line), line, "start")
    end = _parse_date(_required(row, "end", line), line, "end")
    if end <= start:
        raise DataError(f"end {end} is not after start {start}", line=line, field="end")
    visits = _parse_visits(_required(row, "visits", line), line)

    return {"customer": customer, "kind": kind, "start": start, "end": end, "visits": visits}


def _required(row: dict[Any, Any], field: str, line: int) -> str:
    """Return the value of a field without surrounding spaces, or fail if it is empty."""
    value = row.get(field)
    if value is None or not value.strip():
        raise DataError("missing value", line=line, field=field)
    return value.strip()


def _parse_date(text: str, line: int, field: str) -> date:
    """Parse an ISO date; the ValueError of the standard library becomes the cause."""
    try:
        return date.fromisoformat(text)
    except ValueError as error:
        raise DataError(f"not a date (YYYY-MM-DD): '{text}'", line=line, field=field) from error


def _parse_visits(text: str, line: int) -> int:
    """Parse the number of visits: a whole number, zero or more."""
    try:
        visits = int(text)
    except ValueError as error:
        raise DataError(f"not a whole number: '{text}'", line=line, field="visits") from error
    if visits < 0:
        raise DataError(f"must not be negative, got {visits}", line=line, field="visits")
    return visits

"""Tests of reading memberships from CSV."""

from datetime import date
from pathlib import Path

import pytest

from gym_membership.errors import DataError
from gym_membership.loader import load_memberships
from gym_membership.models import AnnualMembership, MonthlyMembership

DATA = Path(__file__).resolve().parents[1] / "data" / "input.csv"
HEADER = "customer,kind,start,end,visits\n"


def write_csv(folder: Path, text: str) -> Path:
    """Write a CSV file in UTF-8 and return its path."""
    path = folder / "input.csv"
    path.write_text(text, encoding="utf-8", newline="")
    return path


def test_control_data_is_loaded_with_types() -> None:
    """The control file gives 8 records with converted and normalised values."""
    records = load_memberships(DATA)

    assert len(records) == 8
    assert records[0] == AnnualMembership("Іван Федорчук", date(2026, 1, 15), date(2027, 1, 15), 45)
    assert records[4].kind == "monthly"  # "Monthly" in the file
    assert records[7].visits == 0  # boundary value


def test_kind_column_chooses_the_class() -> None:
    """Every row becomes an object of the subtype named in the kind column."""
    kinds = [type(record) for record in load_memberships(DATA)]

    assert kinds == [AnnualMembership] * 3 + [MonthlyMembership] * 3 + [
        AnnualMembership,
        MonthlyMembership,
    ]


def test_comma_inside_quotes_stays_in_text() -> None:
    """A quoted customer name with a comma is one field."""
    assert load_memberships(DATA)[1].customer == "Коваль, Петро"


def test_empty_file_gives_no_records(tmp_path: Path) -> None:
    """An empty file is valid and has no records."""
    assert load_memberships(write_csv(tmp_path, "")) == []


def test_header_only_gives_no_records(tmp_path: Path) -> None:
    """A file with only the header is valid and has no records."""
    assert load_memberships(write_csv(tmp_path, HEADER)) == []


def test_header_with_byte_order_mark_is_accepted(tmp_path: Path) -> None:
    """A byte order mark written by a Windows editor does not hide the first column."""
    path = write_csv(tmp_path, "﻿" + HEADER + "Анна Литвин,monthly,2026-10-05,2026-11-05,12\n")

    assert load_memberships(path)[0].customer == "Анна Литвин"


def test_missing_column_is_reported_before_rows(tmp_path: Path) -> None:
    """A header without a required column is rejected with line 1."""
    path = write_csv(
        tmp_path, "customer,kind,start,end\nАнна Литвин,monthly,2026-10-05,2026-11-05\n"
    )

    with pytest.raises(DataError, match=r"^line 1: missing columns: visits$"):
        load_memberships(path)


@pytest.mark.parametrize(
    ("row", "message"),
    [
        (",monthly,2026-10-05,2026-11-05,12", r"^line 2, field 'customer': missing value$"),
        ("Анна Литвин,monthly,2026-10-05,2026-11-05,", r"^line 2, field 'visits': missing value$"),
        ("Анна Литвин,monthly,2026-10-05,2026-11-05", r"^line 2, field 'visits': missing value$"),
        ("Анна Литвин,weekly,2026-10-05,2026-11-05,12", r"^line 2, field 'kind': unknown kind"),
        ("Анна Литвин,monthly,2026-10-05,2026-11-05,12,extra", r"^line 2: too many values$"),
    ],
)
def test_invalid_row_names_line_and_field(tmp_path: Path, row: str, message: str) -> None:
    """Every rule of a row gives DataError with the line and the field."""
    path = write_csv(tmp_path, HEADER + row + "\n")

    with pytest.raises(DataError, match=message):
        load_memberships(path)


@pytest.mark.parametrize(
    ("row", "message"),
    [
        (
            "Анна Литвин,monthly,2026-10-05,2026-10-05,12",
            "line 2: end 2026-10-05 must be after start 2026-10-05",
        ),
        (
            "Анна Литвин,monthly,2026-10-05,2026-11-05,-1",
            "line 2: visits must be a whole number from 0, got -1",
        ),
    ],
)
def test_rule_of_the_class_becomes_data_error(tmp_path: Path, row: str, message: str) -> None:
    """The class checks its rules; the loader adds the line and keeps the ValueError."""
    path = write_csv(tmp_path, HEADER + row + "\n")

    with pytest.raises(DataError) as error:
        load_memberships(path)

    assert str(error.value) == message
    assert isinstance(error.value.__cause__, ValueError)


def test_bad_number_keeps_original_error(tmp_path: Path) -> None:
    """A bad number is reported with the line and keeps the ValueError as the cause."""
    path = write_csv(tmp_path, HEADER + "Анна Литвин,monthly,2026-10-05,2026-11-05,twelve\n")

    with pytest.raises(DataError) as error:
        load_memberships(path)

    assert str(error.value) == "line 2, field 'visits': not a whole number: 'twelve'"
    assert (error.value.line, error.value.field) == (2, "visits")
    assert isinstance(error.value.__cause__, ValueError)


def test_bad_date_keeps_original_error(tmp_path: Path) -> None:
    """A date outside ISO format is reported with the field and keeps the ValueError."""
    path = write_csv(tmp_path, HEADER + "Анна Литвин,monthly,05.10.2026,2026-11-05,12\n")

    with pytest.raises(DataError, match=r"^line 2, field 'start': not a date") as error:
        load_memberships(path)

    assert isinstance(error.value.__cause__, ValueError)


def test_line_number_counts_from_header(tmp_path: Path) -> None:
    """The third data row is line 4 of the file."""
    rows = (
        "Анна Литвин,monthly,2026-10-05,2026-11-05,12\n" * 2
        + "Олег Савчук,monthly,x,2026-11-20,12\n"
    )
    path = write_csv(tmp_path, HEADER + rows)

    with pytest.raises(DataError, match=r"^line 4, field 'start'"):
        load_memberships(path)


def test_input_file_is_not_changed() -> None:
    """Reading does not change the input file."""
    before = DATA.read_bytes()
    load_memberships(DATA)
    assert DATA.read_bytes() == before

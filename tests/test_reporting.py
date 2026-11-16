"""Tests of the text report."""

from datetime import date
from pathlib import Path

from gym_membership.loader import load_memberships
from gym_membership.reporting import format_line, format_report, write_report

DATA = Path(__file__).resolve().parents[1] / "data" / "input.csv"

EXPECTED = "".join(
    [
        "Gym membership report\n",
        "Reference date: 2026-11-01\n",
        "Records: 8\n",
        "Active memberships: 5\n",
        "Active customers: Іван Федорчук; Марія Іванівна; Олег Савчук; Анна Литвин; Ірина Бондар\n",
        "Visits by kind: annual=93, monthly=30\n",
        "Unique customers: 7\n",
        "Customers: Іван Федорчук; Ірина Бондар; Анна Литвин; Коваль, Петро; "
        "Марія Іванівна; Олег Савчук; Олена Петрівна\n",
        "Average visits: 15.38\n",
        "Leader: Іван Федорчук (45 visits)\n",
    ]
)


def test_format_line_lists_values_in_order() -> None:
    """*values are joined with the default joiner in the given order."""
    assert format_line("Records", 8) == "Records: 8"
    assert format_line("Kinds", "annual", "monthly") == "Kinds: annual, monthly"


def test_format_line_adds_keyword_pairs() -> None:
    """**pairs follow the values as key=value, in the order they were passed."""
    assert (
        format_line("Visits by kind", annual=93, monthly=30)
        == "Visits by kind: annual=93, monthly=30"
    )
    assert format_line("Leader", "Іван", visits=45) == "Leader: Іван, visits=45"


def test_format_line_with_another_joiner() -> None:
    """The default joiner can be replaced, e.g. for names that contain a comma."""
    assert (
        format_line("Customers", "Коваль, Петро", "Анна", joiner="; ")
        == "Customers: Коваль, Петро; Анна"
    )


def test_format_line_without_values() -> None:
    """A line without values reads 'none'."""
    assert format_line("Leader") == "Leader: none"


def test_report_of_control_data() -> None:
    """The control data give exactly the expected report."""
    assert format_report(load_memberships(DATA), date(2026, 11, 1)) == EXPECTED


def test_report_is_stable() -> None:
    """The same records give the same text every time."""
    records = load_memberships(DATA)

    assert format_report(records, date(2026, 11, 1)) == format_report(
        list(records), date(2026, 11, 1)
    )


def test_report_with_empty_selection() -> None:
    """No membership is active on 2025-01-01; the other lines do not change."""
    text = format_report(load_memberships(DATA), date(2025, 1, 1))

    assert "Active memberships: 0\nActive customers: none\n" in text
    assert "Leader: Іван Федорчук (45 visits)" in text


def test_report_of_no_records() -> None:
    """An empty input gives zeros and 'none' instead of failing."""
    assert format_report([], date(2026, 11, 1)) == (
        "Gym membership report\n"
        "Reference date: 2026-11-01\n"
        "Records: 0\n"
        "Active memberships: 0\n"
        "Active customers: none\n"
        "Visits by kind: none\n"
        "Unique customers: 0\n"
        "Customers: none\n"
        "Average visits: 0.00\n"
        "Leader: none\n"
    )


def test_write_report_uses_utf8_and_lf(tmp_path: Path) -> None:
    """The report file is UTF-8 with LF line ends; a missing folder is created."""
    path = tmp_path / "reports" / "summary.txt"

    write_report(path, EXPECTED)

    assert path.read_bytes() == EXPECTED.encode("utf-8")
    assert b"\r\n" not in path.read_bytes()


def test_write_report_replaces_old_file(tmp_path: Path) -> None:
    """A second report replaces the first one completely."""
    path = tmp_path / "summary.txt"
    write_report(path, "old report with more text\n")

    write_report(path, "new\n")

    assert path.read_text(encoding="utf-8") == "new\n"

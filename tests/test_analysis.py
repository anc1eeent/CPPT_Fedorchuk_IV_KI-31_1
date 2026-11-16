"""Tests of the analysis functions. They describe what analysis.py must do."""

import copy
from datetime import date
from pathlib import Path

from gym_membership.analysis import (
    active_on,
    average_visits,
    leader,
    unique_customers,
    visits_by_kind,
)
from gym_membership.loader import Membership, load_memberships

DATA = Path(__file__).resolve().parents[1] / "data" / "input.csv"
REFERENCE_DATE = date(2026, 11, 1)


def records() -> list[Membership]:
    """Control data of variant 20."""
    return load_memberships(DATA)


def membership(
    customer: str, visits: int, start: str = "2026-10-01", end: str = "2026-11-01"
) -> Membership:
    """Build one membership for a small test."""
    return {
        "customer": customer,
        "kind": "monthly",
        "start": date.fromisoformat(start),
        "end": date.fromisoformat(end),
        "visits": visits,
    }


def test_active_on_reference_date_keeps_file_order() -> None:
    """Five memberships are active on 2026-11-01, in the order of the file."""
    names = [record["customer"] for record in active_on(records(), REFERENCE_DATE)]

    assert names == [
        "Іван Федорчук",
        "Марія Іванівна",
        "Олег Савчук",
        "Анна Литвин",
        "Ірина Бондар",
    ]


def test_active_on_includes_start_and_excludes_end() -> None:
    """The first day counts, the end day does not."""
    starts = membership("Starts", 1, start="2026-11-01", end="2026-12-01")
    ends = membership("Ends", 1, start="2026-10-01", end="2026-11-01")

    assert active_on([starts, ends], REFERENCE_DATE) == [starts]


def test_active_on_can_select_nothing() -> None:
    """No membership is active on 2025-01-01: the selection is empty."""
    assert active_on(records(), date(2025, 1, 1)) == []


def test_visits_by_kind_sums_in_sorted_order() -> None:
    """Visits are summed per kind; the keys are sorted."""
    result = visits_by_kind(records())

    assert result == {"annual": 93, "monthly": 30}
    assert list(result) == ["annual", "monthly"]


def test_visits_by_kind_of_empty_list() -> None:
    """No records give an empty dictionary."""
    assert visits_by_kind([]) == {}


def test_unique_customers_removes_duplicates() -> None:
    """Марія Іванівна has two memberships but is one customer."""
    customers = unique_customers(records())

    assert isinstance(customers, set)
    assert len(customers) == 7
    assert {"Марія Іванівна", "Коваль, Петро"} <= customers


def test_average_visits() -> None:
    """123 visits in 8 memberships, and 0.0 when there are none."""
    assert average_visits(records()) == 123 / 8
    assert average_visits([]) == 0.0


def test_leader_has_most_visits() -> None:
    """Іван Федорчук has the most visits; there is no leader without records."""
    top = leader(records())

    assert top is not None
    assert (top["customer"], top["visits"]) == ("Іван Федорчук", 45)
    assert leader([]) is None


def test_leader_tie_is_resolved_by_name() -> None:
    """With equal visits the alphabetically first customer wins."""
    oleh = membership("Олег Савчук", 12)
    anna = membership("Анна Литвин", 12)

    assert leader([oleh, anna]) == anna


def test_functions_do_not_change_input() -> None:
    """Analysis only reads the records."""
    data = records()
    before = copy.deepcopy(data)

    active_on(data, REFERENCE_DATE)
    visits_by_kind(data)
    unique_customers(data)
    average_visits(data)
    leader(data)

    assert data == before

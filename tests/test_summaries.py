"""Tests for the subtypes, the VisitCostSource protocol and ExpirySummary."""

import dataclasses
from datetime import date, timedelta

import pytest

from gym_membership.models import AnnualMembership, Membership, MonthlyMembership
from gym_membership.summaries import (
    EXPIRY_WINDOW,
    ExpirySummary,
    average_visit_cost,
    most_expensive_visit,
    summarize_expiry,
)

REFERENCE = date(2026, 11, 1)

# The same eight memberships as data/input.csv.
RECORDS = [
    AnnualMembership("Іван Федорчук", date(2026, 1, 15), date(2027, 1, 15), 45),
    AnnualMembership("Коваль, Петро", date(2026, 8, 1), date(2026, 11, 1), 10),
    AnnualMembership("Марія Іванівна", date(2026, 6, 10), date(2026, 12, 10), 20),
    MonthlyMembership("Олег Савчук", date(2026, 10, 20), date(2026, 11, 20), 12),
    MonthlyMembership("Анна Литвин", date(2026, 10, 5), date(2026, 11, 5), 12),
    MonthlyMembership("Ірина Бондар", date(2026, 11, 1), date(2026, 12, 1), 6),
    AnnualMembership("Марія Іванівна", date(2025, 12, 1), date(2026, 6, 1), 18),
    MonthlyMembership("Олена Петрівна", date(2026, 11, 2), date(2026, 12, 2), 0),
]


class Priced:
    """Not a Membership: has only what the protocol asks for."""

    def __init__(self, customer: str, cost: float) -> None:
        self.customer = customer
        self.cost = cost

    def visit_cost(self) -> float:
        return self.cost


# --- subtypes ---------------------------------------------------------------


def test_subtypes_are_memberships_with_their_kind():
    monthly, annual = RECORDS[3], RECORDS[0]
    assert isinstance(monthly, Membership) and monthly.kind == "monthly"
    assert isinstance(annual, Membership) and annual.kind == "annual"


def test_monthly_cost_is_price_of_months_per_visit():
    two_months = MonthlyMembership("A", date(2026, 1, 1), date(2026, 3, 1), 30)
    assert two_months.visit_cost() == pytest.approx(600.0 * 2 / 30)


def test_annual_cost_is_contract_price_per_visit():
    assert RECORDS[0].visit_cost() == pytest.approx(120.0)


@pytest.mark.parametrize("kind", [MonthlyMembership, AnnualMembership])
def test_zero_visits_count_as_one(kind):
    record = kind("A", date(2026, 1, 1), date(2026, 2, 1), 0)
    assert record.visit_cost() == pytest.approx(kind("A", record.start, record.end, 1).visit_cost())


def test_visit_cost_of_every_input_record():
    costs = [round(record.visit_cost(), 2) for record in RECORDS]
    assert costs == [120.0, 540.0, 270.0, 50.0, 50.0, 100.0, 300.0, 600.0]


def test_subtypes_with_same_values_are_not_equal():
    values = ("A", date(2026, 1, 1), date(2026, 2, 1), 5)
    assert MonthlyMembership(*values) != AnnualMembership(*values)
    assert MonthlyMembership(*values) == MonthlyMembership(*values)


def test_repr_and_str_use_the_subtype():
    record = RECORDS[3]
    assert repr(record).startswith("MonthlyMembership(customer='Олег Савчук'")
    assert str(record) == "Олег Савчук: monthly, 2026-10-20 to 2026-11-20, 12 visits"


# --- protocol ---------------------------------------------------------------


def test_average_visit_cost():
    assert average_visit_cost(RECORDS) == pytest.approx(253.75)


def test_average_visit_cost_of_nothing_is_zero():
    assert average_visit_cost([]) == 0.0


def test_most_expensive_visit():
    assert most_expensive_visit(RECORDS) is RECORDS[7]


def test_most_expensive_visit_tie_goes_to_smaller_name():
    assert most_expensive_visit([Priced("Б", 10), Priced("А", 10)]).customer == "А"


def test_most_expensive_visit_of_nothing_is_none():
    assert most_expensive_visit([]) is None


def test_protocol_accepts_objects_that_are_not_memberships():
    items = [Priced("X", 30.0), RECORDS[3]]
    assert average_visit_cost(items) == pytest.approx(40.0)
    assert most_expensive_visit(items).customer == "Олег Савчук"


def test_functions_accept_any_iterable():
    assert average_visit_cost(record for record in RECORDS) == pytest.approx(253.75)


# --- dataclass --------------------------------------------------------------


def test_summarize_expiry_on_input_data():
    assert summarize_expiry(RECORDS, REFERENCE) == ExpirySummary(
        reference_date=REFERENCE, expired=2, ending_soon=3, next_end=date(2026, 11, 5)
    )


def test_window_end_is_inclusive():
    record = MonthlyMembership("A", REFERENCE, REFERENCE + EXPIRY_WINDOW, 1)
    assert summarize_expiry([record], REFERENCE).ending_soon == 1


def test_custom_window():
    assert summarize_expiry(RECORDS, REFERENCE, window=timedelta(days=5)).ending_soon == 1


def test_summarize_expiry_of_nothing():
    assert summarize_expiry([], REFERENCE) == ExpirySummary(REFERENCE, 0, 0, None)


def test_expiry_summary_is_frozen():
    summary = summarize_expiry(RECORDS, REFERENCE)
    with pytest.raises(dataclasses.FrozenInstanceError):
        summary.expired = 0


def test_expiry_summary_has_generated_repr_and_hash():
    summary = ExpirySummary(REFERENCE, 1, 2, None)
    assert repr(summary) == (
        "ExpirySummary(reference_date=datetime.date(2026, 11, 1), "
        "expired=1, ending_soon=2, next_end=None)"
    )
    assert len({summary, ExpirySummary(REFERENCE, 1, 2, None)}) == 1

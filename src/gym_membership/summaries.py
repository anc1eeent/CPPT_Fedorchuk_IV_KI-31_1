"""Summaries built on top of the object model: visit costs and expiry dates."""

from __future__ import annotations

from collections.abc import Iterable
from dataclasses import dataclass
from datetime import date, timedelta
from typing import Protocol

from gym_membership.models import Membership

EXPIRY_WINDOW = timedelta(days=30)
"""A membership ends soon when its end is at most this far from the reference date."""


class VisitCostSource(Protocol):
    """Anything with a customer name and a visit price.

    Membership objects match this protocol without inheriting from it: Python
    checks only that the attribute and the method exist (duck typing).
    """

    @property
    def customer(self) -> str: ...

    def visit_cost(self) -> float: ...


def average_visit_cost(items: Iterable[VisitCostSource]) -> float:
    """Return the average price of a visit, or 0.0 when there are no items."""
    costs = [item.visit_cost() for item in items]
    return sum(costs) / len(costs) if costs else 0.0


def most_expensive_visit(items: Iterable[VisitCostSource]) -> VisitCostSource | None:
    """Return the item with the highest visit price; a tie goes to the smaller name."""
    return min(items, key=lambda item: (-item.visit_cost(), item.customer), default=None)


@dataclass(frozen=True)
class ExpirySummary:
    """How many memberships ended or end soon on a reference date (immutable)."""

    reference_date: date
    expired: int
    ending_soon: int
    next_end: date | None


def summarize_expiry(
    records: Iterable[Membership],
    reference_date: date,
    window: timedelta = EXPIRY_WINDOW,
) -> ExpirySummary:
    """Count expired memberships and active ones that end within the window."""
    records = list(records)
    active_ends = [record.end for record in records if record.is_active_on(reference_date)]
    return ExpirySummary(
        reference_date=reference_date,
        expired=sum(1 for record in records if record.end <= reference_date),
        ending_soon=sum(1 for end in active_ends if end - reference_date <= window),
        next_end=min(active_ends, default=None),
    )

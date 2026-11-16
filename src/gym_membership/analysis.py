"""Indicators of variant 20 computed with lists, dictionaries, sets and comprehensions."""

from __future__ import annotations

from datetime import date

from .loader import Membership


def active_on(records: list[Membership], reference_date: date) -> list[Membership]:
    """Return the memberships active on the date: start <= date < end, in input order."""
    return [record for record in records if record.start <= reference_date < record.end]


def visits_by_kind(records: list[Membership]) -> dict[str, int]:
    """Return the total visits per kind, with the kinds in sorted order."""
    kinds = sorted({record.kind for record in records})
    return {kind: sum(record.visits for record in records if record.kind == kind) for kind in kinds}


def unique_customers(records: list[Membership]) -> set[str]:
    """Return the names of all customers without duplicates."""
    return {record.customer for record in records}


def average_visits(records: list[Membership]) -> float:
    """Return the average number of visits, or 0.0 when there are no records."""
    return sum(record.visits for record in records) / len(records) if records else 0.0


def leader(records: list[Membership]) -> Membership | None:
    """Return the record with the most visits; a tie goes to the alphabetically first customer."""
    return min(records, key=lambda record: (-record.visits, record.customer), default=None)

#!/usr/bin/env python3
"""Import customers from a CSV file into the AgentFlow REST API."""

import argparse
import csv
import re
import sys
from pathlib import Path

import requests


EMAIL_PATTERN = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


def validate_customer(row, row_number):
    """Validate and transform one CSV row into the API request shape."""
    values = {
        key: (row.get(key) or "").strip()
        for key in ("first_name", "last_name", "email", "phone")
    }
    for field in ("first_name", "last_name", "email"):
        if not values[field]:
            raise ValueError(f"row {row_number}: {field} is required")
    if not EMAIL_PATTERN.fullmatch(values["email"]):
        raise ValueError(f"row {row_number}: email is invalid")

    return {
        "firstName": values["first_name"],
        "lastName": values["last_name"],
        "email": values["email"].lower(),
        "phone": values["phone"] or None,
    }


def response_error(response):
    """Return a concise error from a ProblemDetail or plain HTTP response."""
    try:
        body = response.json()
    except (ValueError, requests.exceptions.JSONDecodeError):
        body = {}
    title = body.get("title")
    detail = body.get("detail")
    if title and detail:
        return f"{title} - {detail}"
    return title or detail or response.text.strip() or "request failed"


def import_customers(csv_path, base_url, session=None):
    """Import all valid rows, continuing after validation and HTTP errors."""
    client = session or requests.Session()
    endpoint = f"{base_url.rstrip('/')}/api/customers"
    total = 0
    imported = 0
    errors = []

    with Path(csv_path).open(newline="", encoding="utf-8-sig") as csv_file:
        for row_number, row in enumerate(csv.DictReader(csv_file), start=2):
            total += 1
            try:
                customer = validate_customer(row, row_number)
                response = client.post(endpoint, json=customer, timeout=10)
                if response.status_code < 200 or response.status_code >= 300:
                    errors.append(
                        f"row {row_number}: API returned {response.status_code}: "
                        f"{response_error(response)}"
                    )
                    continue
                imported += 1
            except ValueError as error:
                errors.append(str(error))
            except requests.RequestException as error:
                errors.append(f"row {row_number}: request failed: {error}")

    return total, imported, errors


def build_parser():
    parser = argparse.ArgumentParser(
        description="Import customers from CSV into AgentFlow."
    )
    parser.add_argument("csv_file", type=Path)
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="AgentFlow API base URL (default: %(default)s)",
    )
    return parser


def main(argv=None, session=None):
    args = build_parser().parse_args(argv)
    try:
        total, imported, errors = import_customers(
            args.csv_file, args.base_url, session=session
        )
    except (OSError, csv.Error) as error:
        print(f"Could not read {args.csv_file}: {error}", file=sys.stderr)
        return 1

    print(f"Imported {imported} of {total} customers")
    for error in errors:
        print(error, file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())

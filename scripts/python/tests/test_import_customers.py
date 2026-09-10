import importlib.util
from pathlib import Path

import pytest


MODULE_PATH = Path(__file__).parents[1] / "import_customers.py"


def load_importer():
    spec = importlib.util.spec_from_file_location("import_customers", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FakeResponse:
    def __init__(self, status_code, body=None, text=""):
        self.status_code = status_code
        self._body = body or {}
        self.text = text

    def json(self):
        return self._body


class FakeSession:
    def __init__(self, responses):
        self.responses = iter(responses)
        self.posts = []

    def post(self, url, json, timeout):
        self.posts.append({"url": url, "json": json, "timeout": timeout})
        return next(self.responses)


def test_validate_customer_trims_and_transforms_csv_fields():
    importer = load_importer()

    customer = importer.validate_customer(
        {
            "first_name": "  Ada ",
            "last_name": " Lovelace  ",
            "email": " ADA@example.com ",
            "phone": " +44 20 7946 0958 ",
        },
        row_number=2,
    )

    assert customer == {
        "firstName": "Ada",
        "lastName": "Lovelace",
        "email": "ada@example.com",
        "phone": "+44 20 7946 0958",
    }


@pytest.mark.parametrize(
    ("row", "message"),
    [
        (
            {"first_name": "", "last_name": "Lovelace", "email": "ada@example.com"},
            "row 2: first_name is required",
        ),
        (
            {"first_name": "Ada", "last_name": "Lovelace", "email": "not-an-email"},
            "row 2: email is invalid",
        ),
    ],
)
def test_validate_customer_rejects_invalid_rows(row, message):
    importer = load_importer()

    with pytest.raises(ValueError, match=message):
        importer.validate_customer(row, row_number=2)


def test_import_continues_after_http_error_and_main_returns_one(tmp_path, capsys):
    importer = load_importer()
    csv_path = tmp_path / "customers.csv"
    csv_path.write_text(
        "first_name,last_name,email,phone\n"
        "Ada,Lovelace,ada@example.com,+442079460958\n"
        "Grace,Hopper,grace@example.com,\n",
        encoding="utf-8",
    )
    session = FakeSession(
        [
            FakeResponse(201, {"id": "customer-1"}),
            FakeResponse(
                409,
                {"title": "Duplicate email", "detail": "Email already exists"},
            ),
        ]
    )

    exit_code = importer.main(
        [str(csv_path), "--base-url", "http://localhost:8080/"],
        session=session,
    )

    assert exit_code == 1
    assert len(session.posts) == 2
    assert session.posts[0] == {
        "url": "http://localhost:8080/api/customers",
        "json": {
            "firstName": "Ada",
            "lastName": "Lovelace",
            "email": "ada@example.com",
            "phone": "+442079460958",
        },
        "timeout": 10,
    }
    output = capsys.readouterr()
    assert "Imported 1 of 2 customers" in output.out
    assert "row 3: API returned 409: Duplicate email - Email already exists" in output.err

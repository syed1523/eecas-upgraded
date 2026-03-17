import json
import uuid
from datetime import date
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
EXPENSE_FILE = BASE_DIR / "data" / "expenses.json"
BUDGET_FILE = BASE_DIR / "data" / "budgets.json"


def load_expenses():
    try:
        with open(EXPENSE_FILE) as f:
            content = f.read().strip()
            return json.loads(content) if content else []
    except FileNotFoundError:
        return []


def save_expenses(expenses):
    with open(EXPENSE_FILE, "w") as f:
        json.dump(expenses, f, indent=2)


def submit_expense():
    expense = {
        "id": str(uuid.uuid4()),
        "employee_id": input("Employee ID: "),
        "amount": float(input("Amount: ")),
        "category": input("Category: "),
        "date": date.today().isoformat(),
        "receipt_path": input("Receipt path (optional): ").strip() or None,
        "status": "pending",
        "fraud_score": 0.0
    }

    expenses = load_expenses()
    expenses.append(expense)
    save_expenses(expenses)

    print("✅ Expense submitted")
    print("ID:", expense["id"])


def view_budgets():
    try:
        with open(BUDGET_FILE) as f:
            budgets = json.load(f)
    except FileNotFoundError:
        print("No budgets found.")
        return

    print("\n💰 BUDGET STATUS")
    for cat, data in budgets.items():
        remaining = data["limit"] - data["used"]
        print(
            f"{cat.upper()}: Used {data['used']:.2f} / "
            f"Limit {data['limit']:.2f} | Remaining {remaining:.2f}"
        )


if __name__ == "__main__":
    print("""
Choose an action:
1. Submit expense
2. View budgets
""")

    choice = input("Enter choice: ").strip()

    if choice == "1":
        submit_expense()
    elif choice == "2":
        view_budgets()
    else:
        print("Invalid choice")

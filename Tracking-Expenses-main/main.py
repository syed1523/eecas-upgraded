
import json
import time
from pathlib import Path

from agents.receipt_scanner import ReceiptScanner
from agents.policy_checker import PolicyChecker
from agents.fraud_detector import FraudDetector
from agents.approval_router import ApprovalRouter
from agents.accounting import AccountingSystem

BASE_DIR = Path(__file__).resolve().parent
EXPENSE_FILE = BASE_DIR / "data" / "expenses.json"
BUDGET_FILE = BASE_DIR / "data" / "budgets.json"

def load_json(path):
    try:
        with open(path) as f:
            content = f.read().strip()
            return json.loads(content) if content else []
    except FileNotFoundError:
        return []


def save_json(path, data):
    with open(path, "w") as f:
        json.dump(data, f, indent=2)


def load_budgets():
    try:
        with open(BUDGET_FILE) as f:
            return json.load(f)
    except FileNotFoundError:
        return {}


def save_budgets(budgets):
    with open(BUDGET_FILE, "w") as f:
        json.dump(budgets, f, indent=2)


def main():
    scanner = ReceiptScanner()
    policy = PolicyChecker()
    fraud = FraudDetector()
    router = ApprovalRouter()
    accounting = AccountingSystem()

    expenses = load_json(EXPENSE_FILE)
    budgets = load_budgets()

    processed = 0
    auto_approved = 0
    start = time.time()

    for expense in expenses:
        if expense.get("status") != "pending":
            continue

        processed += 1

        scan_result = scanner.scan(expense.get("receipt_path"))
        violations = policy.check(expense, scan_result)
        fraud_score = fraud.score(expense)

        decision = router.route(violations, fraud_score)

        expense["violations"] = violations
        expense["fraud_score"] = fraud_score
        expense["status"] = decision

        if decision == "approved":
            auto_approved += 1
            accounting.sync(expense)

            category = expense["category"]
            amount = expense.get("amount")

            if category in budgets and isinstance(amount, (int, float)):
                budgets[category]["used"] += amount

                if budgets[category]["used"] > budgets[category]["limit"]:
                    print(f"⚠ BUDGET EXCEEDED for {category.upper()}!")

    save_json(EXPENSE_FILE, expenses)
    save_budgets(budgets)

    end = time.time()
    total = len(expenses)

    print("\nFINAL RESULT")
    print(f"Processed expenses: {processed}")
    print(f"Auto-approved: {auto_approved}")
    print(f"Policy compliance rate: {(auto_approved / total) * 100 if total else 0:.0f}%")
    print(f"Avg processing time: {((end - start) / processed) * 1000 if processed else 0:.1f} ms")


if __name__ == "__main__":
    main()

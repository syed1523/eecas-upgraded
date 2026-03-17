import json
from pathlib import Path
import streamlit as st
from collections import Counter
from datetime import datetime

BASE_DIR = Path(__file__).resolve().parent
EXPENSE_FILE = BASE_DIR / "data" / "expenses.json"
BUDGET_FILE = BASE_DIR / "data" / "budgets.json"
AUDIT_FILE = BASE_DIR / "data" / "audit_log.json"


def load_json(path, default):
    try:
        with open(path) as f:
            content = f.read().strip()
            return json.loads(content) if content else default
    except FileNotFoundError:
        return default


expenses = load_json(EXPENSE_FILE, [])
budgets = load_json(BUDGET_FILE, {})
audit_logs = load_json(AUDIT_FILE, [])

st.set_page_config(page_title="Expense Dashboard", layout="wide")

st.title("📊 Expense Management Dashboard")

# ---------- METRICS ----------

statuses = Counter(e["status"] for e in expenses)

total = len(expenses)
approved = statuses.get("approved", 0)
compliance_rate = (approved / total * 100) if total else 0

col1, col2, col3 = st.columns(3)
col1.metric("Total Expenses", total)
col2.metric("Approved", approved)
col3.metric("Compliance Rate", f"{compliance_rate:.0f}%")

st.divider()

# ---------- STATUS BREAKDOWN ----------

st.subheader("📌 Expense Status Breakdown")
st.bar_chart(statuses)

# ---------- MONTHLY SPEND ----------

st.subheader("💰 Monthly Spend")

monthly = {}
for e in expenses:
    if isinstance(e.get("amount"), (int, float)):
        month = e["date"][:7]
        monthly[month] = monthly.get(month, 0) + e["amount"]

st.line_chart(monthly)

# ---------- BUDGET USAGE ----------

st.subheader("💸 Budget Usage")

if budgets:
    budget_data = {
        cat: data["used"] / data["limit"]
        for cat, data in budgets.items()
        if data["limit"] > 0
    }
    st.bar_chart(budget_data)
else:
    st.info("No budgets configured")

# ---------- RECENT EXPENSES ----------

st.subheader("🧾 Recent Expenses")

recent = sorted(
    expenses,
    key=lambda x: x["date"],
    reverse=True
)[:5]

st.table([
    {
        "ID": e["id"],
        "Employee": e["employee_id"],
        "Category": e["category"],
        "Amount": e["amount"],
        "Status": e["status"]
    }
    for e in recent
])

# ---------- AUDIT LOG ----------

st.subheader("📜 Audit Trail")

recent_logs = sorted(
    audit_logs,
    key=lambda x: x["timestamp"],
    reverse=True
)[:5]

st.table([
    {
        "Expense ID": log["expense_id"],
        "Action": log["action"],
        "Actor": log["actor"],
        "Time": log["timestamp"]
    }
    for log in recent_logs
])

📘 Expense Management System

An end-to-end expense management system that automates receipt processing, policy enforcement, approvals, budgeting, and reporting.
The system combines rule-based compliance checks, fraud scoring, human approvals, and a real-time dashboard to simulate enterprise-grade expense workflows.

🚀 Features
Expense submission via CLI
OCR-based receipt scanning (optional)
Policy violation detection
Fraud risk scoring
Auto-approval for compliant expenses
Manager & finance approval workflow
Budget tracking with overspend alerts
Monthly expense reports & summaries
Full audit trail for approvals
Real-time dashboard for visibility

🧱 System Architecture
Core Components
submit_expense.py – Expense creation & user actions
main.py – Processing pipeline (policies, fraud, approvals, budgets)
agents/ – Modular business logic (scanner, policy, fraud, routing, accounting)
data/ – JSON-based persistence layer
dashboard.py – Streamlit-based management dashboard

📁 Project Structure
expense-tracker/
│
├── main.py
├── submit_expense.py
├── approve_expense.py
├── dashboard.py
│
├── agents/
│   ├── receipt_scanner.py
│   ├── policy_checker.py
│   ├── fraud_detector.py
│   ├── approval_router.py
│   ├── accounting.py
│   └── audit_logger.py
│
└── data/
    ├── expenses.json
    ├── budgets.json
    └── audit_log.json

⚙️ Installation & Setup
1️⃣ Create virtual environment
python -m venv venv
source venv/bin/activate   # macOS/Linux
venv\Scripts\activate      # Windows

2️⃣ Install dependencies
pip install streamlit pytesseract pillow
(OCR is optional — system works without it)

▶️ How to Run
Submit an expense
python submit_expense.py

Process expenses (policies, fraud, approvals, budgets)
python main.py

Approve / reject expenses (manager or finance)
python approve_expense.py

Launch dashboard
streamlit run dashboard.py

📊 Dashboard Metrics
Total expenses
Approval & compliance rate
Expense status distribution
Monthly spend trends
Budget utilization
Recent expenses
Audit trail

🛡️ Policy Rules (Sample)
Receipts required for meals & travel
High-value expenses flagged for review
Fraud score influences approval routing
(Policies are configurable in policy_checker.py)

💰 Budget Tracking
Budgets defined per category
Budget usage updated only on approval
Overspend warnings triggered automatically
Configured in:
data/budgets.json

📜 Audit Logging
Every approval or system decision is logged with:
Expense ID
Action taken
Actor (system / manager / finance)
Timestamp
Stored in:
data/audit_log.json

🧠 Design Principles
Modular agent-based architecture
Idempotent processing
Human-in-the-loop approvals
Production-style error handling
Separation of business logic & UI

🔮 Future Enhancements
REST API (FastAPI)
Role-based access control
ML-based fraud anomaly detection
Receipt amount extraction
Cloud deployment

👤 Author
Built as a hands-on project to understand real-world expense systems, financial compliance, and backend architecture.

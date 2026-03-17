class BudgetMonitorAgent:
    def update(self, expense, budget):
        budget["used"] += expense["amount"]
        if budget["used"] > budget["limit"]:
            print("⚠ Budget exceeded!")

class PolicyChecker:
    def check(self, expense, scan_result):
        """
        Check expense against company policies.
        Returns a list of violations.
        """
        violations = []

        amount = expense.get("amount")
        category = expense.get("category")
        receipt = expense.get("receipt_path")

        if amount is None:
            violations.append("Missing amount")

        if category in ["meals", "travel"] and not receipt:
            violations.append("Receipt required")

        if amount and amount > 1000:
            violations.append("Amount exceeds approval threshold")

        return violations

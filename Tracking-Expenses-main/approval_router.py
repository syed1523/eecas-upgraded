class ApprovalRouter:
    def route(self, violations, fraud_score):
        """
        Decide approval path based on violations and fraud score.
        """
        if violations:
            return "manager_review"

        if fraud_score >= 0.5:
            return "finance_review"

        return "approved"

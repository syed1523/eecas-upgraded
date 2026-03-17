class FraudDetector:
    def score(self, expense):
        """
        Return a fraud risk score between 0.0 and 1.0
        """
        amount = expense.get("amount", 0)

        if amount > 500:
            return 0.6
        elif amount > 200:
            return 0.3
        return 0.0

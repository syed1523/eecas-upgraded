import numpy as np
from sklearn.cluster import KMeans

class PolicyCompressor:
    def compress(self, policies, ratio=0.75):
        vectors = []
        for p in policies:
            vectors.append([
                hash(p["category"]) % 1000 / 1000,
                p["max_amount"] / 1000,
                int(p["requires_receipt"])
            ])

        n_clusters = max(1, int(len(policies) * (1 - ratio)))
        kmeans = KMeans(n_clusters=n_clusters, n_init=10)
        labels = kmeans.fit_predict(vectors)

        clusters = {}
        for i, label in enumerate(labels):
            clusters.setdefault(label, []).append(policies[i])

        return clusters

import { useState } from "react";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";

const CHIPS = [
  "Show all rejected expenses",
  "List all pending approval expenses",
  "Show high risk expenses",
  "List flagged anomalies",
  "Show approved expenses above 10000",
  "Show all Marketing department expenses",
  "Show paid expenses this month",
  "Show approved with override expenses",
  "List anomalies with score above 3",
];

const ALLOWED_ROLES = ["FINANCE", "MANAGER", "AUDITOR"];

const normalizeRole = (role) => role?.replace(/^ROLE_/, "");

const formatAmount = (amount) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(amount || 0));

export default function NLQueryPanel({ role: roleProp = null }) {
  const { user } = useAuth();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const userRoles = user?.roles?.map(normalizeRole) || [];
  const fallbackRole = userRoles.find((role) => ALLOWED_ROLES.includes(role)) || null;
  const resolvedRole = roleProp && userRoles.includes(roleProp) ? roleProp : fallbackRole;
  const canUseNLQuery = ALLOWED_ROLES.includes(resolvedRole);

  const handleSubmit = async () => {
    if (!query.trim() || !canUseNLQuery) return;

    setLoading(true);
    setError("");
    setResults([]);

    try {
      const res = await api.post("/audit/nl-query", {
        query,
        role: resolvedRole,
      });
      setResults(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      const errData = err.response?.data;
      const errMessage =
        typeof errData === "string" ? errData : errData?.message;

      if (
        (err.response?.status === 403 || /access denied/i.test(errMessage || "")) &&
        resolvedRole === "MANAGER"
      ) {
        setError("Manager NL Query access is blocked by the running backend. Restart the Spring Boot server so the updated manager permission is loaded.");
        return;
      }
      if (typeof errData === "string") {
        setError(errData);
      } else if (errData?.message) {
        setError(errData.message);
      } else {
        setError("Query failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  if (!canUseNLQuery) {
    return null;
  }

  return (
    <div className="space-y-4">
      <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
        <h2 className="text-2xl font-bold text-white mb-1">Natural Language Query</h2>
        <p className="text-sm text-white/50 mb-4">Ask questions about expenses in plain English</p>
        <textarea
          rows={3}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="e.g. Show all rejected expenses in Marketing"
          className="w-full bg-white/5 border border-white/10 rounded-xl p-3 text-white placeholder-white/30 focus:outline-none focus:border-cyan-500 resize-none mb-3"
        />
        <div className="flex flex-wrap gap-2 mb-4">
          {CHIPS.map((chip) => (
            <button
              key={chip}
              onClick={() => setQuery(chip)}
              className="px-3 py-1 bg-cyan-500/10 border border-cyan-500/30 rounded-full text-cyan-400 text-xs cursor-pointer hover:bg-cyan-500/20 transition-colors"
            >
              {chip}
            </button>
          ))}
        </div>
        <button
          onClick={handleSubmit}
          disabled={loading}
          className="bg-cyan-500 hover:bg-cyan-600 disabled:opacity-50 text-white px-6 py-2 rounded-xl font-medium transition-colors"
        >
          {loading ? "Running..." : "Run Query"}
        </button>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 text-red-400">
          <p>{error}</p>
        </div>
      )}

      {results.length > 0 && (
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
          <p className="text-white/50 text-sm mb-4">{results.length} result(s) found</p>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/10">
                  <th className="text-left text-white/50 uppercase text-xs pb-3 pr-4">ID</th>
                  <th className="text-left text-white/50 uppercase text-xs pb-3 pr-4">Amount</th>
                  <th className="text-left text-white/50 uppercase text-xs pb-3 pr-4">Category</th>
                  <th className="text-left text-white/50 uppercase text-xs pb-3 pr-4">Status</th>
                  <th className="text-left text-white/50 uppercase text-xs pb-3 pr-4">Department</th>
                  <th className="text-left text-white/50 uppercase text-xs pb-3">Risk Level</th>
                </tr>
              </thead>
              <tbody>
                {results.map((expense) => (
                  <tr key={expense.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                    <td className="text-white/80 py-3 pr-4">#{expense.id}</td>
                    <td className="text-white py-3 pr-4 font-medium">{formatAmount(expense.amount)}</td>
                    <td className="text-white/80 py-3 pr-4">{expense.category}</td>
                    <td className="py-3 pr-4">
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-medium ${
                          expense.status === "APPROVED"
                            ? "bg-green-500/20 text-green-400"
                            : expense.status === "REJECTED"
                              ? "bg-red-500/20 text-red-400"
                              : expense.status?.startsWith("PENDING")
                                ? "bg-yellow-500/20 text-yellow-400"
                                : "bg-white/10 text-white/60"
                        }`}
                      >
                        {expense.status}
                      </span>
                    </td>
                    <td className="text-white/80 py-3 pr-4">{expense.departmentName || "-"}</td>
                    <td className="py-3">
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-medium ${
                          expense.riskLevel === "CRITICAL"
                            ? "bg-red-500/20 text-red-400"
                            : expense.riskLevel === "HIGH"
                              ? "bg-orange-500/20 text-orange-400"
                              : expense.riskLevel === "MEDIUM"
                                ? "bg-yellow-500/20 text-yellow-400"
                                : "bg-white/10 text-white/60"
                        }`}
                      >
                        {expense.riskLevel || "-"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!loading && results.length === 0 && !error && (
        <div className="text-center text-white/30 text-sm">
          Run a query to see results
        </div>
      )}
    </div>
  );
}

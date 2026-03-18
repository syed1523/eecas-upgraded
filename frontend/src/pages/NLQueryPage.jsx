import { useState } from "react";
import axios from "axios";

const CHIPS = [
  "Show all rejected expenses",
  "List anomalies with score above 3",
  "Show all Marketing department expenses",
  "List all pending approval expenses"
];

export default function NLQueryPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError("");
    setResults([]);
    try {
      const token = localStorage.getItem("token")
        || JSON.parse(localStorage.getItem("user") || "null")?.token;
      const res = await axios.post(
        "http://localhost:8081/api/audit/nl-query",
        { query },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setResults(res.data);
    } catch (err) {
      setError(err.response?.data || "Query failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 p-6">
      <div className="max-w-5xl mx-auto">
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 mb-6">
          <h1 className="text-2xl font-bold text-white mb-1">Natural Language Query</h1>
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
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 text-red-400 mb-4">
            {error}
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
                  {results.map((exp) => (
                    <tr key={exp.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                      <td className="text-white/80 py-3 pr-4">#{exp.id}</td>
                      <td className="text-white py-3 pr-4 font-medium">
                        ₹{Number(exp.amount).toLocaleString("en-IN")}
                      </td>
                      <td className="text-white/80 py-3 pr-4">{exp.category}</td>
                      <td className="py-3 pr-4">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          exp.status === "APPROVED" ? "bg-green-500/20 text-green-400" :
                          exp.status === "REJECTED" ? "bg-red-500/20 text-red-400" :
                          exp.status === "PENDING" ? "bg-yellow-500/20 text-yellow-400" :
                          "bg-white/10 text-white/60"
                        }`}>{exp.status}</span>
                      </td>
                      <td className="text-white/80 py-3 pr-4">{exp.departmentName || "—"}</td>
                      <td className="py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          exp.riskLevel === "CRITICAL" ? "bg-red-500/20 text-red-400" :
                          exp.riskLevel === "HIGH" ? "bg-orange-500/20 text-orange-400" :
                          exp.riskLevel === "MEDIUM" ? "bg-yellow-500/20 text-yellow-400" :
                          "bg-white/10 text-white/60"
                        }`}>{exp.riskLevel || "—"}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {!loading && results.length === 0 && !error && (
          <div className="text-center text-white/30 text-sm mt-8">
            Run a query to see results
          </div>
        )}
      </div>
    </div>
  );
}

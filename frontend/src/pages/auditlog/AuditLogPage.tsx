import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { auditLogApi } from "../../api/auditLog";
import type { AuditLogEntry } from "../../api/types";
import { ApiError } from "../../api/apiClient";

const ACTION_TONE: Record<string, string> = {
  CREATE: "oasis",
  UPDATE: "gold",
  APPROVE: "oasis",
  REJECT: "rust",
  CANCEL: "rust",
  STATUS_CHANGE: "amber",
  RECORD: "gold",
};

export function AuditLogPage() {
  const [entries, setEntries] = useState<AuditLogEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [username, setUsername] = useState("");
  const [entityType, setEntityType] = useState("");

  useEffect(() => {
    let cancelled = false;
    setEntries(null);
    setError(null);

    auditLogApi
      .getAll({ username: username || undefined, entityType: entityType || undefined })
      .then((data) => {
        if (!cancelled) setEntries(data);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "Something went wrong loading the audit log.");
      });

    return () => {
      cancelled = true;
    };
  }, [username, entityType]);

  return (
    <div>
      <Link to="/" className="page-breadcrumb">
        ← Main menu
      </Link>
      <h1 className="page-title-banner">Audit log</h1>

      <div className="filter-bar">
        <select value={entityType} onChange={(e) => setEntityType(e.target.value)}>
          <option value="">All entity types</option>
          <option value="Race">Race</option>
          <option value="Registration">Registration</option>
          <option value="Result">Result</option>
        </select>
        <input
          placeholder="Filter by username..."
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
      </div>

      {error ? (
        <div className="state-box state-box--error">
          <p>{error}</p>
        </div>
      ) : entries === null ? (
        <div className="state-box">
          <div className="spinner" />
          <p>Loading audit log...</p>
        </div>
      ) : entries.length === 0 ? (
        <div className="state-box">
          <p>No entries match these filters.</p>
        </div>
      ) : (
        <div className="sheet">
          {entries.map((entry, index) => (
            <div key={entry.id} className="sheet-row rise" style={{ animationDelay: `${index * 40}ms` }}>
              <span className={`sheet-row__stripe stripe-${ACTION_TONE[entry.action] ?? "stone"}`} />
              <div className="sheet-row__body">
                <div className="sheet-row__title">
                  {entry.entityType} — {entry.description ?? entry.action}
                </div>
                <div className="sheet-row__meta">
                  <span>{entry.username}</span>
                  <span>{new Date(entry.timestamp).toLocaleString()}</span>
                  {entry.oldValue && entry.newValue && (
                    <span>
                      {entry.oldValue} → {entry.newValue}
                    </span>
                  )}
                </div>
              </div>
              <span className={`sheet-row__status status-${ACTION_TONE[entry.action] ?? "stone"}`}>
                {entry.action}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
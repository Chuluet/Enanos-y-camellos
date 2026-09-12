import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { competitorsApi } from "../../api/competitors";
import type { CompetitorResponse, CompetitorStatus, CompetitorType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";

const STATUS_TONE: Record<CompetitorStatus, string> = {
  ACTIVE: "oasis",
  INJURED: "rust",
  SUSPENDED: "gold",
  RETIRED: "stone",
};

const TYPE_TONE: Record<CompetitorType, string> = {
  CAMEL: "rust",
  DWARF: "oasis",
  MEDIUM: "stone",
  OTHER: "gold",
};

const PAGE_SIZE = 10;

export function CompetitorListPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<CompetitorStatus | "">("");
  const [type, setType] = useState<CompetitorType | "">("");
  const [search, setSearch] = useState("");

  const [data, setData] = useState<{ content: CompetitorResponse[]; totalPages: number } | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);
  const { isAdministrator } = useRoles();

  useEffect(() => {
    let cancelled = false;
    setData(null);
    setError(null);

    competitorsApi
      .getAll({
        page,
        size: PAGE_SIZE,
        status: status || undefined,
        competitorType: type || undefined,
      })
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "Something went wrong loading competitors.");
      });

    return () => {
      cancelled = true;
    };
  }, [page, status, type]);

  if (error) {
    return (
      <div className="state-box state-box--error">
        <p>{error}</p>
      </div>
    );
  }

  if (data === null) {
    return (
      <div className="state-box">
        <div className="spinner" />
        <p>Loading competitors...</p>
      </div>
    );
  }

  // The backend only filters by status/competitorType (see CompetitorController) —
  // there's no text-search endpoint yet, so this narrows the current page client-side
  // rather than pretending to search the whole dataset.
  const visible = search.trim()
    ? data.content.filter(
        (c) =>
          c.name.toLowerCase().includes(search.toLowerCase()) ||
          c.nickname.toLowerCase().includes(search.toLowerCase())
      )
    : data.content;

  return (
    <div>
      <div className="app-content__header">
        <h1>Competitors</h1>
        {isAdministrator && (
          <Link to="/competitors/new" className="btn btn-primary">
            + New competitor
          </Link>
        )}
      </div>
      <p className="app-content__subtitle">{visible.length} shown on this page</p>

      <div className="filter-bar">
        <input
          placeholder="Search by name or nickname (this page only)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          value={type}
          onChange={(e) => {
            setPage(0);
            setType(e.target.value as CompetitorType | "");
          }}
        >
          <option value="">All types</option>
          <option value="CAMEL">Camel</option>
          <option value="DWARF">Dwarf</option>
          <option value="MEDIUM">Medium</option>
          <option value="OTHER">Other</option>
        </select>
        <select
          value={status}
          onChange={(e) => {
            setPage(0);
            setStatus(e.target.value as CompetitorStatus | "");
          }}
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INJURED">Injured</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="RETIRED">Retired</option>
        </select>
      </div>

      {visible.length === 0 ? (
        <div className="state-box">
          <p>No competitors match these filters.</p>
        </div>
      ) : (
        <div className="sheet">
          {visible.map((c, index) => (
            <Link
              key={c.id}
              to={`/competitors/${c.id}`}
              className="sheet-row"
              style={{ color: "inherit" }}
            >
              <span className="sheet-row__index">
                {String(page * PAGE_SIZE + index + 1).padStart(2, "0")}
              </span>
              <span className={`sheet-row__stripe stripe-${TYPE_TONE[c.competitorType]}`} />
              <div className="sheet-row__body">
                <div className="sheet-row__title">
                  {c.name} <span style={{ opacity: 0.6, fontWeight: 400 }}>“{c.nickname}”</span>
                </div>
                <div className="sheet-row__meta">
                  <span>{c.competitorType}</span>
                  <span>{c.origin}</span>
                  <span>{c.team ? c.team.name : "No team"}</span>
                  <span>{c.victories} wins</span>
                </div>
              </div>
              <span className={`sheet-row__status status-${STATUS_TONE[c.status]}`}>
                {c.status}
              </span>
            </Link>
          ))}
        </div>
      )}

      <div className="pagination">
        <button
          className="btn btn-secondary"
          disabled={page === 0}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          Prev
        </button>
        <span>
          Page {page + 1} of {Math.max(1, data.totalPages)}
        </span>
        <button
          className="btn btn-secondary"
          disabled={page + 1 >= data.totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}

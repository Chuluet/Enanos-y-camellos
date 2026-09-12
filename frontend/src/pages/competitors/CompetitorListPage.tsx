import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { competitorsApi } from "../../api/competitors";
import type { CompetitorResponse, CompetitorStatus, CompetitorType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";
import { COMPETITOR_AVATAR } from "../../assets/competitorAvatars";
import camelBg from "../../assets/camel-bg.jpg";

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

    return (
        <>
            <div className="page-bg-fixed" style={{ backgroundImage: `url(${camelBg})` }} />
            <div>
                <Link to="/" className="page-breadcrumb">
                    ← Main menu
                </Link>
                <h1 className="page-title-banner">Competitors</h1>

                <div className="page-session-bar">
                    <span>Role: {isAdministrator ? "Admin" : "Viewer"}</span>
                    {isAdministrator && (
                        <Link to="/competitors/new" className="btn btn-primary">
                            + New competitor
                        </Link>
                    )}
                </div>

                {error ? (
                    <div className="state-box state-box--error">
                        <p>{error}</p>
                    </div>
                ) : data === null ? (
                    <div className="state-box">
                        <div className="spinner" />
                        <p>Loading competitors...</p>
                    </div>
                ) : (
                    <>
                        <div className="filter-bar">
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

                        {data.content.length === 0 ? (
                            <div className="state-box">
                                <p>No competitors match these filters.</p>
                            </div>
                        ) : (
                            <div className="sheet">
                                {data.content.map((c, index) => (
                                    <Link
                                        key={c.id}
                                        to={`/competitors/${c.id}`}
                                        className="sheet-row sheet-row--competitor cascade"
                                        style={{ color: "inherit", animationDelay: `${index * 40}ms` }}
                                    >
                      <span className="sheet-row__index">
                        {String(page * PAGE_SIZE + index + 1).padStart(2, "0")}
                      </span>
                                        <span className={`sheet-row__stripe stripe-${TYPE_TONE[c.competitorType]}`} />
                                        <img
                                            src={COMPETITOR_AVATAR[c.competitorType]}
                                            alt=""
                                            className="sheet-row__avatar"
                                        />
                                        <div className="sheet-row__body">
                                            <div className="sheet-row__title">{c.name}</div>
                                            <div className="sheet-row__meta">
                                                <span>{c.team ? c.team.name : "No team"}</span>
                                                <span>{c.competitorType}</span>
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
                    </>
                )}
            </div>
        </>
    );
}
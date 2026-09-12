import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { standingsApi } from "../../api/standings";
import type { Standings, CompetitorType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import heroImage from "../../assets/hero.png";
import { COMPETITOR_AVATAR } from "../../assets/competitorAvatars";

const TYPE_TONE: Record<CompetitorType, string> = {
  CAMEL: "rust",
  DWARF: "oasis",
  MEDIUM: "stone",
  OTHER: "gold",
};

export function StandingsPage() {
  const [standings, setStandings] = useState<Standings | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    standingsApi
      .getAll()
      .then((data) => {
        if (!cancelled) setStandings(data);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "Something went wrong loading standings.");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return (
      <div className="state-box state-box--error">
        <p>{error}</p>
      </div>
    );
  }

  if (standings === null) {
    return (
      <div className="state-box">
        <div className="spinner" />
        <p>Loading standings...</p>
      </div>
    );
  }

  return (
    <>
      <div className="page-bg-fixed" style={{ backgroundImage: `url(${heroImage})` }} />
      <div>
        <Link to="/" className="page-breadcrumb">
          ← Main menu
        </Link>
        <h1 className="page-title-banner">Standings</h1>

        <h2>Competitors</h2>
        {standings.competitors.length === 0 ? (
          <div className="state-box">
            <p>No results recorded yet.</p>
          </div>
        ) : (
          <div className="sheet" style={{ marginBottom: 32 }}>
            {standings.competitors.map((c, index) => (
              <Link
                key={c.competitorId}
                to={`/competitors/${c.competitorId}`}
                className="sheet-row cascade"
                style={{ color: "inherit", animationDelay: `${index * 40}ms` }}
              >
                <span className="sheet-row__index" style={{ color: "var(--rust)", opacity: 1 }}>
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span className={`sheet-row__stripe stripe-${TYPE_TONE[c.competitorType]}`} />
                <img
                  src={COMPETITOR_AVATAR[c.competitorType]}
                  alt=""
                  className="sheet-row__avatar"
                />
                <div className="sheet-row__body">
                  <div className="sheet-row__title">{c.nickname}</div>
                  <div className="sheet-row__meta">
                    <span>{c.competitorType}</span>
                    <span>
                      {c.victories}W – {c.defeats}L
                    </span>
                    <span>{c.completedRaces} races completed</span>
                  </div>
                </div>
                <span className="sheet-row__status status-gold">{c.totalPoints} pts</span>
              </Link>
            ))}
          </div>
        )}

        <h2>Teams</h2>
        {standings.teams.length === 0 ? (
          <div className="state-box">
            <p>No results recorded yet.</p>
          </div>
        ) : (
          <div className="sheet">
            {standings.teams.map((t, index) => (
              <Link
                key={t.teamId}
                to={`/teams/${t.teamId}`}
                className="sheet-row cascade"
                style={{ color: "inherit", animationDelay: `${index * 40}ms` }}
              >
                <span className="sheet-row__index" style={{ color: "var(--rust)", opacity: 1 }}>
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span className="sheet-row__stripe stripe-gold" />
                <div className="sheet-row__body">
                  <div className="sheet-row__title">{t.name}</div>
                  <div className="sheet-row__meta">
                    <span>
                      {t.victories}W – {t.defeats}L
                    </span>
                  </div>
                </div>
                <span className="sheet-row__status status-gold">{t.totalPoints} pts</span>
              </Link>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
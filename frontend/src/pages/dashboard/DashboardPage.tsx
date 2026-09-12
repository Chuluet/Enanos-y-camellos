import { useEffect, useState } from "react";
import { racesApi } from "../../api/races";
import { competitorsApi } from "../../api/competitors";
import { teamsApi } from "../../api/teams";
import { resultsApi } from "../../api/results";
import type { Race, RaceResultResponse } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import dwarfBg from "../../assets/dwarf.jpg";

const RACE_STATUS_LABEL: Record<Race["status"], string> = {
    DRAFT: "Borrador",
    OPEN_FOR_REGISTRATION: "Inscripciones abiertas",
    CLOSED_FOR_REGISTRATION: "Inscripciones cerradas",
    IN_PROGRESS: "En curso",
    COMPLETED: "Finalizada",
    CANCELLED: "Cancelada",
};

const RACE_STATUS_TONE: Record<Race["status"], string> = {
    DRAFT: "stone",
    OPEN_FOR_REGISTRATION: "oasis",
    CLOSED_FOR_REGISTRATION: "gold",
    IN_PROGRESS: "gold",
    COMPLETED: "stone",
    CANCELLED: "rust",
};

interface DashboardData {
    featuredRace: Race | null;
    upcomingCount: number;
    activeCompetitorsCount: number;
    teamsCount: number;
    recentRace: Race | null;
    recentResults: RaceResultResponse[];
}

function pickFeaturedRace(races: Race[]): Race | null {
    const inProgress = races.find((r) => r.status === "IN_PROGRESS");
    if (inProgress) return inProgress;

    const upcoming = races
        .filter((r) => r.status === "OPEN_FOR_REGISTRATION")
        .sort((a, b) => a.scheduledDateTime.localeCompare(b.scheduledDateTime));
    return upcoming[0] ?? null;
}

function pickMostRecentCompletedRace(races: Race[]): Race | null {
    const completed = races
        .filter((r) => r.status === "COMPLETED")
        .sort((a, b) => b.scheduledDateTime.localeCompare(a.scheduledDateTime));
    return completed[0] ?? null;
}

function participantLabel(result: RaceResultResponse): string {
    return result.competitor?.nickname ?? result.team?.name ?? "—";
}

export function DashboardPage() {
    const [data, setData] = useState<DashboardData | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function load() {
            setData(null);
            setError(null);
            try {
                const [races, activeCompetitors, teams] = await Promise.all([
                    racesApi.getAll(),
                    competitorsApi.getAll({ status: "ACTIVE" }),
                    teamsApi.getAll(),
                ]);

                const featuredRace = pickFeaturedRace(races);
                const upcomingCount = races.filter(
                    (r) => r.status === "OPEN_FOR_REGISTRATION" || r.status === "CLOSED_FOR_REGISTRATION"
                ).length;
                const recentRace = pickMostRecentCompletedRace(races);

                const recentResults = recentRace
                    ? (await resultsApi.getByRace(recentRace.id))
                        .filter((r) => r.status === "FINISHED" && r.finalPosition != null)
                        .sort((a, b) => (a.finalPosition ?? 0) - (b.finalPosition ?? 0))
                        .slice(0, 3)
                    : [];

                if (cancelled) return;
                setData({
                    featuredRace,
                    upcomingCount,
                    activeCompetitorsCount: activeCompetitors.totalElements,
                    teamsCount: teams.length,
                    recentRace,
                    recentResults,
                });
            } catch (err) {
                if (cancelled) return;
                setError(err instanceof ApiError ? err.message : "Algo salió mal cargando el tablero.");
            }
        }

        load();
        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <>
            <div className="page-bg-fixed" style={{ backgroundImage: `url(${dwarfBg})` }} />
            <div>
                <h1 className="page-title-banner">Tablero</h1>
                <p className="app-content__subtitle">
                    Próxima carrera, competidores activos y últimos resultados
                </p>

                {error ? (
                    <div className="state-box state-box--error">
                        <p>{error}</p>
                    </div>
                ) : data === null ? (
                    <div className="state-box">
                        <div className="spinner" />
                        <p>Cargando tablero...</p>
                    </div>
                ) : (
                    <>
                        {data.featuredRace ? (
                            <div className="detail-card featured-race">
                                <div className="featured-race__header">
                                    <div>
                                        <h2>{data.featuredRace.name}</h2>
                                        <p className="featured-race__meta">
                                            {new Date(data.featuredRace.scheduledDateTime).toLocaleString()} ·{" "}
                                            {data.featuredRace.distanceMeters} m ·{" "}
                                            {data.featuredRace.raceType === "MIXED" ? "Mixta" : data.featuredRace.raceType}
                                        </p>
                                    </div>
                                    <span className={`sheet-row__status status-${RACE_STATUS_TONE[data.featuredRace.status]}`}>
                          {RACE_STATUS_LABEL[data.featuredRace.status]}
                        </span>
                                </div>
                            </div>
                        ) : (
                            <div className="state-box">
                                <p>No hay carreras en curso ni próximas por ahora.</p>
                            </div>
                        )}

                        <div className="dashboard-stats">
                            <div className="stat-card">
                                <div className="stat-card__value">{data.activeCompetitorsCount}</div>
                                <div className="stat-card__label">Competidores activos</div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-card__value">{data.upcomingCount}</div>
                                <div className="stat-card__label">Carreras próximas</div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-card__value">{data.teamsCount}</div>
                                <div className="stat-card__label">Equipos</div>
                            </div>
                        </div>

                        <h2>Resultados recientes</h2>
                        {data.recentRace === null ? (
                            <div className="state-box">
                                <p>Todavía no hay carreras finalizadas.</p>
                            </div>
                        ) : (
                            <div className="detail-card">
                                <div className="page-session-bar">
                                    <span>{data.recentRace.name}</span>
                                    <span className="sheet-row__status status-stone">Finalizada</span>
                                </div>
                                {data.recentResults.length === 0 ? (
                                    <div className="state-box">
                                        <p>Esta carrera no tiene resultados registrados.</p>
                                    </div>
                                ) : (
                                    <div className="sheet">
                                        {data.recentResults.map((result) => (
                                            <div key={result.id} className="sheet-row">
                                                <div className="sheet-row__index">{result.finalPosition}º</div>
                                                <div className="sheet-row__body">
                                                    <div className="sheet-row__title">{participantLabel(result)}</div>
                                                    {result.notes && (
                                                        <div className="sheet-row__meta">
                                                            <span>{result.notes}</span>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </>
                )}
            </div>
        </>
    );
}
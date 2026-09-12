import { apiClient } from "./apiClient";
import type { Standings, CompetitorStanding, TeamStanding } from "./types";

export const standingsApi = {
  getAll: () => apiClient.get<Standings>("/api/standings"),
  getCompetitors: () => apiClient.get<CompetitorStanding[]>("/api/standings/competitors"),
  getTeams: () => apiClient.get<TeamStanding[]>("/api/standings/teams"),
};
import { apiClient } from "./apiClient";
import type { RaceResult, CreateResultInput } from "./types";

export const resultsApi = {
  getByRace: (raceId: string) =>
    apiClient.get<RaceResult[]>(`/api/races/${raceId}/results`),

  getById: (id: string) => apiClient.get<RaceResult>(`/api/results/${id}`),

  record: (raceId: string, data: CreateResultInput) =>
    apiClient.post<RaceResult>(`/api/races/${raceId}/results`, data),
};
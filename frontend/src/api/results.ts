import { apiClient } from "./apiClient";
import type { RaceResultResponse, CreateResultInput, UpdateResultInput } from "./types";

export const resultsApi = {
  /** GET /api/races/{raceId}/results */
  getByRace: (raceId: string) =>
    apiClient.get<RaceResultResponse[]>(`/api/races/${raceId}/results`),

  getById: (id: string) => apiClient.get<RaceResultResponse>(`/api/results/${id}`),

  record: (raceId: string, data: CreateResultInput) =>
    apiClient.post<RaceResultResponse>(`/api/races/${raceId}/results`, data),

  update: (id: string, data: UpdateResultInput) =>
    apiClient.put<RaceResultResponse>(`/api/results/${id}`, data),
};
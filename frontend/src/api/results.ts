import { apiClient } from "./apiClient";
import type { RaceResultResponse } from "./types";

export const resultsApi = {
    /** GET /api/races/{raceId}/results */
    getByRace: (raceId: string) =>
        apiClient.get<RaceResultResponse[]>(`/api/races/${raceId}/results`),

    getById: (id: string) => apiClient.get<RaceResultResponse>(`/api/results/${id}`),
};
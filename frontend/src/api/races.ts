import { apiClient } from "./apiClient";
import type {
  Race,
  CreateRaceInput,
  UpdateRaceInput,
  RaceStatus,
  RaceType,
} from "./types";

export interface RaceFilters {
  status?: RaceStatus;
  type?: RaceType;
}

function buildQuery(filters: RaceFilters): string {
  const params = new URLSearchParams();
  if (filters.status) params.set("status", filters.status);
  if (filters.type) params.set("type", filters.type);
  const query = params.toString();
  return query ? `?${query}` : "";
}

export const racesApi = {
  getAll: (filters: RaceFilters = {}) =>
    apiClient.get<Race[]>(`/api/races${buildQuery(filters)}`),

  getById: (id: string) => apiClient.get<Race>(`/api/races/${id}`),

  create: (data: CreateRaceInput) =>
    apiClient.post<Race>("/api/races", data),

  update: (id: string, data: UpdateRaceInput) =>
    apiClient.put<Race>(`/api/races/${id}`, data),

  changeStatus: (id: string, status: RaceStatus) =>
    apiClient.patch<Race>(`/api/races/${id}/status`, { status }),

  cancel: (id: string) => apiClient.delete(`/api/races/${id}`),
};
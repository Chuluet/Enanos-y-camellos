import { apiClient } from "./apiClient";
import type {
  CompetitorResponse,
  CreateCompetitorInput,
  UpdateCompetitorInput,
  PatchCompetitorInput,
  CompetitorStatus,
  CompetitorType,
  Page,
} from "./types";

export interface CompetitorFilters {
  status?: CompetitorStatus;
  competitorType?: CompetitorType;
  page?: number;
  size?: number;
  sort?: string;
}

function buildQuery(filters: CompetitorFilters): string {
  const params = new URLSearchParams();
  if (filters.status) params.set("status", filters.status);
  if (filters.competitorType) params.set("competitorType", filters.competitorType);
  if (filters.page !== undefined) params.set("page", String(filters.page));
  if (filters.size !== undefined) params.set("size", String(filters.size));
  if (filters.sort) params.set("sort", filters.sort);
  const query = params.toString();
  return query ? `?${query}` : "";
}

export const competitorsApi = {
  /** Paginated + filterable list — GET /api/competitors */
  getAll: (filters: CompetitorFilters = {}) =>
    apiClient.get<Page<CompetitorResponse>>(`/api/competitors${buildQuery(filters)}`),

  /** Full unpaginated list — GET /api/competitors/all (handy for "assign to team" pickers). */
  getAllList: () => apiClient.get<CompetitorResponse[]>("/api/competitors/all"),

  getById: (id: string) => apiClient.get<CompetitorResponse>(`/api/competitors/${id}`),

  create: (data: CreateCompetitorInput) =>
    apiClient.post<CompetitorResponse>("/api/competitors", data),

  /** PUT — full replace. Does not touch status or statistics. */
  update: (id: string, data: UpdateCompetitorInput) =>
    apiClient.put<CompetitorResponse>(`/api/competitors/${id}`, data),

  /** PATCH — partial update. Does not touch status or statistics either. */
  patch: (id: string, data: PatchCompetitorInput) =>
    apiClient.patch<CompetitorResponse>(`/api/competitors/${id}`, data),

  changeStatus: (id: string, status: CompetitorStatus) =>
    apiClient.patch<CompetitorResponse>(`/api/competitors/${id}/status`, { status }),

  /** Not a physical delete — sets status to RETIRED, preserving race history. */
  retire: (id: string) => apiClient.delete(`/api/competitors/${id}`),
};

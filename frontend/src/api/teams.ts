import { apiClient } from "./apiClient";
import type {
  TeamResponse,
  CreateTeamInput,
  UpdateTeamInput,
  PatchTeamInput,
  TeamStatus,
} from "./types";

export const teamsApi = {
  getAll: (status?: TeamStatus) =>
    apiClient.get<TeamResponse[]>(`/api/teams${status ? `?status=${status}` : ""}`),

  getById: (id: string) => apiClient.get<TeamResponse>(`/api/teams/${id}`),

  create: (data: CreateTeamInput) => apiClient.post<TeamResponse>("/api/teams", data),

  /** PUT — full replace. Does not touch members or statistics. */
  update: (id: string, data: UpdateTeamInput) =>
    apiClient.put<TeamResponse>(`/api/teams/${id}`, data),

  /** PATCH — partial update. Does not touch members or statistics either. */
  patch: (id: string, data: PatchTeamInput) =>
    apiClient.patch<TeamResponse>(`/api/teams/${id}`, data),

  addMember: (teamId: string, competitorId: string) =>
    apiClient.post<TeamResponse>(`/api/teams/${teamId}/members/${competitorId}`, undefined),

  /**
   * The backend returns the updated team (200), but apiClient.delete is
   * typed as void for every caller, so we just re-fetch afterwards instead
   * of trying to squeeze a typed body out of a DELETE call.
   */
  removeMember: async (teamId: string, competitorId: string) => {
    await apiClient.delete(`/api/teams/${teamId}/members/${competitorId}`);
    return teamsApi.getById(teamId);
  },

  /** Not a physical delete — sets status to INACTIVE. */
  deactivate: (id: string) => apiClient.delete(`/api/teams/${id}`),
};

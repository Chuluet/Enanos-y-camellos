import { apiClient } from "./apiClient";
import type { Registration, CreateRegistrationInput } from "./types";

export const registrationsApi = {
  getByRace: (raceId: string) =>
    apiClient.get<Registration[]>(`/api/races/${raceId}/registrations`),

  getById: (id: string) => apiClient.get<Registration>(`/api/registrations/${id}`),

  register: (raceId: string, data: CreateRegistrationInput) =>
    apiClient.post<Registration>(`/api/races/${raceId}/registrations`, data),

  approve: (id: string) => apiClient.patch<Registration>(`/api/registrations/${id}/approve`),

  reject: (id: string, reason: string) =>
    apiClient.patch<Registration>(`/api/registrations/${id}/reject`, { reason }),

  cancel: (id: string) => apiClient.delete(`/api/registrations/${id}`),
};
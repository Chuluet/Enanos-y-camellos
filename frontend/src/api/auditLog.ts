import { apiClient } from "./apiClient";
import type { AuditLogEntry } from "./types";

export interface AuditLogFilters {
  entityType?: string;
  entityId?: string;
  username?: string;
  from?: string;
  to?: string;
}

function buildQuery(filters: AuditLogFilters): string {
  const params = new URLSearchParams();
  if (filters.entityType) params.set("entityType", filters.entityType);
  if (filters.entityId) params.set("entityId", filters.entityId);
  if (filters.username) params.set("username", filters.username);
  if (filters.from) params.set("from", filters.from);
  if (filters.to) params.set("to", filters.to);
  const query = params.toString();
  return query ? `?${query}` : "";
}

export const auditLogApi = {
  getAll: (filters: AuditLogFilters = {}) =>
    apiClient.get<AuditLogEntry[]>(`/api/audit-log${buildQuery(filters)}`),
};
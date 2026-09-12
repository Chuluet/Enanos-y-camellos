import { userManager } from "../auth/oidcConfig";
import { API_BASE_URL } from "../auth/oidcConfig";

/** Mirrors GlobalExceptionHandler.ErrorResponse on the backend. */
export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  /** Only present on 400 validation errors: field name -> specific message. */
  validationErrors?: Record<string, string> | null;
}

export class ApiError extends Error {
  status: number;
  body: ApiErrorResponse | null;
  /** Convenience getter: field -> message, or {} if this wasn't a validation error. */
  validationErrors: Record<string, string>;

  constructor(status: number, body: ApiErrorResponse | null) {
    const validationErrors = body?.validationErrors ?? {};
    const detail = Object.entries(validationErrors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join("; ");

    super(detail ? `${body?.message} (${detail})` : body?.message ?? `Request failed with status ${status}`);
    this.status = status;
    this.body = body;
    this.validationErrors = validationErrors;
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const user = await userManager.getUser();
  const token = user?.access_token;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    let body: ApiErrorResponse | null = null;
    try {
      body = await response.json();
    } catch {
      // 403 from the security filter chain (not our GlobalExceptionHandler)
      // comes back with an empty body — see the note in the assignment,
      // that's expected, not a parsing bug.
    }
    throw new ApiError(response.status, body);
  }

  // 204 No Content (DELETE endpoints) has no body to parse.
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: "PATCH",
      body: body ? JSON.stringify(body) : undefined,
    }),
  delete: (path: string) => request<void>(path, { method: "DELETE" }),
};
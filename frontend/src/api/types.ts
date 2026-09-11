export type RaceType = "INDIVIDUAL" | "TEAM" | "MIXED";

export type RaceStatus =
  | "DRAFT"
  | "OPEN_FOR_REGISTRATION"
  | "CLOSED_FOR_REGISTRATION"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

/** Mirrors RaceResponse.java */
export interface Race {
  id: string;
  name: string;
  description: string | null;
  scheduledDateTime: string; // ISO datetime, e.g. "2026-12-01T10:00:00"
  startLocation: string;
  finishLocation: string;
  distanceMeters: number;
  maxParticipants: number;
  raceType: RaceType;
  status: RaceStatus;
  organizer: string;
  registrationDeadline: string;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors RaceRequest.java (POST body) */
export interface CreateRaceInput {
  name: string;
  description?: string;
  scheduledDateTime: string;
  startLocation: string;
  finishLocation: string;
  distanceMeters: number;
  maxParticipants: number;
  raceType: RaceType;
  organizer: string;
  registrationDeadline: string;
}

/** Mirrors RaceUpdateRequest.java (PUT body) — every field optional, only sent fields change */
export type UpdateRaceInput = Partial<Omit<CreateRaceInput, "raceType" | "organizer">>;
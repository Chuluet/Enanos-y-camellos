/** Mirrors Spring Data's default Page<T> JSON shape. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page index, 0-based
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

/* ============================== Races ============================== */

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

/* ============================== Competitors ============================== */

export type CompetitorType = "DWARF" | "CAMEL" | "MEDIUM" | "OTHER";

export type CompetitorStatus = "ACTIVE" | "INJURED" | "SUSPENDED" | "RETIRED";

/** Short version used inside TeamResponse.members — mirrors CompetitorSummaryResponse.java */
export interface CompetitorSummaryResponse {
  id: string;
  name: string;
  nickname: string;
  competitorType: CompetitorType;
  status: CompetitorStatus;
}

/** Mirrors CompetitorResponse.java */
export interface CompetitorResponse {
  id: string;
  name: string;
  nickname: string;
  competitorType: CompetitorType;
  dateOfBirth: string | null; // ISO date, e.g. "1990-01-01"
  approximateAge: number | null;
  height: number;
  weight: number;
  origin: string;
  status: CompetitorStatus;
  registrationDate: string;
  victories: number;
  defeats: number;
  completedRaces: number;
  team: TeamSummaryResponse | null;
}

/** Mirrors CompetitorRequest.java (POST body). Provide dateOfBirth or approximateAge. */
export interface CreateCompetitorInput {
  name: string;
  nickname: string;
  competitorType: CompetitorType;
  dateOfBirth?: string;
  approximateAge?: number;
  height: number;
  weight: number;
  origin: string;
  status?: CompetitorStatus;
}

/** Mirrors CompetitorUpdateRequest.java (PUT body) — full replace, status/stats not editable here. */
export interface UpdateCompetitorInput {
  name: string;
  nickname: string;
  competitorType: CompetitorType;
  dateOfBirth?: string;
  approximateAge?: number;
  height: number;
  weight: number;
  origin: string;
}

/** Mirrors CompetitorPatchRequest.java (PATCH body) — every field optional, only sent fields change. */
export type PatchCompetitorInput = Partial<UpdateCompetitorInput>;

/** Mirrors CompetitorStatusUpdateRequest.java (PATCH /{id}/status body) */
export interface CompetitorStatusUpdateInput {
  status: CompetitorStatus;
}

/* ============================== Teams ============================== */

export type TeamStatus = "ACTIVE" | "SUSPENDED" | "INACTIVE";

/** Short version used inside CompetitorResponse.team — mirrors TeamSummaryResponse.java */
export interface TeamSummaryResponse {
  id: string;
  name: string;
  status: TeamStatus;
}

/** Mirrors TeamResponse.java */
export interface TeamResponse {
  id: string;
  name: string;
  description: string | null;
  coach: string;
  creationDate: string;
  status: TeamStatus;
  victories: number;
  defeats: number;
  maxMembers: number;
  members: CompetitorSummaryResponse[];
}

/** Mirrors TeamRequest.java (POST body) */
export interface CreateTeamInput {
  name: string;
  description?: string;
  coach: string;
  maxMembers: number;
  status?: TeamStatus;
  memberIds?: string[];
}

/** Mirrors TeamUpdateRequest.java (PUT body) — full replace, members/stats not editable here. */
export interface UpdateTeamInput {
  name: string;
  description?: string;
  coach: string;
  maxMembers: number;
  status: TeamStatus;
}

/** Mirrors TeamPatchRequest.java (PATCH body) — every field optional, only sent fields change. */
export type PatchTeamInput = Partial<UpdateTeamInput>;

/* ============================== Registrations ============================== */

export type RegistrationStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

/* ============================== Results ============================== */

export type ResultStatus = "FINISHED" | "DISQUALIFIED" | "DID_NOT_FINISH" | "DID_NOT_START";

/** Mirrors RaceSummaryResponse.java — used inside RaceResultResponse.race and RaceRegistrationResponse.race */
export interface RaceSummaryResponse {
  id: string;
  name: string;
  raceType: RaceType;
  status: RaceStatus;
  scheduledDateTime: string;
}

/** Mirrors RaceRegistrationResponse.java */
export interface Registration {
  id: string;
  race: RaceSummaryResponse;
  competitor: CompetitorSummaryResponse | null;
  team: TeamSummaryResponse | null;
  registrationDate: string;
  status: RegistrationStatus;
  startingPosition: number | null;
  validationNotes: string | null;
  registeredBy: string;
}

/** Mirrors RaceRegistrationRequest.java */
export interface CreateRegistrationInput {
  competitorId?: string;
  teamId?: string;
  startingPosition?: number;
  registeredBy: string;
}

/** Mirrors RaceResultResponse.java. Exactly one of competitor/team is present. */
export interface RaceResultResponse {
  id: string;
  registrationId: string;
  race: RaceSummaryResponse;
  competitor: CompetitorSummaryResponse | null;
  team: TeamSummaryResponse | null;
  startPosition: number | null;
  finalPosition: number | null;
  completionTime: number | null;
  penaltyTime: number | null;
  status: ResultStatus;
  notes: string | null;
  recordedBy: string;
  recordedAt: string;
}

/** Mirrors RaceResultRequest.java */
export interface CreateResultInput {
  registrationId: string;
  startPosition?: number;
  finalPosition?: number;
  completionTime?: number;
  penaltyTime?: number;
  status: ResultStatus;
  notes?: string;
  recordedBy: string;
}

/** Mirrors CompetitorStandingResponse.java */
export interface CompetitorStanding {
  competitorId: string;
  nickname: string;
  competitorType: CompetitorType;
  totalPoints: number;
  victories: number;
  defeats: number;
  completedRaces: number;
}

/** Mirrors TeamStandingResponse.java */
export interface TeamStanding {
  teamId: string;
  name: string;
  totalPoints: number;
  victories: number;
  defeats: number;
}

/** Mirrors StandingsResponse.java */
export interface Standings {
  competitors: CompetitorStanding[];
  teams: TeamStanding[];
}

/** Mirrors AuditLogResponse.java */
export interface AuditLogEntry {
  id: string;
  username: string;
  action: string;
  entityType: string;
  entityId: string;
  timestamp: string;
  description: string | null;
  oldValue: string | null;
  newValue: string | null;
}
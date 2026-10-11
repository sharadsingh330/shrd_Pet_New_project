/**
 * Frontend API adapter for the future Java Servlet backend
 * (React -> Servlets -> Service -> DAO -> JDBC -> MySQL).
 *
 * Not wired into any page yet. Existing pages still use Supabase via
 * pets.functions.ts / platform.functions.ts.
 */

export const API_URL: string =
  (import.meta.env["VITE_API_URL"] as string | undefined)?.replace(/\/+$/, "") ||
  "http://localhost:8080/pawlift/api";

/* ---------------------------------- types ---------------------------------- */

export type Role = "admin" | "shelter" | "adopter";
export type AdoptionStatus = "available" | "pending" | "adopted";
export type ListingStatus = "pending" | "approved" | "rejected";
export type ApplicationStatus = "pending" | "approved" | "rejected";

export interface User {
  id: string;
  email: string;
  full_name: string;
  phone?: string;
  city?: string;
  bio?: string;
  role: Role;
  created_at?: string;
}

export interface Shelter {
  id: string;
  owner_id?: string;
  name: string;
  city: string;
  email?: string;
  verified: boolean;
}

export interface Pet {
  id: string;
  shelter_id: string;
  shelter_name?: string;
  name: string;
  species: string;
  breed: string;
  age_label: string;
  size: string;
  temperament: string;
  city: string;
  description: string;
  image_url: string | null;
  image_key: string;
  match_score: number;
  adoption_status: AdoptionStatus;
  listing_status: ListingStatus;
  created_at?: string;
}

export interface Application {
  id: string;
  pet_id: string;
  adopter_id: string;
  adopter_name: string;
  adopter_email: string;
  housing: string;
  experience: string;
  note: string;
  status: ApplicationStatus;
  created_at: string;
  pet?: Pick<Pet, "id" | "name" | "breed" | "species" | "image_key" | "image_url" | "adoption_status">;
}

export interface Message {
  id: string;
  application_id: string;
  sender_id: string;
  sender_name: string;
  body: string;
  created_at: string;
}

export interface Setting {
  key: string;
  value: string;
  updated_at?: string;
}

export interface AdminOverview {
  users: User[];
  pets: Pet[];
  shelters: Shelter[];
  settings: Setting[];
  analytics: {
    users: number;
    shelters: number;
    listings: number;
    pendingListings: number;
    applications: number;
    approved: number;
    adopted: number;
  };
}

export interface MessageResponse {
  message: string;
}

export interface AuthResponse {
  user: User;
  token?: string;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput extends LoginInput {
  full_name: string;
  role: Exclude<Role, "admin">;
  shelter_name?: string;
  city?: string;
}

export interface PetInput {
  name: string;
  species: string;
  breed: string;
  age_label: string;
  size: string;
  temperament: string;
  description: string;
  image_url: string;
}

export interface PetUpdateInput extends Partial<PetInput> {
  adoption_status?: AdoptionStatus;
  listing_status?: ListingStatus;
}

export interface ApplyInput {
  pet_id: string;
  housing: string;
  experience: string;
  note: string;
}

/* ------------------------------- core request ------------------------------ */

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  headers.set("Accept", "application/json");

  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
    credentials: "include", // send the Servlet session cookie (JSESSIONID)
  });

  const text = await res.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const msg =
      (data && typeof data === "object" && "error" in data ? String((data as { error: unknown }).error) : "") ||
      (typeof data === "string" && data) ||
      res.statusText ||
      "Request failed";
    throw new ApiError(res.status, msg);
  }
  return data as T;
}

const json = (body: unknown) => JSON.stringify(body);
const enc = encodeURIComponent;

/* ----------------------------------- auth ---------------------------------- */

export const login = (input: LoginInput) =>
  request<AuthResponse>("/auth/login", { method: "POST", body: json(input) });

export const register = (input: RegisterInput) =>
  request<AuthResponse>("/auth/register", { method: "POST", body: json(input) });

export const logout = () => request<MessageResponse>("/auth/logout", { method: "POST" });

export const getCurrentUser = () =>
  request<{ user: User; shelter: Shelter | null }>("/auth/me");

/* ----------------------------------- pets ---------------------------------- */

export const getPets = (filters: { q?: string; species?: string; city?: string } = {}) => {
  const params = new URLSearchParams();
  for (const [k, v] of Object.entries(filters)) if (v) params.set(k, v);
  const qs = params.toString();
  return request<Pet[]>(`/pets${qs ? `?${qs}` : ""}`);
};

export const getPetById = (id: string) =>
  request<{ pet: Pet; shelter: Shelter | null }>(`/pets/${enc(id)}`);

export const createPet = (input: PetInput) =>
  request<MessageResponse>("/pets", { method: "POST", body: json(input) });

export const updatePet = (id: string, input: PetUpdateInput) =>
  request<MessageResponse>(`/pets/${enc(id)}`, { method: "PUT", body: json(input) });

export const deletePet = (id: string) =>
  request<MessageResponse>(`/pets/${enc(id)}`, { method: "DELETE" });

/* --------------------------------- shelters -------------------------------- */

export const getShelters = () => request<Shelter[]>("/shelters");

/* ------------------------------- applications ------------------------------ */

/** Adopter: own applications. Shelter: applications for its pets. */
export const getApplications = () => request<Application[]>("/applications");

export const applyForPet = (input: ApplyInput) =>
  request<MessageResponse>("/applications", { method: "POST", body: json(input) });

export const updateApplicationStatus = (id: string, status: ApplicationStatus) =>
  request<MessageResponse>(`/applications/${enc(id)}/status`, {
    method: "PUT",
    body: json({ status }),
  });

/* --------------------------------- messages -------------------------------- */

export const getMessages = (applicationId: string) =>
  request<Message[]>(`/messages?application_id=${enc(applicationId)}`);

export const sendMessage = (applicationId: string, body: string) =>
  request<MessageResponse>("/messages", {
    method: "POST",
    body: json({ application_id: applicationId, body }),
  });

/* ----------------------------------- admin --------------------------------- */

export const getAdminOverview = () => request<AdminOverview>("/admin/overview");

export const updateUserRole = (userId: string, role: Role) =>
  request<MessageResponse>(`/admin/users/${enc(userId)}/role`, { method: "PUT", body: json({ role }) });

export const deleteUser = (userId: string) =>
  request<MessageResponse>(`/admin/users/${enc(userId)}`, { method: "DELETE" });

export const updateSetting = (key: string, value: string) =>
  request<MessageResponse>(`/admin/settings/${enc(key)}`, { method: "PUT", body: json({ value }) });

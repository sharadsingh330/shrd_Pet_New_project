import { createServerFn } from "@tanstack/react-start";
import { requireSupabaseAuth } from "@/integrations/supabase/auth-middleware";
import { z } from "zod";

const uuid = z.string().uuid();

export type AppRole = "admin" | "shelter" | "adopter";

/* ---------------------------------- shared --------------------------------- */

export const getMyContext = createServerFn({ method: "GET" })
  .middleware([requireSupabaseAuth])
  .handler(async ({ context }) => {
    const { supabase, userId } = context;
    const [{ data: profile }, { data: roles }, { data: shelter }] = await Promise.all([
      supabase.from("profiles").select("*").eq("id", userId).maybeSingle(),
      supabase.from("user_roles").select("role").eq("user_id", userId),
      supabase.from("shelters").select("*").eq("owner_id", userId).maybeSingle(),
    ]);
    const roleList = (roles ?? []).map((r) => r.role as AppRole);
    const role: AppRole = roleList.includes("admin")
      ? "admin"
      : roleList.includes("shelter")
        ? "shelter"
        : "adopter";
    return { profile, role, shelter };
  });

export const updateMyProfile = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z
      .object({
        full_name: z.string().trim().max(120),
        phone: z.string().trim().max(40),
        city: z.string().trim().max(120),
        bio: z.string().trim().max(600),
      })
      .parse(data),
  )
  .handler(async ({ data, context }) => {
    const { error } = await context.supabase
      .from("profiles")
      .update(data)
      .eq("id", context.userId);
    if (error) throw new Error(error.message);
    return { message: "Profile updated." };
  });

/* --------------------------------- adopter --------------------------------- */

export const myApplications = createServerFn({ method: "GET" })
  .middleware([requireSupabaseAuth])
  .handler(async ({ context }) => {
    const { data, error } = await context.supabase
      .from("applications")
      .select("*, pets(id, name, breed, species, image_key, image_url, adoption_status)")
      .eq("adopter_id", context.userId)
      .order("created_at", { ascending: false });
    if (error) throw new Error(error.message);
    return data ?? [];
  });

export const applyForPet = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z
      .object({
        pet_id: uuid,
        housing: z.string().trim().min(1).max(120),
        experience: z.string().trim().max(200),
        note: z.string().trim().max(800),
      })
      .parse(data),
  )
  .handler(async ({ data, context }) => {
    const { supabase, userId } = context;
    const { data: existing } = await supabase
      .from("applications")
      .select("id")
      .eq("pet_id", data.pet_id)
      .eq("adopter_id", userId)
      .maybeSingle();
    if (existing) return { message: "You already applied for this pet." };

    const { data: profile } = await supabase
      .from("profiles")
      .select("full_name, email")
      .eq("id", userId)
      .maybeSingle();

    const { error } = await supabase.from("applications").insert({
      ...data,
      adopter_id: userId,
      adopter_name: profile?.full_name || "Adopter",
      adopter_email: profile?.email || "",
    });
    if (error) throw new Error(error.message);
    return { message: "Application submitted. The shelter will be in touch." };
  });

/* --------------------------------- shelter --------------------------------- */

export const shelterOverview = createServerFn({ method: "GET" })
  .middleware([requireSupabaseAuth])
  .handler(async ({ context }) => {
    const { supabase, userId } = context;
    const { data: shelter } = await supabase
      .from("shelters")
      .select("*")
      .eq("owner_id", userId)
      .maybeSingle();
    if (!shelter) return { shelter: null, pets: [], applications: [] };

    const { data: pets } = await supabase
      .from("pets")
      .select("*")
      .eq("shelter_id", shelter.id)
      .order("created_at", { ascending: false });

    const petIds = (pets ?? []).map((p) => p.id);
    const applications = petIds.length
      ? ((
          await supabase
            .from("applications")
            .select("*, pets(id, name, breed)")
            .in("pet_id", petIds)
            .order("created_at", { ascending: false })
        ).data ?? [])
      : [];
    return { shelter, pets: pets ?? [], applications };
  });

export const createPet = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z
      .object({
        name: z.string().trim().min(1).max(60),
        species: z.string().trim().min(1).max(40),
        breed: z.string().trim().max(80),
        age_label: z.string().trim().max(30),
        size: z.string().trim().max(30),
        temperament: z.string().trim().max(60),
        description: z.string().trim().max(1200),
        image_url: z.string().trim().max(2000),
      })
      .parse(data),
  )
  .handler(async ({ data, context }) => {
    const { supabase, userId } = context;
    const { data: shelter } = await supabase
      .from("shelters")
      .select("id, city")
      .eq("owner_id", userId)
      .maybeSingle();
    if (!shelter) throw new Error("No shelter is linked to this account.");

    const imageKey =
      data.species.toLowerCase() === "cat" ? "cat" : data.breed.toLowerCase().includes("beagle") ? "beagle" : "dog";

    const { data: setting } = await supabase
      .from("system_settings")
      .select("value")
      .eq("key", "auto_approve_listings")
      .maybeSingle();
    const autoApprove = (setting?.value ?? "true").toLowerCase() !== "false";

    const { error } = await supabase.from("pets").insert({
      shelter_id: shelter.id,
      name: data.name,
      species: data.species,
      breed: data.breed,
      age_label: data.age_label,
      size: data.size,
      temperament: data.temperament,
      description: data.description,
      image_url: data.image_url || null,
      image_key: imageKey,
      city: shelter.city,
      listing_status: autoApprove ? "approved" : "pending",
      adoption_status: "available",
    });
    if (error) throw new Error(error.message);
    return {
      message: autoApprove ? `${data.name} is now live.` : `${data.name} submitted for admin review.`,
    };
  });

export const updatePetStatus = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z
      .object({
        pet_id: uuid,
        adoption_status: z.enum(["available", "pending", "adopted"]).optional(),
        listing_status: z.enum(["pending", "approved", "rejected"]).optional(),
      })
      .parse(data),
  )
  .handler(async ({ data, context }) => {
    const patch: { adoption_status?: string; listing_status?: string } = {};
    if (data.adoption_status) patch.adoption_status = data.adoption_status;
    if (data.listing_status) patch.listing_status = data.listing_status;
    const { error } = await context.supabase.from("pets").update(patch).eq("id", data.pet_id);
    if (error) throw new Error(error.message);
    return { message: "Listing updated." };
  });

export const deletePet = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) => z.object({ pet_id: uuid }).parse(data))
  .handler(async ({ data, context }) => {
    const { error } = await context.supabase.from("pets").delete().eq("id", data.pet_id);
    if (error) throw new Error(error.message);
    return { message: "Listing removed." };
  });

export const decideApplication = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z
      .object({
        application_id: uuid,
        status: z.enum(["pending", "approved", "rejected"]),
      })
      .parse(data),
  )
  .handler(async ({ data, context }) => {
    const { error } = await context.supabase
      .from("applications")
      .update({ status: data.status })
      .eq("id", data.application_id);
    if (error) throw new Error(error.message);
    return { message: `Application ${data.status}.` };
  });

/* -------------------------------- messaging -------------------------------- */

export const listMessages = createServerFn({ method: "GET" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) => z.object({ application_id: uuid }).parse(data))
  .handler(async ({ data, context }) => {
    const { data: rows, error } = await context.supabase
      .from("messages")
      .select("*")
      .eq("application_id", data.application_id)
      .order("created_at", { ascending: true });
    if (error) throw new Error(error.message);
    return rows ?? [];
  });

export const sendMessage = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z
      .object({
        application_id: uuid,
        body: z.string().trim().min(1).max(1000),
      })
      .parse(data),
  )
  .handler(async ({ data, context }) => {
    const { supabase, userId } = context;
    const { data: profile } = await supabase
      .from("profiles")
      .select("full_name")
      .eq("id", userId)
      .maybeSingle();
    const { error } = await supabase.from("messages").insert({
      application_id: data.application_id,
      sender_id: userId,
      sender_name: profile?.full_name || "User",
      body: data.body,
    });
    if (error) throw new Error(error.message);
    return { message: "Message sent." };
  });

/* ---------------------------------- admin ---------------------------------- */

async function assertAdmin(context: { supabase: { rpc: Function }; userId: string }) {
  const { data } = await (context.supabase as any).rpc("has_role", {
    _user_id: context.userId,
    _role: "admin",
  });
  if (!data) throw new Error("Admins only.");
}

export const adminOverview = createServerFn({ method: "GET" })
  .middleware([requireSupabaseAuth])
  .handler(async ({ context }) => {
    await assertAdmin(context as any);
    const { supabase } = context;
    const [profiles, roles, pets, applications, shelters, settings] = await Promise.all([
      supabase.from("profiles").select("*").order("created_at", { ascending: false }),
      supabase.from("user_roles").select("user_id, role"),
      supabase.from("pets").select("*, shelters(name, verified)").order("created_at", { ascending: false }),
      supabase.from("applications").select("id, status, created_at"),
      supabase.from("shelters").select("*"),
      supabase.from("system_settings").select("*").order("key"),
    ]);
    const roleMap = new Map<string, string>();
    for (const r of roles.data ?? []) roleMap.set(r.user_id, r.role);
    const users = (profiles.data ?? []).map((p) => ({ ...p, role: roleMap.get(p.id) ?? "adopter" }));
    const apps = applications.data ?? [];
    const petRows = pets.data ?? [];
    return {
      users,
      pets: petRows,
      shelters: shelters.data ?? [],
      settings: settings.data ?? [],
      analytics: {
        users: users.length,
        shelters: (shelters.data ?? []).length,
        listings: petRows.length,
        pendingListings: petRows.filter((p) => p.listing_status === "pending").length,
        applications: apps.length,
        approved: apps.filter((a) => a.status === "approved").length,
        adopted: petRows.filter((p) => p.adoption_status === "adopted").length,
      },
    };
  });

export const adminSetRole = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z.object({ user_id: uuid, role: z.enum(["admin", "shelter", "adopter"]) }).parse(data),
  )
  .handler(async ({ data, context }) => {
    await assertAdmin(context as any);
    const { supabaseAdmin } = await import("@/integrations/supabase/client.server");
    await supabaseAdmin.from("user_roles").delete().eq("user_id", data.user_id);
    const { error } = await supabaseAdmin
      .from("user_roles")
      .insert({ user_id: data.user_id, role: data.role });
    if (error) throw new Error(error.message);
    return { message: `Role set to ${data.role}.` };
  });

export const adminDeleteUser = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) => z.object({ user_id: uuid }).parse(data))
  .handler(async ({ data, context }) => {
    await assertAdmin(context as any);
    if (data.user_id === context.userId) throw new Error("You cannot delete your own account.");
    const { supabaseAdmin } = await import("@/integrations/supabase/client.server");
    const { error } = await supabaseAdmin.auth.admin.deleteUser(data.user_id);
    if (error) throw new Error(error.message);
    return { message: "User deleted." };
  });

export const adminUpdateSetting = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data) =>
    z.object({ key: z.string().trim().min(1).max(60), value: z.string().trim().max(200) }).parse(data),
  )
  .handler(async ({ data, context }) => {
    await assertAdmin(context as any);
    const { error } = await context.supabase
      .from("system_settings")
      .update({ value: data.value, updated_at: new Date().toISOString() })
      .eq("key", data.key);
    if (error) throw new Error(error.message);
    return { message: "Setting saved." };
  });

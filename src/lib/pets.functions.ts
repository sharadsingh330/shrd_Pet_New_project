import { createServerFn } from "@tanstack/react-start";
import { createClient } from "@supabase/supabase-js";
import { z } from "zod";
import type { Database } from "@/integrations/supabase/types";

function publicClient() {
  const key = process.env["SUPABASE_PUBLISHABLE_KEY"]!;
  return createClient<Database>(process.env["SUPABASE_URL"]!, key, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: {
      fetch: (input, init) => {
        const h = new Headers(init?.headers);
        if (key.startsWith("sb_") && h.get("Authorization") === `Bearer ${key}`) {
          h.delete("Authorization");
        }
        h.set("apikey", key);
        return fetch(input, { ...init, headers: h });
      },
    },
  });
}

const PET_COLUMNS =
  "id, name, species, breed, age_label, size, temperament, city, description, image_url, image_key, match_score, adoption_status, shelter_id, created_at";

export type PublicPet = {
  id: string;
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
  adoption_status: string;
  shelter_id: string;
  shelter_name: string;
};

export const listPublicPets = createServerFn({ method: "GET" }).handler(async () => {
  const supabase = publicClient();
  const [pets, shelters] = await Promise.all([
    supabase
      .from("pets")
      .select(PET_COLUMNS)
      .eq("listing_status", "approved")
      .order("match_score", { ascending: false }),
    supabase.from("shelters").select("id, name, city, verified"),
  ]);
  const names = new Map((shelters.data ?? []).map((s) => [s.id, s.name]));
  const rows: PublicPet[] = (pets.data ?? []).map((p) => ({
    ...p,
    shelter_name: names.get(p.shelter_id) ?? "Partner shelter",
  }));
  return {
    pets: rows,
    shelterCount: (shelters.data ?? []).length,
  };
});

export const getPublicPet = createServerFn({ method: "GET" })
  .inputValidator((data) => z.object({ id: z.string().uuid() }).parse(data))
  .handler(async ({ data }) => {
    const supabase = publicClient();
    const { data: pet } = await supabase
      .from("pets")
      .select(PET_COLUMNS)
      .eq("id", data.id)
      .eq("listing_status", "approved")
      .maybeSingle();
    if (!pet) return null;
    const { data: shelter } = await supabase
      .from("shelters")
      .select("id, name, city, email, verified")
      .eq("id", pet.shelter_id)
      .maybeSingle();
    return { pet, shelter };
  });

export const listPublicShelters = createServerFn({ method: "GET" }).handler(async () => {
  const supabase = publicClient();
  const { data } = await supabase.from("shelters").select("id, name, city, verified, email").order("name");
  return data ?? [];
});

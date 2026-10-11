import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  adminDeleteUser, adminOverview, adminSetRole, adminUpdateSetting, createPet, decideApplication,
  deletePet, getMyContext, listMessages, myApplications, sendMessage, shelterOverview, updatePetStatus,
} from "@/lib/platform.functions";
import { SiteHeader } from "@/components/site-header";
import { Button, Card, Input, Label, Notice, SectionTitle, Select, StatusPill, Textarea } from "@/components/ui-kit";
import { petImage } from "@/lib/pet-images";
import { supabase } from "@/integrations/supabase/client";

export const Route = createFileRoute("/_authenticated/dashboard")({
  head: () => ({
    meta: [
      { title: "Dashboard — Pawlift" },
      { name: "description", content: "Manage your Pawlift applications, listings and settings." },
      { property: "og:title", content: "Dashboard — Pawlift" },
      { property: "og:description", content: "Your Pawlift dashboard." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Dashboard,
});

function useAction() {
  const qc = useQueryClient();
  const [note, setNote] = useState<{ text: string; tone: "ok" | "error" }>({ text: "", tone: "ok" });
  async function run(fn: () => Promise<{ message: string }>) {
    try {
      const r = await fn();
      setNote({ text: r.message, tone: "ok" });
      qc.invalidateQueries();
    } catch (e) {
      setNote({ text: (e as Error).message, tone: "error" });
    }
  }
  return { note, run };
}

function Dashboard() {
  const ctx = useQuery({ queryKey: ["me"], queryFn: () => getMyContext() });
  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />
      <main className="mx-auto max-w-6xl space-y-8 px-6 py-10">
        {ctx.isLoading ? <p className="text-muted-foreground">Loading…</p> : null}
        {ctx.error ? <Notice tone="error" text={(ctx.error as Error).message} /> : null}
        {ctx.data ? (
          <>
            <div>
              <p className="text-xs uppercase tracking-widest text-muted-foreground">{ctx.data.role} dashboard</p>
              <h1 className="font-display text-4xl font-bold tracking-tight">
                Hi {ctx.data.profile?.full_name || "there"}
              </h1>
            </div>
            {ctx.data.role === "admin" ? <AdminPanel /> : ctx.data.role === "shelter" ? <ShelterPanel /> : <AdopterPanel />}
          </>
        ) : null}
      </main>
    </div>
  );
}

/* --------------------------------- adopter -------------------------------- */

function AdopterPanel() {
  const apps = useQuery({ queryKey: ["my-apps"], queryFn: () => myApplications() });
  const [open, setOpen] = useState<string | null>(null);
  return (
    <Card className="space-y-4">
      <SectionTitle aside={`${apps.data?.length ?? 0} total`}>My applications</SectionTitle>
      {apps.data?.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          No applications yet. <Link to="/" className="text-primary">Browse pets</Link>
        </p>
      ) : null}
      {apps.data?.map((a) => (
        <div key={a.id} className="rounded-xl border border-border p-4">
          <div className="flex items-center gap-4">
            {a.pets ? <img src={petImage(a.pets)} alt="" className="h-14 w-14 rounded-lg object-cover" /> : null}
            <div className="flex-1">
              <p className="font-semibold">{a.pets?.name ?? "Pet"}</p>
              <p className="text-xs text-muted-foreground">{a.pets?.breed}</p>
            </div>
            <StatusPill status={a.status} />
            <Button variant="outline" onClick={() => setOpen(open === a.id ? null : a.id)}>Messages</Button>
          </div>
          {open === a.id ? <Thread applicationId={a.id} /> : null}
        </div>
      ))}
    </Card>
  );
}

function Thread({ applicationId }: { applicationId: string }) {
  const msgs = useQuery({
    queryKey: ["msgs", applicationId],
    queryFn: () => listMessages({ data: { application_id: applicationId } }),
  });
  const [body, setBody] = useState("");
  const { note, run } = useAction();
  return (
    <div className="mt-4 space-y-3 border-t border-border pt-4">
      {msgs.data?.length === 0 ? <p className="text-sm text-muted-foreground">No messages yet.</p> : null}
      {msgs.data?.map((m) => (
        <p key={m.id} className="text-sm">
          <span className="font-semibold">{m.sender_name}:</span> {m.body}
        </p>
      ))}
      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          run(() => sendMessage({ data: { application_id: applicationId, body } })).then(() => setBody(""));
        }}
      >
        <Input value={body} required placeholder="Write a message…" onChange={(e) => setBody(e.target.value)} />
        <Button type="submit">Send</Button>
      </form>
      <Notice text={note.tone === "error" ? note.text : ""} tone="error" />
    </div>
  );
}

/* --------------------------------- shelter -------------------------------- */

const emptyPet = { name: "", species: "Dog", breed: "", age_label: "", size: "Medium", temperament: "", description: "", image_url: "" };

function ShelterPanel() {
  const ov = useQuery({ queryKey: ["shelter"], queryFn: () => shelterOverview() });
  const [pet, setPet] = useState(emptyPet);
  const [open, setOpen] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const { note, run } = useAction();
  if (!ov.data) return <p className="text-muted-foreground">Loading…</p>;
  if (!ov.data.shelter) return <Notice tone="error" text="No shelter is linked to this account." />;
  const set = (k: keyof typeof emptyPet) => (e: { target: { value: string } }) => setPet({ ...pet, [k]: e.target.value });

  return (
    <div className="space-y-6">
      <Notice text={note.text} tone={note.tone} />
      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="Listings" value={ov.data.pets.length} />
        <Stat label="Applications" value={ov.data.applications.length} />
        <Stat label="Adopted" value={ov.data.pets.filter((p) => p.adoption_status === "adopted").length} />
      </div>
      <Card className="space-y-4">
        <SectionTitle>Add a pet</SectionTitle>
        <form
          className="grid gap-3 sm:grid-cols-2"
          onSubmit={(e) => {
            e.preventDefault();
            run(() => createPet({ data: pet })).then(() => setPet(emptyPet));
          }}
        >
          <label><Label>Name</Label><Input required value={pet.name} onChange={set("name")} /></label>
          <label><Label>Species</Label>
            <Select value={pet.species} onChange={set("species")}><option>Dog</option><option>Cat</option><option>Other</option></Select></label>
          <label><Label>Breed</Label><Input value={pet.breed} onChange={set("breed")} /></label>
          <label><Label>Age</Label><Input value={pet.age_label} placeholder="2 years" onChange={set("age_label")} /></label>
          <label><Label>Size</Label>
            <Select value={pet.size} onChange={set("size")}><option>Small</option><option>Medium</option><option>Large</option></Select></label>
          <label><Label>Temperament</Label><Input value={pet.temperament} onChange={set("temperament")} /></label>
          <label className="sm:col-span-2"><Label>Photo (optional)</Label>
            <input
              type="file"
              accept="image/*"
              className="block w-full text-sm text-muted-foreground file:mr-3 file:rounded-lg file:border-0 file:bg-primary file:px-4 file:py-2 file:font-semibold file:text-primary-foreground"
              onChange={async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                if (file.size > 5 * 1024 * 1024) { alert("Photo must be under 5 MB."); return; }
                setUploading(true);
                try {
                  const { data: u } = await supabase.auth.getUser();
                  const path = `${u.user?.id}/${Date.now()}-${file.name.replace(/[^a-zA-Z0-9.]/g, "_")}`;
                  const up = await supabase.storage.from("pet-photos").upload(path, file, { contentType: file.type });
                  if (up.error) throw up.error;
                  const signed = await supabase.storage.from("pet-photos").createSignedUrl(path, 60 * 60 * 24 * 365 * 10);
                  if (signed.error) throw signed.error;
                  setPet((p) => ({ ...p, image_url: signed.data.signedUrl }));
                } catch (err) {
                  alert(err instanceof Error ? err.message : "Upload failed");
                } finally {
                  setUploading(false);
                }
              }}
            />
            {uploading ? <span className="text-xs text-muted-foreground">Uploading…</span> : null}
            {pet.image_url ? <img src={pet.image_url} alt="Preview" className="mt-2 h-32 rounded-xl object-cover" /> : null}
          </label>
          <label className="sm:col-span-2"><Label>Description</Label><Textarea value={pet.description} onChange={set("description")} /></label>
          <Button type="submit" className="sm:col-span-2" disabled={uploading}>Add pet</Button>
        </form>
      </Card>
      <Card className="space-y-3">
        <SectionTitle>My listings</SectionTitle>
        {ov.data.pets.map((p) => (
          <div key={p.id} className="flex flex-wrap items-center gap-3 rounded-xl border border-border p-3">
            <img src={petImage(p)} alt="" className="h-12 w-12 rounded-lg object-cover" />
            <p className="flex-1 font-semibold">{p.name}</p>
            <StatusPill status={p.listing_status} />
            <Select
              className="w-36"
              value={p.adoption_status}
              onChange={(e) => run(() => updatePetStatus({ data: { pet_id: p.id, adoption_status: e.target.value as "available" } }))}
            >
              <option value="available">Available</option><option value="pending">Pending</option><option value="adopted">Adopted</option>
            </Select>
            <Button variant="danger" onClick={() => confirm(`Remove ${p.name}?`) && run(() => deletePet({ data: { pet_id: p.id } }))}>Remove</Button>
          </div>
        ))}
      </Card>
      <Card className="space-y-3">
        <SectionTitle>Applications</SectionTitle>
        {ov.data.applications.length === 0 ? <p className="text-sm text-muted-foreground">No applications yet.</p> : null}
        {ov.data.applications.map((a) => (
          <div key={a.id} className="rounded-xl border border-border p-4">
            <div className="flex flex-wrap items-center gap-3">
              <div className="flex-1">
                <p className="font-semibold">{a.adopter_name} → {a.pets?.name}</p>
                <p className="text-xs text-muted-foreground">{a.adopter_email} · {a.housing}</p>
                {a.note ? <p className="mt-1 text-sm">{a.note}</p> : null}
              </div>
              <StatusPill status={a.status} />
              <Button variant="outline" onClick={() => run(() => decideApplication({ data: { application_id: a.id, status: "approved" } }))}>Approve</Button>
              <Button variant="danger" onClick={() => run(() => decideApplication({ data: { application_id: a.id, status: "rejected" } }))}>Reject</Button>
              <Button variant="ghost" onClick={() => setOpen(open === a.id ? null : a.id)}>Messages</Button>
            </div>
            {open === a.id ? <Thread applicationId={a.id} /> : null}
          </div>
        ))}
      </Card>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <Card>
      <p className="text-xs uppercase tracking-widest text-muted-foreground">{label}</p>
      <p className="mt-1 font-display text-3xl font-bold">{value}</p>
    </Card>
  );
}

/* ---------------------------------- admin --------------------------------- */

function AdminPanel() {
  const ov = useQuery({ queryKey: ["admin"], queryFn: () => adminOverview() });
  const { note, run } = useAction();
  if (ov.error) return <Notice tone="error" text={(ov.error as Error).message} />;
  if (!ov.data) return <p className="text-muted-foreground">Loading…</p>;
  const a = ov.data.analytics;
  return (
    <div className="space-y-6">
      <Notice text={note.text} tone={note.tone} />
      <div className="grid gap-4 sm:grid-cols-4">
        <Stat label="Users" value={a.users} />
        <Stat label="Listings" value={a.listings} />
        <Stat label="Applications" value={a.applications} />
        <Stat label="Adopted" value={a.adopted} />
      </div>
      <Card className="space-y-3">
        <SectionTitle aside={`${a.pendingListings} pending`}>Listings</SectionTitle>
        {ov.data.pets.map((p) => (
          <div key={p.id} className="flex flex-wrap items-center gap-3 rounded-xl border border-border p-3">
            <p className="flex-1 font-semibold">{p.name} <span className="text-xs text-muted-foreground">· {p.shelters?.name}</span></p>
            <StatusPill status={p.listing_status} />
            <Button variant="outline" onClick={() => run(() => updatePetStatus({ data: { pet_id: p.id, listing_status: "approved" } }))}>Approve</Button>
            <Button variant="danger" onClick={() => run(() => updatePetStatus({ data: { pet_id: p.id, listing_status: "rejected" } }))}>Reject</Button>
          </div>
        ))}
      </Card>
      <Card className="space-y-3">
        <SectionTitle>Users</SectionTitle>
        {ov.data.users.map((u) => (
          <div key={u.id} className="flex flex-wrap items-center gap-3 rounded-xl border border-border p-3">
            <div className="flex-1">
              <p className="font-semibold">{u.full_name || "—"}</p>
              <p className="text-xs text-muted-foreground">{u.email}</p>
            </div>
            <Select
              className="w-36"
              value={u.role}
              onChange={(e) => run(() => adminSetRole({ data: { user_id: u.id, role: e.target.value as "adopter" } }))}
            >
              <option value="adopter">Adopter</option><option value="shelter">Shelter</option><option value="admin">Admin</option>
            </Select>
            <Button variant="danger" onClick={() => confirm("Delete this user?") && run(() => adminDeleteUser({ data: { user_id: u.id } }))}>Delete</Button>
          </div>
        ))}
      </Card>
      <Card className="space-y-3">
        <SectionTitle>Settings</SectionTitle>
        {ov.data.settings.map((s) => (
          <SettingRow key={s.key} k={s.key} v={String(s.value ?? "")} onSave={(value) => run(() => adminUpdateSetting({ data: { key: s.key, value } }))} />
        ))}
      </Card>
    </div>
  );
}

function SettingRow({ k, v, onSave }: { k: string; v: string; onSave: (v: string) => void }) {
  const [val, setVal] = useState(v);
  return (
    <div className="flex items-center gap-3">
      <span className="w-48 text-sm font-medium">{k}</span>
      <Input value={val} onChange={(e) => setVal(e.target.value)} />
      <Button variant="outline" onClick={() => onSave(val)}>Save</Button>
    </div>
  );
}

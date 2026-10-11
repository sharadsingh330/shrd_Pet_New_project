import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { queryOptions, useSuspenseQuery } from "@tanstack/react-query";
import { useState } from "react";
import { getPublicPet } from "@/lib/pets.functions";
import { applyForPet } from "@/lib/platform.functions";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { Button, Card, Input, Label, Notice, StatusPill, Textarea } from "@/components/ui-kit";
import { petImage } from "@/lib/pet-images";
import { useSession } from "@/hooks/useSession";

const petQuery = (id: string) =>
  queryOptions({ queryKey: ["pet", id], queryFn: () => getPublicPet({ data: { id } }) });

export const Route = createFileRoute("/pets/$petId")({
  loader: async ({ context, params }) => {
    const res = await context.queryClient.ensureQueryData(petQuery(params.petId));
    if (!res) throw notFound();
    return res;
  },
  head: ({ loaderData }) => {
    const name = loaderData?.pet.name ?? "Pet";
    const desc = `Meet ${name}, a ${loaderData?.pet.breed ?? "shelter pet"} ready for adoption on Pawlift.`;
    return {
      meta: [
        { title: `${name} — Adopt on Pawlift` },
        { name: "description", content: desc },
        { property: "og:title", content: `${name} — Adopt on Pawlift` },
        { property: "og:description", content: desc },
        { property: "og:type", content: "article" },
        { name: "twitter:card", content: "summary_large_image" },
      ],
    };
  },
  errorComponent: () => <p className="p-10 text-center">Couldn't load this pet.</p>,
  notFoundComponent: () => (
    <div className="p-10 text-center">
      <p>This pet isn't listed.</p>
      <Link to="/" className="text-primary">Back to pets</Link>
    </div>
  ),
  component: PetDetail,
});

function PetDetail() {
  const { petId } = Route.useParams();
  const { data } = useSuspenseQuery(petQuery(petId));
  const { session } = useSession();
  const [form, setForm] = useState({ housing: "", experience: "", note: "" });
  const [msg, setMsg] = useState<{ text: string; tone: "ok" | "error" }>({ text: "", tone: "ok" });
  const [busy, setBusy] = useState(false);
  if (!data) return null;
  const { pet, shelter } = data;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      const r = await applyForPet({ data: { pet_id: pet.id, ...form } });
      setMsg({ text: r.message, tone: "ok" });
    } catch (err) {
      setMsg({ text: (err as Error).message, tone: "error" });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />
      <main className="mx-auto grid max-w-6xl gap-10 px-6 py-12 lg:grid-cols-2">
        <img src={petImage(pet)} alt={pet.name} className="aspect-square w-full rounded-3xl object-cover" />
        <div className="space-y-6">
          <div>
            <StatusPill status={pet.adoption_status} />
            <h1 className="mt-3 font-display text-5xl font-bold tracking-tight">{pet.name}</h1>
            <p className="mt-2 text-muted-foreground">
              {pet.breed} · {pet.age_label} · {pet.size} · {pet.city}
            </p>
          </div>
          <p className="leading-relaxed">{pet.description}</p>
          {shelter ? (
            <p className="text-sm text-muted-foreground">
              Listed by <span className="font-semibold text-foreground">{shelter.name}</span>, {shelter.city}
            </p>
          ) : null}
          <Card>
            <h2 className="font-display text-lg font-bold">Apply to adopt</h2>
            {!session ? (
              <p className="mt-3 text-sm text-muted-foreground">
                <Link to="/auth" className="font-semibold text-primary">Sign in</Link> to apply for {pet.name}.
              </p>
            ) : (
              <form onSubmit={submit} className="mt-4 space-y-4">
                <label className="block">
                  <Label>Housing</Label>
                  <Input required value={form.housing} placeholder="House with yard, apartment…" onChange={(e) => setForm({ ...form, housing: e.target.value })} />
                </label>
                <label className="block">
                  <Label>Pet experience</Label>
                  <Input value={form.experience} onChange={(e) => setForm({ ...form, experience: e.target.value })} />
                </label>
                <label className="block">
                  <Label>Note to shelter</Label>
                  <Textarea value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
                </label>
                <Notice text={msg.text} tone={msg.tone} />
                <Button type="submit" disabled={busy}>{busy ? "Sending…" : "Submit application"}</Button>
              </form>
            )}
          </Card>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

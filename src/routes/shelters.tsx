import { createFileRoute } from "@tanstack/react-router";
import { queryOptions, useSuspenseQuery } from "@tanstack/react-query";
import { listPublicShelters } from "@/lib/pets.functions";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { Card, StatusPill } from "@/components/ui-kit";

const sheltersQuery = queryOptions({ queryKey: ["public-shelters"], queryFn: () => listPublicShelters() });

export const Route = createFileRoute("/shelters")({
  loader: ({ context }) => context.queryClient.ensureQueryData(sheltersQuery),
  head: () => ({
    meta: [
      { title: "Partner shelters — Pawlift" },
      { name: "description", content: "Meet the verified rescue shelters listing adoptable pets on Pawlift." },
      { property: "og:title", content: "Partner shelters — Pawlift" },
      { property: "og:description", content: "Verified rescue shelters listing adoptable pets." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  errorComponent: () => <p className="p-10 text-center">Couldn't load shelters.</p>,
  notFoundComponent: () => <p className="p-10 text-center">Not found.</p>,
  component: Shelters,
});

function Shelters() {
  const { data } = useSuspenseQuery(sheltersQuery);
  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />
      <main className="mx-auto max-w-5xl px-6 py-14">
        <h1 className="font-display text-4xl font-bold tracking-tight">Partner shelters</h1>
        <p className="mt-2 text-muted-foreground">Every listing comes from one of these rescues.</p>
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          {data.map((s) => (
            <Card key={s.id}>
              <div className="flex items-center justify-between gap-3">
                <h2 className="font-display text-xl font-bold">{s.name}</h2>
                {s.verified ? <StatusPill status="approved" /> : <StatusPill status="pending" />}
              </div>
              <p className="mt-2 text-sm text-muted-foreground">{s.city}</p>
              {s.email ? <p className="mt-1 text-sm">{s.email}</p> : null}
            </Card>
          ))}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

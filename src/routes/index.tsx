import { createFileRoute, Link } from "@tanstack/react-router";
import { queryOptions, useSuspenseQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { listPublicPets } from "@/lib/pets.functions";
import { PetCard } from "@/components/pet-card";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { Input } from "@/components/ui-kit";
import { heroImage } from "@/lib/pet-images";
import greyhound from "@/assets/pet-dog-winston.jpg";
import cat from "@/assets/pet-cat-juniper.jpg";
import beagle from "@/assets/pet-dog-biscuit.jpg";

const petsQuery = queryOptions({
  queryKey: ["public-pets"],
  queryFn: () => listPublicPets(),
});

export const Route = createFileRoute("/")({
  loader: ({ context }) => context.queryClient.ensureQueryData(petsQuery),
  head: () => ({
    meta: [
      { title: "Pawlift — Adopt a shelter pet near you" },
      {
        name: "description",
        content:
          "Browse verified adoptable pets from partner shelters, apply in minutes, and track your adoption application.",
      },
      { property: "og:title", content: "Pawlift — Adopt a shelter pet near you" },
      {
        property: "og:description",
        content: "Verified shelter pets, fast applications, and one place to track every adoption.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

const zigzagSteps = [
  {
    img: greyhound,
    alt: "A greyhound relaxing at a shelter",
    kicker: "Step 1",
    title: "Find your match",
    body: "Search by breed, city, or temperament. Every listing is reviewed by our team before it goes live, so what you see is what you meet.",
  },
  {
    img: cat,
    alt: "A curious cat waiting for a new home",
    kicker: "Step 2",
    title: "Apply in minutes",
    body: "One short form tells the shelter about your home and pet experience. No paperwork, no waiting rooms — just an honest hello.",
  },
  {
    img: beagle,
    alt: "A beagle enjoying the sunshine",
    kicker: "Step 3",
    title: "Bring them home",
    body: "Message the shelter, get approved, and welcome your new best friend. Track every step from your dashboard.",
  },
];

function Index() {
  const { data } = useSuspenseQuery(petsQuery);
  const [term, setTerm] = useState("");

  const featured = useMemo(() => {
    const q = term.trim().toLowerCase();
    const list = q
      ? data.pets.filter((p) =>
          [p.name, p.breed, p.species, p.city].join(" ").toLowerCase().includes(q),
        )
      : data.pets;
    return list;
  }, [data.pets, term]);

  const hero = data.pets[0];

  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />

      <section className="relative overflow-hidden border-b border-border">
        <div className="bg-hero-glow absolute inset-0" />
        <div className="a-drift absolute -left-24 top-6 size-72 rounded-full bg-primary/15 blur-3xl" />
        <div className="a-drift2 absolute right-10 top-24 size-64 rounded-full bg-accent/25 blur-3xl" />

        <div className="relative mx-auto grid max-w-7xl items-center gap-10 px-6 pb-24 pt-16 lg:grid-cols-2 lg:gap-14">
          <div className="a-rise">
            <span className="inline-flex items-center gap-2 rounded-full border border-input bg-card px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              <span className="a-pulse size-2 rounded-full bg-primary" />
              {data.pets.length} pets looking right now
            </span>
            <h1 className="mt-5 font-display text-6xl font-bold leading-[0.95] tracking-tight sm:text-7xl">
              Find your <span className="text-primary">forever</span> match.
            </h1>
            <p className="mt-5 max-w-md text-base leading-relaxed text-muted-foreground">
              Browse verified adoptable pets, apply in minutes, and meet your new best friend.
              Shelters list, admins keep it safe, you get home.
            </p>
            <div className="mt-8 flex max-w-md flex-col gap-3 sm:flex-row">
              <Input
                value={term}
                onChange={(e) => setTerm(e.target.value)}
                placeholder="Search by breed, city, or name"
              />
              <a
                href="#available"
                className="shrink-0 rounded-xl bg-primary px-6 py-3 text-center text-sm font-bold text-primary-foreground transition hover:brightness-110"
              >
                Browse pets
              </a>
            </div>
            <div className="mt-8 flex gap-8">
              <div>
                <div className="font-display text-2xl font-bold text-primary">{data.pets.length}</div>
                <div className="text-xs uppercase tracking-widest text-muted-foreground">Listed</div>
              </div>
              <div>
                <div className="font-display text-2xl font-bold text-primary">
                  {data.shelterCount}
                </div>
                <div className="text-xs uppercase tracking-widest text-muted-foreground">
                  Shelters
                </div>
              </div>
              <div>
                <div className="font-display text-2xl font-bold text-primary">4.9</div>
                <div className="text-xs uppercase tracking-widest text-muted-foreground">
                  Match score
                </div>
              </div>
            </div>
          </div>

          <div className="a-rise relative" style={{ animationDelay: ".12s" }}>
            <div className="a-tilt absolute -left-8 top-16 z-0 size-56 rounded-2xl border border-border bg-card" />
            <div className="a-tilt2 absolute -right-6 bottom-4 z-0 size-64 rounded-2xl border border-border bg-card" />
            <div className="relative z-10 overflow-hidden rounded-3xl border border-border bg-card p-2 shadow-xl">
              <div className="overflow-hidden rounded-2xl">
                <img
                  src={heroImage}
                  alt="A golden retriever waiting on a sunny porch"
                  width={912}
                  height={1024}
                  className="aspect-[4/5] w-full object-cover"
                />
              </div>
              {hero ? (
                <div className="flex items-center gap-3 px-3 py-3">
                  <div className="grid size-11 shrink-0 place-items-center rounded-full bg-surface text-[9px] uppercase tracking-widest text-muted-foreground">
                    {hero.species}
                  </div>
                  <div className="min-w-0">
                    <div className="font-display text-base font-bold">{hero.name}</div>
                    <div className="truncate text-xs text-muted-foreground">
                      {hero.breed} · {hero.age_label} · {hero.city}
                    </div>
                  </div>
                  <span className="ml-auto shrink-0 rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest text-primary">
                    {hero.match_score}% match
                  </span>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-6 py-20">
        <div className="space-y-16">
          {zigzagSteps.map((step, i) => (
            <div
              key={step.kicker}
              className={`flex flex-col items-center gap-10 md:flex-row ${
                i % 2 === 1 ? "md:flex-row-reverse" : ""
              }`}
            >
              <img
                src={step.img}
                alt={step.alt}
                loading="lazy"
                className="aspect-[4/3] w-full max-w-md rounded-3xl border border-border object-cover"
              />
              <div className="max-w-md">
                <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-bold uppercase tracking-widest text-primary">
                  {step.kicker}
                </span>
                <h2 className="mt-4 font-display text-3xl font-bold tracking-tight">{step.title}</h2>
                <p className="mt-3 leading-relaxed text-muted-foreground">{step.body}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section id="available" className="mx-auto max-w-7xl px-6 pb-24">
        <div className="mb-8 flex items-end justify-between">
          <h2 className="font-display text-2xl font-bold tracking-tight">Ready to meet you</h2>
          <Link to="/shelters" className="text-sm font-semibold text-primary hover:underline">
            See partner shelters →
          </Link>
        </div>
        {featured.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No pets match that search yet. Try another breed or city.
          </p>
        ) : (
          <div className="grid gap-6 md:grid-cols-3">
            {featured.map((pet) => (
              <PetCard key={pet.id} pet={pet} />
            ))}
          </div>
        )}
      </section>

      <SiteFooter />
    </div>
  );
}

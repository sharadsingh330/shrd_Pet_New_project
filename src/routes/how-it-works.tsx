import { createFileRoute, Link } from "@tanstack/react-router";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import golden from "@/assets/pet-hero.jpg";
import beagle from "@/assets/pet-dog-biscuit.jpg";
import cat from "@/assets/pet-cat-juniper.jpg";

export const Route = createFileRoute("/how-it-works")({
  head: () => ({
    meta: [
      { title: "How Pawlift adoption works" },
      {
        name: "description",
        content:
          "Three roles, one flow: shelters list pets, admins approve listings, adopters apply and track status.",
      },
      { property: "og:title", content: "How Pawlift adoption works" },
      {
        property: "og:description",
        content: "Shelters list, admins approve, adopters apply and track every step.",
      },
      { property: "og:type", content: "article" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: HowItWorks,
});

const steps = [
  {
    role: "Adopter",
    img: golden,
    alt: "A golden retriever waiting for a home",
    title: "Browse and apply",
    body: "Search by breed, city or size, open a pet, and send one application with your housing and experience. Track status from your dashboard.",
  },
  {
    role: "Shelter",
    img: beagle,
    alt: "A beagle resting after a walk",
    title: "List and review",
    body: "Add pets with photos and descriptions, review incoming applications, message adopters, and mark pets adopted.",
  },
  {
    role: "Admin",
    img: cat,
    alt: "A cat keeping watch over the shelter",
    title: "Approve and oversee",
    body: "Approve or reject submitted listings, manage user accounts and roles, adjust platform settings, and watch the analytics.",
  },
];

function HowItWorks() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />
      <section className="relative overflow-hidden border-b border-border">
        <div className="bg-hero-glow absolute inset-0" />
        <div className="relative mx-auto max-w-7xl px-6 py-20">
          <h1 className="font-display text-5xl font-bold tracking-tight">How it works</h1>
          <p className="mt-4 max-w-xl text-muted-foreground">
            Pawlift keeps three jobs separate and honest: shelters list real animals, admins keep
            the listings clean, adopters get a clear answer.
          </p>
        </div>
      </section>

      <section className="mx-auto max-w-7xl space-y-16 px-6 py-16">
        {steps.map((step, i) => (
          <div
            key={step.role}
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
                {step.role}
              </span>
              <h2 className="mt-4 font-display text-2xl font-bold tracking-tight">{step.title}</h2>
              <p className="mt-3 leading-relaxed text-muted-foreground">{step.body}</p>
            </div>
          </div>
        ))}
        <Link
          to="/auth"
          className="inline-block rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground transition hover:brightness-110"
        >
          Create your account
        </Link>
      </section>
      <SiteFooter />
    </div>
  );
}

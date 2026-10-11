import { Link } from "@tanstack/react-router";
import type { PublicPet } from "@/lib/pets.functions";
import { petImage } from "@/lib/pet-images";

export function PetCard({ pet }: { pet: PublicPet }) {
  return (
    <div className="group overflow-hidden rounded-2xl border border-border bg-card">
      <div className="relative">
        <img
          src={petImage(pet)}
          alt={`${pet.name}, ${pet.breed}`}
          loading="lazy"
          width={960}
          height={704}
          className="aspect-[7/5] w-full object-cover"
        />
        <div className="pointer-events-none absolute inset-x-0 top-0 h-1/2 -skew-y-6 bg-gradient-to-r from-primary/15 to-transparent" />
        <span className="absolute left-3 top-3 rounded-full bg-background/70 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-widest text-foreground/70 backdrop-blur">
          {pet.species}
        </span>
      </div>
      <div className="p-5">
        <div className="flex items-baseline justify-between">
          <h3 className="font-display text-lg font-bold">{pet.name}</h3>
          <span className="text-xs font-medium text-muted-foreground">{pet.city}</span>
        </div>
        <p className="mt-1 text-sm text-foreground/50">
          {pet.breed} · {pet.age_label} · {pet.temperament}
        </p>
        <div className="mt-4 flex items-center gap-3">
          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-foreground/10">
            <div className="bg-meter h-full rounded-full" style={{ width: `${pet.match_score}%` }} />
          </div>
          <span className="font-display text-sm font-bold text-primary">{pet.match_score}%</span>
        </div>
        <Link
          to="/pets/$petId"
          params={{ petId: pet.id }}
          className="mt-4 block w-full rounded-lg border border-input bg-foreground/5 py-2.5 text-center text-sm font-semibold transition group-hover:border-primary/50 group-hover:bg-primary group-hover:text-primary-foreground"
        >
          Apply now
        </Link>
      </div>
    </div>
  );
}

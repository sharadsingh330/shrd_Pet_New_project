import golden from "@/assets/pet-hero.jpg";
import cat from "@/assets/pet-cat-juniper.jpg";
import beagle from "@/assets/pet-dog-biscuit.jpg";
import greyhound from "@/assets/pet-dog-winston.jpg";

const library: Record<string, string> = {
  golden,
  cat,
  beagle,
  greyhound,
  dog: greyhound,
};

export const heroImage = golden;

export function petImage(pet: { image_url?: string | null; image_key?: string | null }): string {
  if (pet.image_url && pet.image_url.startsWith("http")) return pet.image_url;
  return library[pet.image_key ?? "dog"] ?? golden;
}

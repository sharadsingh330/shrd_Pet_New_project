import { Link } from "@tanstack/react-router";

export function SiteFooter() {
  return (
    <footer className="border-t border-border bg-background">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-6 py-10 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2.5">
          <span className="grid size-8 place-items-center rounded-lg bg-primary font-display text-sm font-bold text-primary-foreground">
            P
          </span>
          <span className="font-display font-bold">Pawlift</span>
          <span className="text-sm text-muted-foreground">— every pet deserves a home.</span>
        </div>
        <div className="flex gap-6 text-sm text-muted-foreground">
          <Link to="/how-it-works" className="hover:text-foreground">
            How it works
          </Link>
          <Link to="/shelters" className="hover:text-foreground">
            Shelters
          </Link>
          <Link to="/auth" className="hover:text-foreground">
            Sign in
          </Link>
        </div>
      </div>
    </footer>
  );
}

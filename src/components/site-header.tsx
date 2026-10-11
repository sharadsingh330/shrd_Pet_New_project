import { Link, useNavigate } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { supabase } from "@/integrations/supabase/client";
import { useSession } from "@/hooks/useSession";
import { Button } from "@/components/ui-kit";
import { ThemeToggle } from "@/components/theme-toggle";

export function SiteHeader() {
  const { session } = useSession();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  async function signOut() {
    await queryClient.cancelQueries();
    queryClient.clear();
    await supabase.auth.signOut();
    navigate({ to: "/auth", replace: true });
  }

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background/70 backdrop-blur-xl">
      <div className="mx-auto flex max-w-7xl items-center gap-4 px-6 py-4">
        <Link to="/" className="flex items-center gap-2.5">
          <span className="grid size-9 place-items-center rounded-lg bg-primary font-display text-lg font-bold text-primary-foreground">
            P
          </span>
          <span className="font-display text-lg font-bold tracking-tight">Pawlift</span>
        </Link>
        <nav className="ml-6 hidden items-center gap-1 md:flex">
          <Link
            to="/"
            className="rounded-md px-3 py-1.5 text-sm font-medium text-foreground/60 transition hover:bg-foreground/5 hover:text-foreground"
          >
            Browse
          </Link>
          <Link
            to="/shelters"
            className="rounded-md px-3 py-1.5 text-sm font-medium text-foreground/60 transition hover:bg-foreground/5 hover:text-foreground"
          >
            Shelters
          </Link>
          <Link
            to="/how-it-works"
            className="rounded-md px-3 py-1.5 text-sm font-medium text-foreground/60 transition hover:bg-foreground/5 hover:text-foreground"
          >
            How it works
          </Link>
        </nav>
        {session ? (
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <Link
              to="/dashboard"
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition hover:brightness-110"
            >
              Dashboard
            </Link>
            <Button variant="ghost" onClick={signOut}>
              Sign out
            </Button>
          </div>
        ) : (
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <Link
              to="/auth"
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition hover:brightness-110"
            >
              Sign in
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}

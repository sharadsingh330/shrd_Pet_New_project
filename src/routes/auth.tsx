import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { supabase } from "@/integrations/supabase/client";
import { SiteHeader } from "@/components/site-header";
import { Button, Card, Input, Label, Notice, Select } from "@/components/ui-kit";

export const Route = createFileRoute("/auth")({
  head: () => ({
    meta: [
      { title: "Sign in — Pawlift" },
      { name: "description", content: "Sign in or create a Pawlift account as an adopter or shelter." },
      { property: "og:title", content: "Sign in — Pawlift" },
      { property: "og:description", content: "Join Pawlift as an adopter or partner shelter." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: AuthPage,
});

function AuthPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<"in" | "up">("in");
  const [f, setF] = useState({ email: "", password: "", full_name: "", role: "adopter", shelter_name: "", city: "" });
  const [msg, setMsg] = useState<{ text: string; tone: "ok" | "error" }>({ text: "", tone: "ok" });
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setMsg({ text: "", tone: "ok" });
    if (mode === "in") {
      const { error } = await supabase.auth.signInWithPassword({ email: f.email, password: f.password });
      setBusy(false);
      if (error) return setMsg({ text: error.message, tone: "error" });
      navigate({ to: "/dashboard" });
    } else {
      const { data, error } = await supabase.auth.signUp({
        email: f.email,
        password: f.password,
        options: {
          emailRedirectTo: window.location.origin + "/dashboard",
          data: { full_name: f.full_name, role: f.role, shelter_name: f.shelter_name, city: f.city },
        },
      });
      setBusy(false);
      if (error) return setMsg({ text: error.message, tone: "error" });
      if (data.session) navigate({ to: "/dashboard" });
      else setMsg({ text: "Check your email to confirm your account, then sign in.", tone: "ok" });
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <SiteHeader />
      <main className="mx-auto max-w-md px-6 py-14">
        <Card>
          <div className="mb-6 flex gap-2">
            <Button variant={mode === "in" ? "primary" : "ghost"} onClick={() => setMode("in")}>Sign in</Button>
            <Button variant={mode === "up" ? "primary" : "ghost"} onClick={() => setMode("up")}>Create account</Button>
          </div>
          <form onSubmit={submit} className="space-y-4">
            {mode === "up" ? (
              <>
                <label className="block"><Label>Full name</Label>
                  <Input required value={f.full_name} onChange={(e) => setF({ ...f, full_name: e.target.value })} /></label>
                <label className="block"><Label>I am a</Label>
                  <Select value={f.role} onChange={(e) => setF({ ...f, role: e.target.value })}>
                    <option value="adopter">Adopter</option>
                    <option value="shelter">Shelter</option>
                  </Select></label>
                {f.role === "shelter" ? (
                  <label className="block"><Label>Shelter name</Label>
                    <Input required value={f.shelter_name} onChange={(e) => setF({ ...f, shelter_name: e.target.value })} /></label>
                ) : null}
                <label className="block"><Label>City</Label>
                  <Input value={f.city} onChange={(e) => setF({ ...f, city: e.target.value })} /></label>
              </>
            ) : null}
            <label className="block"><Label>Email</Label>
              <Input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></label>
            <label className="block"><Label>Password</Label>
              <Input type="password" required minLength={6} value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></label>
            <Notice text={msg.text} tone={msg.tone} />
            <Button type="submit" className="w-full" disabled={busy}>
              {busy ? "Please wait…" : mode === "in" ? "Sign in" : "Create account"}
            </Button>
          </form>
        </Card>
      </main>
    </div>
  );
}

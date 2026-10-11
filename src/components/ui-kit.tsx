import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

type Variant = "primary" | "ghost" | "outline" | "danger";

const variants: Record<Variant, string> = {
  primary:
    "bg-primary text-primary-foreground font-semibold hover:brightness-110 disabled:opacity-50",
  ghost: "text-foreground/70 hover:bg-foreground/5 hover:text-foreground",
  outline:
    "border border-input bg-foreground/5 text-foreground font-semibold hover:bg-foreground/10 disabled:opacity-50",
  danger: "border border-input text-foreground/70 hover:bg-destructive/15 hover:text-foreground",
};

export function Button({
  variant = "primary",
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      {...props}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm transition disabled:cursor-not-allowed",
        variants[variant],
        className,
      )}
    />
  );
}

const fieldBase =
  "w-full rounded-xl border border-input bg-foreground/5 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground outline-none transition focus:border-ring";

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn(fieldBase, className)} />;
}

export function Textarea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={cn(fieldBase, "min-h-24 resize-y", className)} />;
}

export function Select({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cn(fieldBase, "appearance-none", className)} />;
}

export function Label({ children }: { children: ReactNode }) {
  return (
    <span className="mb-1.5 block text-xs font-medium uppercase tracking-widest text-muted-foreground">
      {children}
    </span>
  );
}

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <div className={cn("rounded-2xl border border-border bg-card p-6", className)}>{children}</div>
  );
}

export function SectionTitle({ children, aside }: { children: ReactNode; aside?: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <h3 className="font-display text-lg font-bold tracking-tight">{children}</h3>
      {aside ? <span className="text-xs text-muted-foreground">{aside}</span> : null}
    </div>
  );
}

const pillTone: Record<string, string> = {
  approved: "bg-primary/10 text-primary",
  available: "bg-primary/10 text-primary",
  pending: "bg-foreground/10 text-foreground/60",
  rejected: "bg-destructive/20 text-destructive",
  adopted: "bg-success/15 text-success",
};

export function StatusPill({ status }: { status: string }) {
  return (
    <span
      className={cn(
        "rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest",
        pillTone[status] ?? "bg-foreground/10 text-foreground/60",
      )}
    >
      {status}
    </span>
  );
}

export function Notice({ text, tone = "ok" }: { text: string; tone?: "ok" | "error" }) {
  if (!text) return null;
  return (
    <p
      className={cn(
        "rounded-lg border px-3 py-2 text-sm",
        tone === "ok"
          ? "border-primary/30 bg-primary/10 text-primary"
          : "border-destructive/40 bg-destructive/10 text-destructive",
      )}
    >
      {text}
    </p>
  );
}

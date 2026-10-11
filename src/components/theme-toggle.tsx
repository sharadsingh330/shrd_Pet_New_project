import { useEffect, useState } from "react";

/**
 * Light/dark switch. Light is the default; the choice is remembered in
 * localStorage as "pawlift-theme" and applied by the inline script in
 * __root.tsx before paint to avoid a flash of the wrong theme.
 */
export function ThemeToggle() {
  const [dark, setDark] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setDark(document.documentElement.classList.contains("dark"));
    setReady(true);
  }, []);

  function toggle() {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    try {
      localStorage.setItem("pawlift-theme", next ? "dark" : "light");
    } catch {
      /* private mode — ignore */
    }
  }

  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
      className="grid size-9 shrink-0 place-items-center rounded-lg border border-input text-sm transition hover:bg-foreground/5"
    >
      {ready ? (dark ? "☀️" : "🌙") : ""}
    </button>
  );
}

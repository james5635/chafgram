/**
 * Presentational regional loading overlay. Render it as a child of the busy
 * region's `relative` root, outside the `inert` content subtree. The visually
 * hidden live status lives in a permanently mounted sibling of that root.
 */
export function BusyOverlay({ label }: { label: string }) {
  return (
    <div
      aria-hidden="true"
      className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-2 bg-[var(--surface-strong)] backdrop-blur-[2px]"
    >
      <span
        aria-hidden="true"
        className="h-6 w-6 animate-spin rounded-full border-2 border-[var(--lagoon)] border-t-[var(--brand)] motion-reduce:animate-none"
      />
      <span className="text-sm font-semibold text-[var(--sea-ink)]">{label}</span>
    </div>
  )
}

/**
 * Regional loading overlay. Render it as a sibling of the busy region's content
 * inside a `relative` parent, and mark that parent with `aria-busy`.
 */
export function BusyOverlay({ label }: { label: string }) {
  return (
    <div
      role="status"
      aria-live="polite"
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

export default BusyOverlay

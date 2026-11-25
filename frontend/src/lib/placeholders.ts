/** Generate an inline SVG gradient placeholder as a data URI (no network needed). */
export function gradientPlaceholder(opts: {
  colors: [string, string]
  label?: string
  width?: number
  height?: number
}): string {
  const w = opts.width ?? 600
  const h = opts.height ?? 400
  const [a, b] = opts.colors
  const label = opts.label
    ? `<text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-family="Manrope, sans-serif" font-size="24" font-weight="700" fill="rgba(255,255,255,0.9)">${opts.label}</text>`
    : ''
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${w} ${h}"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${a}"/><stop offset="1" stop-color="${b}"/></linearGradient></defs><rect width="${w}" height="${h}" fill="url(#g)"/>${label}</svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

/** Local blue landscape used for the profile cover when no member photo exists. */
export function profileCoverPlaceholder(): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 320"><defs><linearGradient id="sky" x2="0" y2="1"><stop stop-color="#dbeafe"/><stop offset="1" stop-color="#93c5fd"/></linearGradient></defs><rect width="1200" height="320" fill="url(#sky)"/><circle cx="940" cy="66" r="34" fill="#fff" opacity=".78"/><path d="M0 170 185 82l130 74 190-104 190 105 172-77 190 99 143-70v211H0Z" fill="#bfdbfe" opacity=".92"/><path d="m0 224 180-84 166 74 186-94 170 82 174-89 155 79 169-63v191H0Z" fill="#60a5fa" opacity=".8"/><path d="M0 264c174-34 263-14 413-38s234-46 392-18 244 26 395-12v124H0Z" fill="#3b82f6" opacity=".55"/><path d="M0 292c209-31 353-6 511-18s377-32 689-2v48H0Z" fill="#2563eb" opacity=".3"/></svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

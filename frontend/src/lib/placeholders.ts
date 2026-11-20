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

/** Number of deterministic background tints defined in styles.css. */
const AVATAR_TINT_COUNT = 6

/** A single, uppercase initial for a name — used when there is no avatar image. */
export function avatarInitial(name: string): string {
  return name.trim().charAt(0).toUpperCase() || '?'
}

/**
 * Stable tint for a name: the same name always gets the same colour, and
 * names with different casings/spacing are treated alike.
 */
export function avatarTint(name: string): string {
  const key = name.trim().toLowerCase()
  let hash = 0
  for (let i = 0; i < key.length; i += 1) {
    hash = (hash * 31 + key.charCodeAt(i)) >>> 0
  }
  return `var(--avatar-tint-${(hash % AVATAR_TINT_COUNT) + 1})`
}

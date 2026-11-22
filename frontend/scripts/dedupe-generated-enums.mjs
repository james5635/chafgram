// Codegen hook: @graphql-codegen/typescript and @graphql-codegen/typescript-operations
// both emit the same enum string-union aliases into one file. Keep the first copy.
import { readFileSync, writeFileSync } from 'node:fs'

const file = 'src/generated/graphql.ts'
let source = readFileSync(file, 'utf8')

for (const name of ['PostType', 'ReactionType', 'NotificationType']) {
  const pattern = new RegExp(`export type ${name} =[\\s\\S]*?;\\n`, 'g')
  const matches = [...source.matchAll(pattern)]
  for (const match of matches.slice(1)) {
    source = source.replace(match[0], '')
  }
}

writeFileSync(file, source)

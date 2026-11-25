import type { CodegenConfig } from '@graphql-codegen/cli'

/**
 * GraphQL codegen for the Chafgram frontend.
 *
 * Schema is read directly from the backend's committed .graphqls files, so
 * codegen works without a running server. Documents live in src/graphql/.
 * Output is generated source (git-ignored) — never hand-edit it.
 *
 * Schema types and operation types are generated into two files: the
 * operations plugin imports the schema types (including the enums) from
 * `types.ts` instead of emitting a second copy of them.
 */
const config: CodegenConfig = {
  schema: '../backend/src/main/resources/graphql/*.graphqls',
  documents: 'src/graphql/**/*.graphql',
  generates: {
    'src/generated/types.ts': {
      plugins: ['typescript'],
      config: {
        useTypeImports: true,
        enumsAsTypes: true,
        scalars: { ID: 'string' },
      },
    },
    'src/generated/graphql.ts': {
      plugins: ['typescript-operations', 'typed-document-node'],
      config: {
        useTypeImports: true,
        scalars: { ID: 'string' },
        // Resolved relative to the process cwd (frontend/), not the output file.
        importSchemaTypesFrom: 'src/generated/types',
      },
    },
  },
}

export default config

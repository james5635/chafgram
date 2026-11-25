import { MockedProvider } from '@apollo/client/testing/react'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { MeDocument } from '../generated/graphql'

vi.mock('./ThemeToggle', () => ({ default: () => <button type="button">Theme</button> }))
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    Link: ({ to, children, ...rest }: { to: string; children: ReactNode; [key: string]: unknown }) => (
      <a href={String(to)} {...rest}>{children}</a>
    ),
  }
})

import Header from './Header'

describe('Header', () => {
  it('labels the avatar with the signed-in member, not a hardcoded name', async () => {
    render(
      <MockedProvider
        mocks={[
          {
            request: { query: MeDocument },
            result: {
              data: {
                me: {
                  __typename: 'Member',
                  id: '1',
                  username: 'devadmin',
                  displayName: 'Dev Admin',
                  bio: null,
                  avatarMediaId: null,
                  role: 'ADMIN',
                  status: 'ACTIVE',
                  followerCount: 0,
                  followingCount: 0,
                  isFollowing: false,
                },
              },
            },
          },
        ]}
      >
        <Header />
      </MockedProvider>,
    )

    expect(await screen.findByLabelText('Dev Admin')).toBeInTheDocument()
    expect(screen.getByText('D')).toBeInTheDocument()
    expect(screen.queryByText('R')).not.toBeInTheDocument()
  })
})

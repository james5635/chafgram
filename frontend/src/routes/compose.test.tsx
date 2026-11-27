import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ComponentProps, ReactNode } from 'react'
import { CreatePostDocument, MeDocument } from '../generated/graphql'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/compose' }),
    useParams: () => ({ username: 'rojame' }),
    useNavigate: () => navigate,
    Link: ({
      to,
      params,
      children,
      activeProps: _activeProps,
      ...rest
    }: {
      to: string
      params?: Record<string, string>
      children: ReactNode
      activeProps?: unknown
      [key: string]: unknown
    }) => {
      let href = String(to)
      for (const [key, value] of Object.entries(params ?? {})) {
        href = href.replace(`$${key}`, String(value)).replace(`:${key}`, String(value))
      }
      return <a href={href} {...rest}>{children}</a>
    },
  }
})

import { Compose } from './compose'

const MEMBER = {
  __typename: 'Member' as const,
  id: '1',
  username: 'rojame',
  displayName: 'Rojame',
  bio: 'Software Engineer',
  avatarMediaId: null,
  role: 'MEMBER',
  status: 'ACTIVE',
  followerCount: 128,
  followingCount: 320,
  isFollowing: false,
}

const mocks = [
  { request: { query: MeDocument }, result: { data: { me: MEMBER } } },
  {
    request: { query: CreatePostDocument, variables: { type: 'NOTE', body: 'Fresh post' } },
    result: {
      data: {
        createPost: {
          __typename: 'Post',
          id: 'p2',
          type: 'NOTE',
          body: 'Fresh post',
          createdAt: '2026-11-21T10:10:00Z',
          author: { id: '1', username: 'rojame', displayName: 'Rojame' },
        },
      },
    },
  },
]

type ScreenMocks = ComponentProps<typeof MockedProvider>['mocks']

function renderScreen(ui: ReactNode, screenMocks: ScreenMocks = mocks) {
  return render(
    <MockedProvider mocks={screenMocks} mockLinkDefaultOptions={{ delay: 0 }}>
      {ui}
    </MockedProvider>,
  )
}

beforeEach(() => {
  navigate.mockClear()
})

describe('Compose screen', () => {
  it('renders the editor, chips, and upload area', () => {
    renderScreen(<Compose />)

    expect(screen.getByRole('heading', { name: 'Create Post' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText("What's on your mind?")).toBeInTheDocument()
    for (const chip of ['Photo', 'Video', 'Feeling', 'Poll']) {
      expect(screen.getByText(chip)).toBeInTheDocument()
    }
    expect(screen.getByText('Upload Image')).toBeInTheDocument()
    expect(screen.getByText('or drag and drop')).toBeInTheDocument()
  })

  it('renders the action bar', () => {
    renderScreen(<Compose />)
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Post' })).toBeInTheDocument()
  })

  it('publishes the composed post and returns to the feed', async () => {
    const user = userEvent.setup()
    renderScreen(<Compose />)

    await user.type(screen.getByPlaceholderText("What's on your mind?"), 'Fresh post')
    await user.click(screen.getByRole('button', { name: 'Post' }))

    expect(await screen.findByRole('heading', { name: 'Create Post' })).toBeInTheDocument()
  })

  it('shows Posting… while publishing and ignores a second rapid Post click', async () => {
    const createPostResult = vi.fn(() => ({
      data: {
        createPost: {
          __typename: 'Post' as const,
          id: 'p2',
          type: 'NOTE' as const,
          body: 'Fresh post',
          createdAt: '2026-11-21T10:10:00Z',
          author: { id: '1', username: 'rojame', displayName: 'Rojame' },
        },
      },
    }))
    const slowMocks = [
      mocks[0],
      {
        request: {
          query: CreatePostDocument,
          variables: { type: 'NOTE', body: 'Fresh post', mediaIds: null },
        },
        result: createPostResult,
        delay: 250,
        maxUsageCount: 5,
      },
    ]
    const user = userEvent.setup()
    renderScreen(<Compose />, slowMocks)

    await user.type(screen.getByPlaceholderText("What's on your mind?"), 'Fresh post')
    const postButton = screen.getByRole('button', { name: 'Post' })
    fireEvent.click(postButton)
    fireEvent.click(postButton)

    expect(createPostResult).not.toHaveBeenCalled()
    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('Posting…')
    const region = postButton.closest('[aria-busy="true"]')
    expect(region).not.toBeNull()
    expect(region).toHaveClass('relative')
    expect(region).not.toContainElement(status)
    expect(status.closest('[aria-busy]')).toBeNull()
    expect(status.closest('[inert]')).toBeNull()

    const inertContent = postButton.closest('[inert]')
    expect(inertContent).not.toBeNull()
    expect(inertContent).toContainElement(screen.getByPlaceholderText("What's on your mind?"))
    expect(inertContent).not.toContainElement(status)
    expect(postButton).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Photo' })).toBeDisabled()
    expect(screen.getByRole('button', { name: /Upload Image/ })).toBeDisabled()

    await waitFor(() => expect(createPostResult).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/' }))
    await waitFor(() => expect(screen.getByRole('status')).toBeEmptyDOMElement())
  })
})

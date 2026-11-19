import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import axios from 'axios'
import App from '../App'
import { CorporateAction } from '../types'

vi.mock('axios')

const createDeferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void

  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })

  return { promise, resolve, reject }
}

const action: CorporateAction = {
  id: 1,
  securityId: 'AAPL',
  actionType: 'STOCK_SPLIT',
  ratio: '2:1',
  recordDate: '2026-11-23',
  exDate: '2026-11-24',
  payDate: '2026-11-28',
  status: 'ANALYZED',
  aiAnalysis: 'AI detected a 2:1 split for AAPL.',
  impacts: [
    { id: 10, accountId: 'ACC-001', originalQuantity: 100, newQuantity: 200 },
  ],
}

describe('App (HITL dashboard)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the AI analysis and computed position impacts', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [action] })

    render(<App />)

    expect(await screen.findByText('AAPL - STOCK_SPLIT')).toBeInTheDocument()
    expect(screen.getByText(/AI detected a 2:1 split for AAPL/)).toBeInTheDocument()
    expect(screen.getByText('ACC-001')).toBeInTheDocument()
    expect(screen.getByText('200 Shares')).toBeInTheDocument()
    expect(screen.getByText('DECLARED RATIO: 2:1')).toBeInTheDocument()
    expect(screen.queryByText(/confidence:/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/received 2m ago/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /approve & execute/i })).toBeInTheDocument()
  })

  it('shows an empty dashboard when there are no actions', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [] })

    render(<App />)

    await waitFor(() => expect(screen.getByText('Total Actions')).toBeInTheDocument())
    expect(screen.getByText('0')).toBeInTheDocument()
  })

  it('approves an action through the API when the operator clicks the button', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [action] })
    vi.mocked(axios.post).mockResolvedValue({ data: {} })
    const user = userEvent.setup()

    render(<App />)

    const approveButton = await screen.findByRole('button', { name: /approve & execute/i })
    await user.click(approveButton)

    await waitFor(() =>
      expect(vi.mocked(axios.post)).toHaveBeenCalledWith('/api/ca/1/approve')
    )
  })

  it('disables approval while a request is in flight and prevents duplicate submits', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [action] })

    const approvalRequest = createDeferred<{ data: object }>()
    vi.mocked(axios.post).mockImplementation(() => approvalRequest.promise as never)

    const user = userEvent.setup()
    render(<App />)

    const approveButton = await screen.findByRole('button', { name: /approve & execute/i })
    await user.click(approveButton)

    expect(approveButton).toBeDisabled()
    expect(approveButton).toHaveTextContent(/approving/i)
    expect(vi.mocked(axios.post)).toHaveBeenCalledTimes(1)

    await user.click(approveButton)
    expect(vi.mocked(axios.post)).toHaveBeenCalledTimes(1)

    approvalRequest.resolve({ data: {} })

    await waitFor(() => expect(approveButton).not.toBeDisabled())
    expect(approveButton).toHaveTextContent(/approve & execute/i)
  })

  it('shows error message when the pending-actions request fails', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('network down'))

    render(<App />)

    expect(await screen.findByText(/Failed to fetch corporate actions/)).toBeInTheDocument()
  })

  it('waits for the current poll request to finish before scheduling the next one', async () => {
    vi.useFakeTimers()

    const initialRequest = createDeferred<{ data: CorporateAction[] }>()
    vi.mocked(axios.get).mockImplementation(() => initialRequest.promise as never)

    render(<App />)

    expect(vi.mocked(axios.get)).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(15000)
    expect(vi.mocked(axios.get)).toHaveBeenCalledTimes(1)

    initialRequest.resolve({ data: [action] })
    await Promise.resolve()
    await Promise.resolve()

    await vi.advanceTimersByTimeAsync(4999)
    expect(vi.mocked(axios.get)).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(1)
    expect(vi.mocked(axios.get)).toHaveBeenCalledTimes(2)
  })
})

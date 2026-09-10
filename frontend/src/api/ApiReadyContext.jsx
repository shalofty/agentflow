import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { getHealth } from './client'

const ApiReadyContext = createContext({
  status: 'checking',
  error: null,
  retry: () => {},
})

const MAX_ATTEMPTS = 12
const INITIAL_DELAY_MS = 1500
const MAX_DELAY_MS = 8000
const ATTEMPT_TIMEOUT_MS = 25000

function sleep(ms, signal) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'))
      return
    }
    const timer = setTimeout(resolve, ms)
    signal?.addEventListener(
      'abort',
      () => {
        clearTimeout(timer)
        reject(new DOMException('Aborted', 'AbortError'))
      },
      { once: true },
    )
  })
}

export function ApiReadyProvider({ children }) {
  const [status, setStatus] = useState('checking')
  const [error, setError] = useState(null)
  const [generation, setGeneration] = useState(0)
  const abortRef = useRef(null)

  const retry = useCallback(() => {
    setGeneration((value) => value + 1)
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    abortRef.current = controller
    let cancelled = false

    async function poll() {
      setStatus('checking')
      setError(null)
      let delay = INITIAL_DELAY_MS

      for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
        if (cancelled) {
          return
        }
        if (attempt > 1) {
          setStatus('waking')
        }
        const attemptController = new AbortController()
        const onAbort = () => attemptController.abort()
        controller.signal.addEventListener('abort', onAbort)
        const timeout = setTimeout(() => attemptController.abort(), ATTEMPT_TIMEOUT_MS)
        try {
          const health = await getHealth({ signal: attemptController.signal })
          if (cancelled) {
            return
          }
          if (health?.status === 'UP') {
            setStatus('ready')
            setError(null)
            return
          }
          throw new Error('API health returned an unexpected payload')
        } catch (err) {
          if (cancelled || controller.signal.aborted) {
            return
          }
          if (attempt === MAX_ATTEMPTS) {
            setStatus('failed')
            setError(
              err.name === 'AbortError'
                ? 'Timed out waiting for the API. The free host may still be waking up.'
                : err.message || 'Unable to reach the API.',
            )
            return
          }
        } finally {
          clearTimeout(timeout)
          controller.signal.removeEventListener('abort', onAbort)
        }

        try {
          await sleep(delay, controller.signal)
        } catch {
          return
        }
        delay = Math.min(delay * 1.4, MAX_DELAY_MS)
      }
    }

    poll()

    return () => {
      cancelled = true
      controller.abort()
    }
  }, [generation])

  return (
    <ApiReadyContext.Provider value={{ status, error, retry }}>
      {children}
    </ApiReadyContext.Provider>
  )
}

export function useApiReady() {
  return useContext(ApiReadyContext)
}

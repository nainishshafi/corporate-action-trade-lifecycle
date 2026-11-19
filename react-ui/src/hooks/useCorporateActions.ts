import { useState, useEffect, useCallback, useRef } from 'react';
import { CorporateAction } from '../types';
import { caService } from '../api/caService';

export type DecisionType = 'approve' | 'reject';

const isAbortError = (error: unknown): boolean => {
  if (!(error instanceof Error)) {
    return false;
  }

  return error.name === 'CanceledError' || error.name === 'AbortError';
};

export const useCorporateActions = (pollingInterval = 5000) => {
  const [actions, setActions] = useState<CorporateAction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingDecisionById, setPendingDecisionById] = useState<
    Record<number, DecisionType>
  >({});

  const latestRequestIdRef = useRef(0);
  const inFlightDecisionsRef = useRef(new Set<number>());
  const isMountedRef = useRef(true);
  const pollTimeoutRef = useRef<number | undefined>(undefined);
  const activeControllerRef = useRef<AbortController | null>(null);

  const clearScheduledPoll = useCallback(() => {
    if (pollTimeoutRef.current !== undefined) {
      window.clearTimeout(pollTimeoutRef.current);
      pollTimeoutRef.current = undefined;
    }
  }, []);

  const runFetch = useCallback(async () => {
    clearScheduledPoll();
    activeControllerRef.current?.abort();

    const controller = new AbortController();
    activeControllerRef.current = controller;
    const requestId = ++latestRequestIdRef.current;

    try {
      const data = await caService.getPendingActions(controller.signal);

      if (!isMountedRef.current || requestId !== latestRequestIdRef.current) {
        return;
      }

      setActions(data);
      setError(null);
    } catch (err: unknown) {
      if (
        !isMountedRef.current ||
        requestId !== latestRequestIdRef.current ||
        isAbortError(err)
      ) {
        return;
      }

      console.error('Error fetching actions:', err);
      setError('Failed to fetch corporate actions. Retrying...');
    } finally {
      if (activeControllerRef.current === controller) {
        activeControllerRef.current = null;
      }

      if (isMountedRef.current && requestId === latestRequestIdRef.current) {
        if (!controller.signal.aborted) {
          setLoading(false);
        }

        pollTimeoutRef.current = window.setTimeout(() => {
          void runFetch();
        }, pollingInterval);
      }
    }
  }, [clearScheduledPoll, pollingInterval]);

  useEffect(() => {
    isMountedRef.current = true;
    void runFetch();

    return () => {
      isMountedRef.current = false;
      clearScheduledPoll();
      activeControllerRef.current?.abort();
      activeControllerRef.current = null;
    };
  }, [clearScheduledPoll, runFetch]);

  const submitDecision = useCallback(
    async (
      id: number,
      decision: DecisionType,
      actionFn: (actionId: number) => Promise<void>,
    ) => {
      if (inFlightDecisionsRef.current.has(id)) {
        return;
      }

      inFlightDecisionsRef.current.add(id);

      if (isMountedRef.current) {
        setPendingDecisionById((current) => ({ ...current, [id]: decision }));
      }

      try {
        await actionFn(id);
        await runFetch();
      } catch (err: unknown) {
        console.error(`Error during ${decision} action:`, err);
        throw err;
      } finally {
        inFlightDecisionsRef.current.delete(id);

        if (isMountedRef.current) {
          setPendingDecisionById((current) => {
            const next = { ...current };
            delete next[id];
            return next;
          });
        }
      }
    },
    [runFetch],
  );

  const approveAction = useCallback(
    (id: number) => submitDecision(id, 'approve', caService.approveAction),
    [submitDecision],
  );

  const rejectAction = useCallback(
    (id: number) => submitDecision(id, 'reject', caService.rejectAction),
    [submitDecision],
  );

  return {
    actions,
    loading,
    error,
    pendingDecisionById,
    approveAction,
    rejectAction,
    refresh: runFetch,
  };
};

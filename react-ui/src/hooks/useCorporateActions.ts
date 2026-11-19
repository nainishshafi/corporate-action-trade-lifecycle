import { useState, useEffect, useCallback, useRef } from 'react';
import { CorporateAction } from '../types';
import { caService } from '../api/caService';

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
  const [approvingActionIds, setApprovingActionIds] = useState<number[]>([]);
  const latestRequestIdRef = useRef(0);
  const inFlightApprovalsRef = useRef(new Set<number>());
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const fetchActions = useCallback(async (signal?: AbortSignal) => {
    const requestId = ++latestRequestIdRef.current;

    try {
      const data = await caService.getPendingActions(signal);

      if (!isMountedRef.current || requestId !== latestRequestIdRef.current) {
        return;
      }

      setActions(data);
      setError(null);
    } catch (error) {
      if (
        !isMountedRef.current ||
        requestId !== latestRequestIdRef.current ||
        isAbortError(error)
      ) {
        return;
      }

      console.error('Error fetching actions:', error);
      setError('Failed to fetch corporate actions. Retrying...');
    } finally {
      if (
        isMountedRef.current &&
        requestId === latestRequestIdRef.current &&
        !signal?.aborted
      ) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    let isDisposed = false;
    let timeoutId: number | undefined;
    let controller: AbortController | null = null;

    const poll = async () => {
      controller = new AbortController();
      await fetchActions(controller.signal);

      if (!isDisposed) {
        timeoutId = window.setTimeout(() => {
          void poll();
        }, pollingInterval);
      }
    };

    void poll();

    return () => {
      isDisposed = true;
      controller?.abort();

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [fetchActions, pollingInterval]);

  const approveAction = async (id: number) => {
    if (inFlightApprovalsRef.current.has(id)) {
      return;
    }

    inFlightApprovalsRef.current.add(id);

    if (isMountedRef.current) {
      setApprovingActionIds((currentIds) => [...currentIds, id]);
    }

    try {
      await caService.approveAction(id);
      await fetchActions();
    } catch (error) {
      console.error('Error approving action:', error);
      throw error;
    } finally {
      inFlightApprovalsRef.current.delete(id);

      if (isMountedRef.current) {
        setApprovingActionIds((currentIds) =>
          currentIds.filter((currentId) => currentId !== id),
        );
      }
    }
  };

  return {
    actions,
    loading,
    error,
    approvingActionIds,
    approveAction,
    refresh: fetchActions,
  };
};

import { useEffect, useRef, useState, useCallback } from 'react';
import api from '../api/client';
import { apiError } from './format';

/** Current time, refreshed every `ms` milliseconds (drives the live SLA clocks). */
export function useNow(ms = 1000) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), ms);
    return () => clearInterval(id);
  }, [ms]);
  return now;
}

/** GET a URL and keep { data, error, loading, reload }. Pass null to skip. */
export function useApi(url, { initial = null, poll = 0 } = {}) {
  const [data, setData] = useState(initial);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(Boolean(url));
  const mounted = useRef(true);

  const load = useCallback(
    async (quiet = false) => {
      if (!url) return;
      if (!quiet) setLoading(true);
      try {
        const res = await api.get(url);
        if (mounted.current) {
          setData(res.data);
          setError(null);
        }
      } catch (e) {
        if (mounted.current) setError(apiError(e));
      } finally {
        if (mounted.current) setLoading(false);
      }
    },
    [url]
  );

  useEffect(() => {
    mounted.current = true;
    load();
    let id;
    if (poll) id = setInterval(() => load(true), poll);
    return () => {
      mounted.current = false;
      if (id) clearInterval(id);
    };
  }, [load, poll]);

  return { data, error, loading, reload: load, setData };
}

/** Debounced value, for live classification / duplicate checks while typing. */
export function useDebounced(value, ms = 450) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setV(value), ms);
    return () => clearTimeout(id);
  }, [value, ms]);
  return v;
}

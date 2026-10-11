import { useCallback, useEffect, useState } from 'react';
import { errorText } from './api';

export function useResource<T>(loader: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const reload = useCallback(async () => { setLoading(true); try { setData(await loader()); setError(''); } catch (e) { setError(errorText(e)); } finally { setLoading(false); } }, deps);
  useEffect(() => { void reload(); }, [reload]);
  return { data, loading, error, reload, setData };
}
export function useMutation() {
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [success, setSuccess] = useState('');
  async function run<T>(action: () => Promise<T>, message = 'Сохранено'): Promise<T | null> {
    setBusy(true); setError(''); setSuccess('');
    try { const result = await action(); setSuccess(message); return result; }
    catch (e) { setError(errorText(e)); return null; }
    finally { setBusy(false); }
  }
  return { busy, error, success, run, clear: () => { setError(''); setSuccess(''); } };
}

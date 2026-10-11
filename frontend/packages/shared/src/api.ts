import type { Role } from './types';

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message); this.name = 'ApiError'; }
}

type AuthState = { mode: 'local'; email: string; role: Role } | { mode: 'keycloak'; token: () => Promise<string> };
let auth: AuthState = { mode: 'local', email: 'candidate1@example.com', role: 'CANDIDATE' };
export function setApiAuth(next: AuthState) { auth = next; }

const baseUrl = import.meta.env.VITE_API_URL || '/api/v1';
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!(init.body instanceof FormData) && init.body !== undefined) headers.set('Content-Type', 'application/json');
  if (auth.mode === 'local') { headers.set('X-User-Email', auth.email); headers.set('X-User-Role', auth.role); }
  else headers.set('Authorization', `Bearer ${await auth.token()}`);
  let response: Response;
  try { response = await fetch(`${baseUrl}${path}`, { ...init, headers }); }
  catch { throw new ApiError(0, 'NETWORK', 'Не удалось связаться с сервером. Проверьте, что backend запущен.'); }
  if (!response.ok) {
    const data = await response.json().catch(() => ({})) as { code?: string; message?: string };
    throw new ApiError(response.status, data.code || `HTTP_${response.status}`, data.message || 'Не удалось выполнить запрос');
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
export const get = <T>(path: string) => api<T>(path);
export const post = <T>(path: string, data?: unknown) => api<T>(path, { method: 'POST', body: data === undefined ? undefined : JSON.stringify(data) });
export const put = <T>(path: string, data: unknown) => api<T>(path, { method: 'PUT', body: JSON.stringify(data) });
export function query(params: Record<string, string | number | boolean | null | undefined>) {
  const p = new URLSearchParams(); Object.entries(params).forEach(([k, v]) => { if (v !== null && v !== undefined && v !== '') p.set(k, String(v)); });
  return p.toString();
}
export function errorText(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'JS_007') return 'Повторный тест станет доступен через 30 дней после предыдущей попытки.';
    if (error.code === 'JS_006') return 'Для этого действия требуются согласия на обработку и публикацию профиля.';
    return error.message;
  }
  return error instanceof Error ? error.message : 'Что-то пошло не так';
}

import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import Keycloak from 'keycloak-js';
import { get, setApiAuth, errorText } from './api';
import type { Me, Role } from './types';

interface AuthContextValue { me: Me | null; loading: boolean; error: string; mode: 'local' | 'keycloak'; login: () => void; logout: () => void; switchDemo: (email: string) => void; refresh: () => Promise<void> }
const AuthContext = createContext<AuthContextValue | null>(null);
const mode = import.meta.env.VITE_AUTH_MODE === 'keycloak' ? 'keycloak' : 'local';
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081';
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM || 'jobsearcher';
const keycloak = mode === 'keycloak' ? new Keycloak({ url: keycloakUrl, realm: keycloakRealm, clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'jobsearcher-frontend' }) : null;
const localUser = (email: string, role: Role): Me => ({ userId: `dev-${email}`, email, role, emailVerified: true, consents: [] });
const localApiError = 'Данные backend недоступны. Запустите из корня проекта: docker compose up -d --build.';

export function AuthProvider({ role, children }: { role: Role; children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [demoEmail, setDemoEmail] = useState(() => sessionStorage.getItem(`demo-${role}`) || (role === 'CANDIDATE' ? 'candidate1@example.com' : 'employer1@example.com'));
  async function refresh() { try { setError(''); setMe(await get<Me>('/me')); } catch (e) { setMe(mode === 'local' ? localUser(demoEmail, role) : null); setError(mode === 'local' ? localApiError : errorText(e)); } }
  useEffect(() => {
    let active = true;
    async function init() {
      try {
        if (keycloak) {
          try {
            await fetch(`${keycloakUrl}/realms/${keycloakRealm}/.well-known/openid-configuration`, { mode: 'no-cors', signal: AbortSignal.timeout(3500) });
          } catch {
            throw new Error('Keycloak недоступен на ' + keycloakUrl + '. Запустите docker compose или переключите VITE_AUTH_MODE=local для локального backend.');
          }
          await keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false });
          if (keycloak.authenticated) setApiAuth({ mode: 'keycloak', token: async () => { await keycloak.updateToken(30); if (!keycloak.token) throw new Error('Сессия истекла'); return keycloak.token; } });
          else { if (active) { setMe(null); setLoading(false); } return; }
        } else setApiAuth({ mode: 'local', email: demoEmail, role });
        const user = await get<Me>('/me');
        if (active) { setMe(user); setError(''); }
      } catch (e) { if (active) { setMe(mode === 'local' ? localUser(demoEmail, role) : null); setError(mode === 'local' ? localApiError : errorText(e)); } }
      finally { if (active) setLoading(false); }
    }
    void init(); return () => { active = false; };
  }, [demoEmail, role]);
  const value: AuthContextValue = { me, loading, error, mode,
    login: () => { void keycloak?.login({ redirectUri: window.location.href }); },
    logout: () => { if (keycloak) void keycloak.logout({ redirectUri: window.location.origin }); },
    switchDemo: email => { sessionStorage.setItem(`demo-${role}`, email); setLoading(true); setDemoEmail(email); },
    refresh };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() { const ctx = useContext(AuthContext); if (!ctx) throw new Error('AuthProvider missing'); return ctx; }

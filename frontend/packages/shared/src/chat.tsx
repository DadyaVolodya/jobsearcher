import { useEffect, useState, type FormEvent } from 'react';
import { ArrowRight, MessageCircle, Send } from 'lucide-react';
import { get, post } from './api';
import { useAuth } from './auth';
import { useMutation, useResource } from './hooks';
import type { ChatMessage, Thread } from './types';
import { Badge, Empty, Feedback, Loading, formatDate, salaryRange, statusName } from './ui';

export function ChatPanel() {
  const { me } = useAuth();
  const threads = useResource(() => get<Thread[]>('/chats'));
  const [selected, setSelected] = useState<string | null>(null);
  const [body, setBody] = useState('');
  const messages = useResource(() => selected ? get<ChatMessage[]>(`/chats/${selected}/messages`) : Promise.resolve([]), [selected]);
  const send = useMutation();
  useEffect(() => { if (!selected && threads.data?.length) setSelected(threads.data[0].id); }, [threads.data, selected]);
  useEffect(() => {
    if (!selected || messages.loading || messages.error) return;
    void threads.reload();
  }, [selected, messages.loading, messages.data?.length]);
  const thread = threads.data?.find(t => t.id === selected);
  async function submit(e: FormEvent) {
    e.preventDefault(); if (!body.trim() || !selected) return;
    const result = await send.run(() => post<ChatMessage>(`/chats/${selected}/messages`, { body: body.trim() }), 'Сообщение отправлено');
    if (result) { setBody(''); void messages.reload(); void threads.reload(); }
  }
  if (threads.loading) return <Loading/>;
  if (threads.error) return <Feedback error={threads.error}/>;
  if (!threads.data?.length) return <div className="chat-empty"><div className="chat-empty-kicker">ФСП / ДИАЛОГИ</div><div className="chat-empty-mark"><MessageCircle size={42} strokeWidth={1.5}/></div><h2>Здесь начнётся разговор</h2><p>Переписка появится после приглашения. Все предложения и ответы будут собраны в одном месте.</p><a className="button primary" href="#/invitations">Открыть приглашения <ArrowRight size={17}/></a><div className="chat-empty-line" aria-hidden="true"/></div>;
  return <div className="chat-layout"><div className="chat-list"><div className="chat-list-head">Диалоги <Badge tone="purple">{threads.data.length}</Badge></div>{threads.data.map(t => <button className={`chat-thread ${selected === t.id ? 'active' : ''}`} key={t.id} onClick={() => setSelected(t.id)}><strong>{me?.role === 'CANDIDATE' ? t.companyName : t.candidateName}{!!t.unreadCount && <Badge tone="purple">{t.unreadCount}</Badge>}</strong><small>{salaryRange(t.salaryFrom, t.salaryTo)} · {statusName(t.invitationStatus)}</small></button>)}</div><div className="chat-main">{thread && <><div className="chat-head"><strong>{me?.role === 'CANDIDATE' ? thread.companyName : thread.candidateName}</strong><small>{salaryRange(thread.salaryFrom, thread.salaryTo)} · {statusName(thread.invitationStatus)}</small>{thread.contactsRevealed && <div className="muted-line">Контакты: {me?.role === 'CANDIDATE' ? [thread.employerEmail, thread.employerPhone].filter(Boolean).join(' · ') : [thread.candidateEmail, thread.candidatePhone].filter(Boolean).join(' · ')}</div>}{thread.declineReason && <div className="muted-line">Причина отказа: {thread.declineReason}</div>}</div><div className="chat-messages">{messages.loading ? <Loading/> : messages.error ? <Feedback error={messages.error}/> : messages.data?.length ? messages.data.map(m => <div key={m.id} className={`chat-bubble ${m.senderUserId === me?.userId ? 'mine' : ''}`}>{m.body}<small>{formatDate(m.createdAt)}</small></div>) : <Empty title="Начните разговор" description="Напишите первое сообщение по приглашению."/>}</div><form className="chat-compose" onSubmit={submit}><input value={body} onChange={e => setBody(e.target.value)} placeholder="Ваше сообщение…" aria-label="Сообщение"/><button className="button primary" disabled={send.busy || !body.trim()} aria-label="Отправить"><Send size={17}/></button></form>{send.error && <div style={{padding:'0 14px'}}><Feedback error={send.error}/></div>}</>}</div></div>;
}

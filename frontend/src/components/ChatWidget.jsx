import { useEffect, useRef, useState } from 'react';
import { Bot, MessageCircle, Mic, MicOff, Send, X, RotateCcw, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { apiError } from '../lib/format';

const initialSuggestions = (role) => [
  'Report an incident', 'My open incidents',
  ...(role === 'TECHNICIAN' ? ['My queue'] : []),
  ...(role === 'ADMIN' ? ['System summary'] : []),
];

/** Accessible, responsive request/response chat; no additional frontend dependencies. */
export default function ChatWidget() {
  const { role } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [failed, setFailed] = useState(null);
  const [listening, setListening] = useState(false);
  const button = useRef(null);
  const editor = useRef(null);
  const scroll = useRef(null);
  const recognition = useRef(null);
  const speechSupported = typeof window !== 'undefined' && Boolean(window.SpeechRecognition || window.webkitSpeechRecognition);

  useEffect(() => {
    if (!open) return;
    let active = true;
    api.get('/chat/history').then(({ data }) => {
      if (active) setItems(data.map((m) => ({ fromUser: m.fromUser, text: m.text, cards: [], actions: [], quickReplies: [] })));
    }).catch(() => { if (active) setError('Could not load chat history. You can still send a message.'); });
    editor.current?.focus();
    const escape = (e) => { if (e.key === 'Escape') setOpen(false); };
    document.addEventListener('keydown', escape);
    return () => { active = false; document.removeEventListener('keydown', escape); recognition.current?.stop?.(); };
  }, [open]);

  useEffect(() => { scroll.current?.scrollTo({ top: scroll.current.scrollHeight, behavior: 'smooth' }); }, [items, busy]);
  useEffect(() => { if (!open) button.current?.focus(); }, [open]);

  const send = async (value = draft) => {
    const text = value.trim();
    if (!text || busy || text.length > 1000) return;
    setDraft(''); setError(''); setFailed(null);
    setItems((old) => [...old, { fromUser: true, text }]);
    setBusy(true);
    try {
      const { data } = await api.post('/chat/message', { message: text });
      setItems((old) => [...old, { fromUser: false, ...data }]);
    } catch (e) {
      setFailed(text);
      setError(e.response?.status === 429 ? 'Too many messages. Wait a minute and retry.' : apiError(e, 'Message failed. Try again.'));
    } finally { setBusy(false); }
  };

  const reset = async () => {
    if (busy) return;
    try {
      const { data } = await api.post('/chat/reset');
      setItems((old) => [...old, { fromUser: false, text: data.text }]);
    } catch (e) { setError(apiError(e)); }
  };

  const voice = () => {
    if (!speechSupported) return;
    if (listening) { recognition.current?.stop(); return; }
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const r = new SpeechRecognition();
    recognition.current = r;
    r.lang = 'en-IN'; r.interimResults = false; r.maxAlternatives = 1;
    r.onresult = (e) => { setDraft((previous) => `${previous} ${e.results[0][0].transcript}`.trim().slice(0, 1000)); editor.current?.focus(); };
    r.onerror = (e) => setError(e.error === 'not-allowed' ? 'Microphone permission denied. You can type instead.' : 'Voice input is unavailable. Please type.');
    r.onend = () => setListening(false);
    try { r.start(); setListening(true); } catch { setError('Could not start voice input.'); }
  };

  return (
    <>
      <button ref={button} onClick={() => setOpen(true)} aria-label="Open IT support chatbot" aria-expanded={open}
        className="fixed bottom-5 right-5 z-40 flex items-center gap-2 rounded-full bg-ink px-4 py-3 font-semibold text-white shadow-lg hover:opacity-90">
        <MessageCircle className="h-5 w-5" /> <span className="hidden sm:inline">IT assistant</span>
      </button>
      {open && (
        <section role="dialog" aria-modal="true" aria-label="IT support assistant"
          className="fixed inset-0 z-50 flex flex-col overflow-hidden bg-surface shadow-2xl sm:inset-auto sm:bottom-20 sm:right-5 sm:h-[min(640px,calc(100vh-110px))] sm:w-[410px] sm:rounded-2xl sm:border sm:border-line">
          <header className="flex items-center gap-3 border-b border-line p-4">
            <span className="rounded-full bg-sunken p-2"><Bot className="h-5 w-5" /></span>
            <div className="flex-1"><h2 className="font-semibold">I-ITSM assistant</h2><p className="text-xs text-muted">Rule-based support · human handoff on request</p></div>
            <button onClick={reset} title="Cancel current draft" aria-label="Reset chat" className="rounded-md p-2 hover:bg-sunken"><RotateCcw className="h-4 w-4" /></button>
            <button onClick={() => setOpen(false)} aria-label="Close chat" className="rounded-md p-2 hover:bg-sunken"><X className="h-5 w-5" /></button>
          </header>
          <div ref={scroll} role="log" aria-live="polite" aria-relevant="additions" className="flex-1 space-y-3 overflow-y-auto bg-paper p-4">
            {items.length === 0 && (
              <div className="space-y-3 rounded-xl bg-surface p-4 text-sm text-steel">
                <p>Hi! I can help {role === 'EMPLOYEE' ? 'report and track your incidents' : 'with tickets and your role-specific tasks'}. What would you like to do?</p>
                <div className="flex flex-wrap gap-2">{initialSuggestions(role).map((s) => <Suggestion key={s} onClick={() => send(s)}>{s}</Suggestion>)}</div>
              </div>
            )}
            {items.map((m, index) => (
              <div key={index} className={`flex ${m.fromUser ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[90%] space-y-2 rounded-xl px-3 py-2.5 text-sm ${m.fromUser ? 'bg-ink text-white' : 'border border-line bg-surface text-ink'}`}>
                  <p className="whitespace-pre-wrap break-words">{m.text}</p>
                  {m.cards?.map((c, n) => <div key={n} className="rounded-lg border border-line bg-paper p-2 text-ink">
                    <p className="font-semibold">{c.title}</p><p className="text-xs text-steel">{c.subtitle}</p><p className="mt-1 whitespace-pre-wrap text-xs">{c.detail}</p>
                  </div>)}
                  {m.actions?.map((a, n) => <button key={n} className="mr-2 inline-flex items-center gap-1 rounded-lg bg-p3-soft px-2 py-1 text-p3"
                    onClick={() => a.type === 'OPEN_INCIDENT' && Number.isInteger(a.incidentId)
                      ? (setOpen(false), navigate(`/incidents/${a.incidentId}`))
                      : send(a.type === 'CONFIRM_CREATE' ? 'Confirm' : 'Cancel')}>
                    {a.label} {a.type === 'OPEN_INCIDENT' && <ExternalLink className="h-3 w-3" />}
                  </button>)}
                  {m.quickReplies?.length > 0 && <div className="flex flex-wrap gap-1.5 pt-1">{m.quickReplies.map((q) =>
                    <Suggestion key={q} onClick={() => send(q)}>{q}</Suggestion>)}</div>}
                </div>
              </div>
            ))}
            {busy && <p className="text-sm text-muted" role="status">Assistant is typing…</p>}
          </div>
          {error && <div className="border-t border-line bg-bad/10 px-4 py-2 text-sm text-bad" role="alert">
            {error} {failed && <button onClick={() => send(failed)} className="font-bold underline">Retry</button>}
          </div>}
          <form onSubmit={(e) => { e.preventDefault(); send(); }} className="flex items-end gap-2 border-t border-line p-3">
            <textarea ref={editor} rows={2} maxLength={1000} value={draft} onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
              aria-label="Message the IT assistant" placeholder="Describe your issue…"
              className="max-h-28 min-h-12 flex-1 resize-none rounded-lg border border-line bg-paper p-2.5 text-sm outline-offset-2" />
            {speechSupported && <button type="button" onClick={voice} aria-label={listening ? 'Stop voice input' : 'Start voice input'}
              className="rounded-lg p-2 hover:bg-sunken">{listening ? <MicOff className="h-5 w-5" /> : <Mic className="h-5 w-5" />}</button>}
            <button disabled={busy || !draft.trim()} aria-label="Send message" type="submit" className="rounded-lg bg-ink p-2.5 text-white disabled:opacity-40"><Send className="h-5 w-5" /></button>
          </form>
        </section>
      )}
    </>
  );
}

function Suggestion({ children, onClick }) {
  return <button type="button" onClick={onClick} className="rounded-full border border-line bg-sunken px-2.5 py-1 text-xs font-medium text-ink hover:bg-p3-soft">{children}</button>;
}

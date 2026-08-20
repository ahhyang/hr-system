import { FormEvent, useRef, useState } from 'react';
import { ErrorAlert, PageHeader } from '../../components/ui';
import { aiApi } from '../../lib/api';
import { ApiException } from '../../lib/api';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  sources?: string[];
}

export default function AiAssistantPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: "Hi — I'm PulseHR. Ask about your leave, attendance, payslip, claims, or company policy. I only answer from your company's live data.",
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  const send = async (e: FormEvent) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || loading) return;

    setInput('');
    setError('');
    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setLoading(true);

    try {
      const res = await aiApi.chat(text);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: res.reply, sources: res.sources },
      ]);
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to get response');
    } finally {
      setLoading(false);
      setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 100);
    }
  };

  return (
    <div className="flex h-[calc(100vh-8rem)] flex-col">
      <PageHeader
        title="Ask PulseHR"
        description="Plain-language answers from your leave, attendance, payroll, and policy records"
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      <div className="mb-4 flex flex-wrap gap-2">
        {[
          'How many annual leave days do I have left?',
          'Was I late this month?',
          'What is my latest net salary?',
          'How is Saturday OT calculated?',
        ].map((q) => (
          <button
            key={q}
            type="button"
            className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-600 hover:border-brand-300 hover:text-brand-700"
            onClick={() => setInput(q)}
          >
            {q}
          </button>
        ))}
      </div>

      <div className="card flex flex-1 flex-col overflow-hidden">
        <div className="flex-1 space-y-4 overflow-y-auto p-4">
          {messages.map((msg, i) => (
            <div
              key={i}
              className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
                  msg.role === 'user'
                    ? 'bg-brand-600 text-white'
                    : 'bg-slate-100 text-slate-800'
                }`}
              >
                <p className="whitespace-pre-wrap">{msg.content}</p>
                {msg.sources && msg.sources.length > 0 && (
                  <div className="mt-2 border-t border-slate-200 pt-2">
                    <p className="text-xs font-medium text-slate-500">Sources:</p>
                    <ul className="mt-1 space-y-0.5">
                      {msg.sources.map((s, j) => (
                        <li key={j} className="text-xs text-slate-500">• {s}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          ))}
          {loading && (
            <div className="flex justify-start">
              <div className="rounded-2xl bg-slate-100 px-4 py-3 text-sm text-slate-500">
                Thinking…
              </div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        <form onSubmit={send} className="border-t border-slate-200 p-4">
          <div className="flex gap-2">
            <input
              className="input flex-1"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask about leave, payslips, policies…"
              disabled={loading}
            />
            <button type="submit" className="btn-primary shrink-0" disabled={loading || !input.trim()}>
              Send
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

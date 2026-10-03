// Srutam app screens rebuilt from the Compose source (light theme). 1dp == 1px inside the phone.
import React from 'react';

const F = {play: "'Playfair Display', serif", body: "'Roboto', system-ui, sans-serif", mono: "'JetBrains Mono', monospace"};
export const K = {
	bg: '#F4F5F8', card: '#FFFFFF', border: '#E2E8F0', chip: '#F2F2F7', soft: '#F8FAFC', ink: '#0F172A', sec: '#64748B', muted: '#8E8E93',
	tmuted: '#94A3B8', cobalt: '#2563EB', cobaltC: '#EFF6FF', cobaltB: '#BFDBFE', onCobalt: '#1E40AF', em: '#10B981', emC: '#ECFDF5', crimson: '#EF4444', title: '#1E2229',
};

// ---------- Material icons (filled, 24x24) ----------
const P: Record<string, string> = {
	search: 'M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
	settings: 'M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z',
	cloudDone: 'M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM10 17l-3.5-3.5 1.41-1.41L10 14.17 15.18 9l1.41 1.41L10 17z',
	sync: 'M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0 0 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 0 0 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z',
	archive: 'M20.54 5.23l-1.39-1.68C18.88 3.21 18.47 3 18 3H6c-.47 0-.88.21-1.16.55L3.46 5.23C3.17 5.57 3 6.02 3 6.5V19c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6.5c0-.48-.17-.93-.46-1.27zM12 17.5L6.5 12H10v-2h4v2h3.5L12 17.5zM5.12 5l.81-1h12l.94 1H5.12z',
	history: 'M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z',
	menu: 'M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z',
	checklist: 'M22 7h-9v2h9V7zm0 8h-9v2h9v-2zM5.54 11L2 7.46l1.41-1.41 2.12 2.12 4.24-4.24 1.41 1.41L5.54 11zm0 8L2 15.46l1.41-1.41 2.12 2.12 4.24-4.24 1.41 1.41L5.54 19z',
	play: 'M8 5v14l11-7z',
	more: 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',
	back: 'M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z',
	lockOpen: 'M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10z',
	delete: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
	deleteOutline: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5l-1-1h-5l-1 1H5v2h14V4z',
	edit: 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM5.92 19H5v-.92l9.06-9.06.92.92L5.92 19zM20.71 5.63l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83a.996.996 0 0 0 0-1.41z',
	schedule: 'M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z',
	graphicEq: 'M7 18h2V6H7v12zm4 4h2V2h-2v20zm-8-8h2v-4H3v4zm12 4h2V6h-2v12zm4-8v4h2v-4h-2z',
	qr: 'M9.5 6.5v3h-3v-3h3M11 5H5v6h6V5zm-1.5 9.5v3h-3v-3h3M11 13H5v6h6v-6zm6.5-6.5v3h-3v-3h3M19 5h-6v6h6V5zm-6 8h1.5v1.5H13V13zm1.5 1.5H16V16h-1.5v-1.5zM16 13h1.5v1.5H16V13zm-3 3h1.5v1.5H13V16zm1.5 1.5H16V19h-1.5v-1.5zM16 16h1.5v1.5H16V16zm1.5-1.5H19V16h-1.5v-1.5zm0 3H19V19h-1.5v-1.5zM22 7h-2V4h-3V2h5v5zm0 15v-5h-2v3h-3v2h5zM2 22h5v-2H4v-3H2v5zM2 2v5h2V4h3V2H2z',
	cloud: 'M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z',
	account: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm0 14c-2.03 0-4.43-.82-6.14-2.88a9.947 9.947 0 0 1 12.28 0C16.43 19.18 14.03 20 12 20z',
	exit: 'M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5a2 2 0 0 0-2 2v4h2V5h14v14H5v-4H3v4a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z',
	computer: 'M20 18c1.1 0 1.99-.9 1.99-2L22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2H0v2h24v-2h-4zM4 6h16v10H4V6z',
	check: 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z',
	copy: 'M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z',
	terminal: 'M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16c1.1 0 2-.9 2-2V6a2 2 0 0 0-2-2zm0 14H4V8h16v10zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.59-2.59L7.5 9l4 4-4 4z',
	bell: 'M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z',
	chev: 'M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z',
	add: 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
	mic: 'M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z',
	pause: 'M6 19h4V5H6v14zm8-14v14h4V5h-4z',
	close: 'M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
	stop: 'M6 6h12v12H6z',
	apps: 'M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z',
};
export type IconName = keyof typeof P;
export const MI: React.FC<{n: IconName; s?: number; c?: string; style?: React.CSSProperties}> = ({n, s = 20, c = K.title, style}) => (
	<svg width={s} height={s} viewBox="0 0 24 24" style={{display: 'block', flex: 'none', ...style}}><path d={P[n]} fill={c} /></svg>
);

// ---------- frame ----------
export const Phone: React.FC<{children: React.ReactNode; dark?: boolean; style?: React.CSSProperties}> = ({children, dark, style}) => (
	<div className="phone" style={style}>
		<div className="vol" /><div className="pwr" />
		<div className="scr" style={{background: dark ? '#000' : K.bg, fontFamily: F.body}}>
			<div style={{position: 'absolute', top: 0, left: 0, right: 0, height: 40, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 30px 0', fontSize: 14, fontWeight: 500, color: dark ? '#fff' : K.ink, zIndex: 20}}>
				<span>9:41</span>
				<span style={{display: 'flex', gap: 5, alignItems: 'center'}}>
					<svg width="15" height="11" viewBox="0 0 15 11"><path d="M7.5 11L0 3.5a10.6 10.6 0 0 1 15 0z" fill={dark ? '#fff' : K.ink} /></svg>
					<svg width="13" height="12" viewBox="0 0 13 12"><path d="M13 0v12H0z" fill={dark ? '#fff' : K.ink} /></svg>
					<svg width="22" height="11" viewBox="0 0 22 11"><rect x="0.5" y="0.5" width="19" height="10" rx="2.5" fill="none" stroke={dark ? '#fff' : K.ink} /><rect x="2" y="2" width="14" height="7" rx="1.2" fill={dark ? '#fff' : K.ink} /><rect x="20.3" y="3.5" width="1.5" height="4" rx=".7" fill={dark ? '#fff' : K.ink} /></svg>
				</span>
			</div>
			{children}
		</div>
	</div>
);
// screen container (below status bar)
export const Screen: React.FC<{children: React.ReactNode; bg?: string; style?: React.CSSProperties}> = ({children, bg = K.bg, style}) => (
	<div style={{position: 'absolute', inset: 0, paddingTop: 40, background: bg, overflow: 'hidden', ...style}}>{children}</div>
);

// ---------- top bar ----------
export const Squircle: React.FC<{children: React.ReactNode}> = ({children}) => (
	<div style={{width: 40, height: 40, borderRadius: 12, background: 'rgba(255,255,255,.88)', border: '1px solid rgba(214,224,236,.85)', boxShadow: '0 1px 2px rgba(15,23,42,.08)', display: 'grid', placeItems: 'center'}}>{children}</div>
);
export const TopBar: React.FC<{accent?: string; actions: React.ReactNode[]}> = ({accent, actions}) => (
	<div style={{background: 'rgba(244,245,248,.85)', borderBottom: '1px solid rgba(214,224,236,.6)', padding: '10px 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
		<div style={{display: 'flex', alignItems: 'baseline', gap: 6}}>
			<span style={{fontFamily: F.play, fontSize: 28, fontWeight: 700, color: K.title, lineHeight: 1.2}}>Srutam</span>
			{accent && <span style={{fontFamily: F.play, fontSize: 20, fontWeight: 500, fontStyle: 'italic', color: K.cobalt}}>{accent}</span>}
		</div>
		<div style={{display: 'flex', gap: 6}}>{actions.map((a, i) => <Squircle key={i}>{a}</Squircle>)}</div>
	</div>
);

// ---------- feed ----------
const bars = (n: number, f: (i: number) => number) => [...Array(n)].map((_, i) => f(i));
export const Waveform: React.FC<{n?: number; h?: number; played?: number; color?: string; base?: string}> = ({n = 44, h = 34, played = 0, color = '#0066FF', base = '#CBD5E1'}) => (
	<div style={{flex: 1, height: h, display: 'flex', alignItems: 'center', gap: 2, padding: '0 4px', overflow: 'hidden'}}>
		{bars(n, (i) => Math.max(5, (0.25 + 0.75 * ((Math.sin(0.42 * i) * Math.cos(0.16 * i) + 1) / 2)) * h)).map((bh, i) => (
			<i key={i} style={{display: 'block', width: 2.5, flex: 'none', height: bh, borderRadius: 1.5, background: i / n < played ? color : base}} />
		))}
	</div>
);
export type NoteState = 'summarized' | 'analyzing' | 'insights';
export const NoteCard: React.FC<{title: string; date: string; dur: string; state: NoteState; summary?: string; nextSteps?: boolean; shimmer?: number}> = ({title, date, dur, state, summary, nextSteps, shimmer = 0}) => {
	const proc = state === 'analyzing';
	const pill = proc
		? {bg: '#EBF3FF', b: '1px solid rgba(37,99,235,.2)', c: K.cobalt, label: 'Analyzing...', icon: <span style={{width: 11, height: 11, borderRadius: '50%', border: '1.6px solid #2563EB', borderTopColor: 'transparent', display: 'inline-block', transform: `rotate(${shimmer * 720}deg)`}} />}
		: state === 'summarized'
			? {bg: K.cobaltC, b: 'none', c: K.cobalt, label: 'Summarized', icon: <span>✦</span>}
			: {bg: '#BACFFC', b: 'none', c: K.onCobalt, label: 'AI Insights', icon: <span>✨</span>};
	return (
		<div style={{position: 'relative', overflow: 'hidden', borderRadius: 26, background: proc ? '#F4F8FF' : '#fff', border: `1px solid ${proc ? 'rgba(0,102,255,.4)' : '#E8EAEF'}`, boxShadow: '0 1px 4px rgba(15,23,42,.06)', padding: '14px 16px', display: 'flex', flexDirection: 'column', gap: 8, margin: '4px 0 12px'}}>
			{proc && <div style={{position: 'absolute', top: 0, bottom: 0, width: 160, left: -160 + ((shimmer * 600) % 600), background: 'linear-gradient(90deg,transparent,rgba(37,99,235,.10),transparent)'}} />}
			<div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10}}>
				<span style={{fontSize: 18, fontWeight: 700, color: K.ink, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>{title}</span>
				<span style={{flex: 'none', display: 'inline-flex', alignItems: 'center', gap: 4, padding: '4px 10px', borderRadius: 999, background: pill.bg, border: pill.b, color: pill.c, fontSize: 12, fontWeight: 600}}>{pill.icon}{pill.label}</span>
			</div>
			<div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
				<span style={{fontSize: 12.5, fontWeight: 500, color: K.sec}}>{date}</span>
				{nextSteps && <span style={{fontSize: 11, fontWeight: 600, color: K.em, background: 'rgba(16,185,129,.12)', border: '1px solid rgba(16,185,129,.25)', borderRadius: 8, padding: '2px 6px'}}>✦ Next steps</span>}
			</div>
			{proc ? (
				<div style={{fontSize: 12.5, color: K.cobalt, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>Transcribing audio and extracting insights...</div>
			) : summary ? (
				<div style={{fontSize: 13, lineHeight: '18px', color: '#334155', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden'}}>{summary}</div>
			) : null}
			<div style={{display: 'flex', alignItems: 'center', gap: 10, marginTop: 2}}>
				<div style={{width: 38, height: 38, borderRadius: '50%', background: '#0066FF', boxShadow: '0 3px 8px rgba(0,102,255,.35)', display: 'grid', placeItems: 'center'}}><MI n="play" s={19} c="#fff" /></div>
				<Waveform n={42} />
				<span style={{fontSize: 13, fontWeight: 600, color: K.ink}}>{dur}</span>
				<div style={{width: 28, height: 28, display: 'grid', placeItems: 'center'}}><MI n="more" s={20} c={K.sec} /></div>
			</div>
		</div>
	);
};
export const FeedFilters: React.FC<{count?: number; pending?: number}> = ({count = 24, pending = 1}) => (
	<div style={{display: 'flex', alignItems: 'center', gap: 8, padding: '8px 16px 2px'}}>
		<div style={{flex: 1, height: 32, background: '#EAEFF5', border: '1px solid #DCE4EE', borderRadius: 16, padding: 2, display: 'flex', gap: 4}}>
			<div style={{flex: 1, borderRadius: 14, background: '#fff', border: '1px solid #E2E8F0', boxShadow: '0 1px 3px rgba(15,23,42,.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, fontSize: 11.5, fontWeight: 600, color: K.ink, letterSpacing: 0.25}}>
				All Notes<span style={{background: K.chip, borderRadius: 999, padding: '1px 6px', fontSize: 10, fontWeight: 600}}>{count}</span>
			</div>
			<div style={{flex: 1, borderRadius: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, fontSize: 11.5, fontWeight: 500, color: K.sec}}>
				Pending AI<span style={{background: 'rgba(239,246,255,.6)', borderRadius: 999, padding: '1px 6px', fontSize: 10, fontWeight: 700, color: K.cobalt}}><span style={{fontSize: 8.5}}>✦</span> {pending}</span>
			</div>
		</div>
		<div style={{width: 76, height: 48, display: 'flex', alignItems: 'center', gap: 6}}>
			<span style={{fontSize: 12, fontWeight: 600, color: K.sec}}>Oct</span>
			<div style={{width: 36, textAlign: 'center', lineHeight: '17px'}}>
				<div style={{fontSize: 10.5, color: K.tmuted, opacity: 0.55}}>2</div>
				<div style={{fontSize: 13.5, fontWeight: 600, color: K.ink}}>3</div>
				<div style={{fontSize: 10.5, color: K.tmuted, opacity: 0.55}}>4</div>
			</div>
		</div>
	</div>
);

// ---------- bottom bar ----------
export const BottomBar: React.FC<{tab: 'notes' | 'insights'; badge?: number}> = ({tab, badge}) => {
	const T = (k: 'notes' | 'insights' | 'ai', label: string, icon: React.ReactNode, w: number) => {
		const on = tab === k;
		return (
			<div style={{flex: w, height: '100%', padding: '3px 2px', display: 'flex'}}>
				<div style={{flex: 1, borderRadius: 999, background: on ? '#fff' : 'transparent', border: on ? '0.5px solid #E2E8F0' : 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 3, fontSize: 12.5, fontWeight: on ? 700 : 500, color: on ? K.ink : K.sec}}>
					{icon}{label}
					{!on && k === 'insights' && badge ? <span style={{width: 16, height: 16, borderRadius: 8, background: K.cobalt, color: '#fff', fontSize: 10, fontWeight: 700, display: 'grid', placeItems: 'center', marginLeft: 2}}>{badge}</span> : null}
				</div>
			</div>
		);
	};
	return (
		<div style={{position: 'absolute', left: 0, right: 0, bottom: 0, padding: '28px 16px 22px', background: 'linear-gradient(rgba(244,245,248,0),rgba(244,245,248,.85) 40%,#F4F5F8)', display: 'flex', alignItems: 'center', gap: 12}}>
			<div style={{flex: 1, height: 56, borderRadius: 999, background: 'linear-gradient(90deg,#F8FAFC,#F1F5F9)', border: '1px solid #E2E8F0', padding: 4, display: 'flex'}}>
				{T('notes', 'Notes', <MI n="menu" s={16} c={tab === 'notes' ? K.ink : K.sec} />, 1)}
				{T('insights', 'Insights', <MI n="checklist" s={16} c={tab === 'insights' ? K.ink : K.sec} />, 1.25)}
				{T('ai', 'AI', <span style={{fontSize: 13, fontWeight: 700}}>✦</span>, 0.85)}
			</div>
			<div style={{width: 56, height: 56, borderRadius: '50%', background: '#FECACA', border: '1px solid rgba(252,165,165,.5)', display: 'grid', placeItems: 'center'}}>
				<div style={{width: 42, height: 42, borderRadius: '50%', background: 'linear-gradient(#EF4444,#DC2626,#B91C1C)', display: 'grid', placeItems: 'center'}}>
					<div style={{width: 24, height: 24, borderRadius: '50%', background: '#991B1B'}} />
				</div>
			</div>
		</div>
	);
};

export const FeedScreen: React.FC<{sync?: 'syncing' | 'synced' | null; spin?: number; top?: React.ReactNode}> = ({sync = null, spin = 0, top}) => (
	<Screen>
		<TopBar actions={[
			<MI key="s" n="search" />,
			...(sync ? [sync === 'synced' ? <MI key="c" n="cloudDone" c={K.em} /> : <MI key="c" n="sync" c="#60A5FA" style={{transform: `rotate(${-spin * 360}deg)`}} />] : []),
			<MI key="g" n="settings" />,
		]} />
		<FeedFilters />
		<div style={{padding: '8px 14px 0'}}>
			{top}
			<NoteCard title="Product thoughts" date="Today, 9:12 AM" dur="0:42" state="summarized" nextSteps summary="Ideas for onboarding: pair laptops with a QR scan, fix the sync retry bug before release, demo on Friday." />
			<NoteCard title="Standup recap" date="Yesterday, 6:40 PM" dur="1:18" state="summarized" summary="Release is on track. Two blockers left on the sync worker; design review moves to Thursday." />
			<NoteCard title="Pricing idea" date="Oct 1, 1:05 PM" dur="0:27" state="insights" />
		</div>
		<BottomBar tab="notes" badge={3} />
	</Screen>
);

// ---------- note details ----------
export const DetailScreen: React.FC<{tab: 'transcript' | 'summary'; transcript: string; caret?: boolean}> = ({tab, transcript, caret}) => (
	<Screen>
		<div style={{height: 56, display: 'flex', alignItems: 'center', gap: 8, padding: '0 8px 0 6px', background: 'rgba(244,245,248,.85)', borderBottom: '1px solid rgba(226,232,240,.6)'}}>
			<MI n="back" s={28} c={K.ink} />
			<span style={{flex: 1, fontSize: 18, fontWeight: 600, color: K.ink}}>Note Details</span>
			<div style={{width: 40, display: 'grid', placeItems: 'center'}}><MI n="lockOpen" s={20} c={K.sec} /></div>
			<div style={{width: 40, display: 'grid', placeItems: 'center'}}><MI n="delete" s={20} c={K.sec} /></div>
		</div>
		<div style={{padding: 16, display: 'flex', flexDirection: 'column', gap: 16}}>
			<div style={{background: '#fff', border: '0.5px solid #E2E8F0', borderRadius: 16, padding: 16}}>
				<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
					<span style={{fontSize: 18, fontWeight: 700, color: K.ink}}>Product thoughts</span>
					<div style={{width: 36, height: 36, display: 'grid', placeItems: 'center'}}><MI n="edit" s={20} c={K.cobalt} /></div>
				</div>
				<div style={{display: 'flex', alignItems: 'center', gap: 6, marginTop: 8, fontSize: 13, color: K.muted}}>Oct 03, 09:12 <span>•</span><MI n="schedule" s={13} c={K.muted} /> 0:42</div>
			</div>
			<div style={{background: '#fff', border: '0.5px solid #E2E8F0', borderRadius: 16, padding: 16, display: 'flex', flexDirection: 'column', gap: 14}}>
				<div style={{display: 'flex', alignItems: 'center', gap: 12}}>
					<div style={{width: 44, height: 44, borderRadius: '50%', background: K.cobalt, boxShadow: '0 2px 6px rgba(37,99,235,.3)', display: 'grid', placeItems: 'center'}}><MI n="play" s={22} c="#fff" /></div>
					<div style={{flex: 1, height: 40, display: 'flex', alignItems: 'center', gap: 1.2, overflow: 'hidden'}}>
						{bars(70, (i) => Math.max(6, (0.22 + 0.78 * ((Math.sin(0.38 * i) * Math.cos(0.18 * i) + 1) / 2)) * 40)).map((bh, i) => <i key={i} style={{display: 'block', width: 2, flex: 'none', height: bh, borderRadius: 2, background: '#E5E5EA'}} />)}
					</div>
					<div style={{textAlign: 'right'}}><div style={{fontSize: 12, fontWeight: 700, color: K.ink}}>0:00</div><div style={{fontSize: 10, color: K.muted}}>0:42</div></div>
				</div>
				<div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
					<span style={{fontSize: 12, fontWeight: 500, color: K.muted}}>Playback Speed</span>
					<span style={{display: 'flex', gap: 6}}>{['1.0x', '1.25x', '1.5x', '2.0x'].map((s, i) => <span key={s} style={{padding: '4px 8px', borderRadius: 999, fontSize: 11, fontWeight: i ? 500 : 700, background: i ? K.chip : K.cobalt, color: i ? K.sec : '#fff'}}>{s}</span>)}</span>
				</div>
			</div>
			<div style={{background: K.chip, borderRadius: 14, padding: 4, display: 'flex', gap: 4}}>
				{[['summary', '✦ Summary'], ['transcript', '📄 Transcript'], ['insights', '💡 Insights']].map(([k, l]) => (
					<div key={k} style={{flex: 1, height: 36, borderRadius: 10, display: 'grid', placeItems: 'center', fontSize: 12, fontWeight: tab === k ? 700 : 500, background: tab === k ? '#fff' : 'transparent', color: tab === k ? K.ink : K.muted}}>{l}</div>
				))}
			</div>
			<div style={{background: '#fff', border: '0.5px solid #E2E8F0', borderRadius: 16, padding: 16, minHeight: 250}}>
				<div style={{fontSize: 16, fontWeight: 700, color: K.cobalt}}>Full Transcript</div>
				<div style={{height: 0.5, background: '#E2E8F0', margin: '8px 0'}} />
				<div style={{fontSize: 14, lineHeight: '20px', color: K.ink, letterSpacing: 0.1}}>{transcript}{caret && <span style={{display: 'inline-block', width: 2, height: 16, background: K.cobalt, verticalAlign: -3, marginLeft: 1}} />}</div>
			</div>
		</div>
	</Screen>
);

// ---------- insights ----------
const DOT = {ideas: '#D97706', next: '#2563EB', decisions: '#0D9488'};
export const Segments: React.FC<{sel: 'ideas' | 'next' | 'decisions'; counts?: [number, number, number]}> = ({sel, counts = [3, 2, 1]}) => (
	<div style={{padding: '8px 16px'}}>
		<div style={{height: 44, borderRadius: 22, background: '#F1F5F9', border: '1px solid #E2E8F0', padding: 3, display: 'flex'}}>
			{([['ideas', 'Ideas'], ['next', 'Next Steps'], ['decisions', 'Decisions']] as const).map(([k, l], i) => {
				const on = sel === k;
				return (
					<div key={k} style={{flex: 1, borderRadius: 19, background: on ? '#fff' : 'transparent', border: on ? `1px solid ${DOT[k]}2E` : '1px solid transparent', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5, fontSize: 12, fontWeight: on ? 700 : 500, color: on ? K.ink : K.tmuted, whiteSpace: 'nowrap'}}>
						<span style={{width: 6, height: 6, borderRadius: 3, background: DOT[k], opacity: on ? 1 : 0.45, boxShadow: on ? `0 0 0 3px ${DOT[k]}38` : undefined}} />
						{l} · {counts[i]}
					</div>
				);
			})}
		</div>
	</div>
);
export const SourceChip: React.FC<{label?: string}> = ({label = 'Product thoughts'}) => (
	<span style={{display: 'inline-flex', alignItems: 'center', gap: 4, borderRadius: 8, background: '#F1F5F9', border: '1px solid #E2E8F0', padding: '3px 7px', fontSize: 10.5, fontWeight: 500, color: K.sec}}><MI n="graphicEq" s={12} c={K.cobalt} />{label}</span>
);
export const TaskRow: React.FC<{text: string; date: string; done?: number; source?: string}> = ({text, date, done = 0, source}) => (
	<div style={{background: '#fff', borderRadius: 14, border: `1px solid ${done > 0.5 ? 'rgba(16,185,129,.4)' : '#E2E8F0'}`, padding: '10px 14px', display: 'flex', flexDirection: 'column', gap: 6}}>
		<div style={{display: 'flex', alignItems: 'flex-start', gap: 10}}>
			<span style={{width: 18, height: 18, marginTop: 2, borderRadius: 2, flex: 'none', border: `2px solid ${done > 0 ? K.em : K.tmuted}`, background: done > 0 ? `rgba(16,185,129,${done})` : 'transparent', display: 'grid', placeItems: 'center'}}>{done > 0.5 && <MI n="check" s={14} c="#fff" />}</span>
			<span style={{fontSize: 14.5, lineHeight: '20px', fontWeight: 500, color: done > 0.5 ? K.tmuted : K.ink, textDecoration: done > 0.5 ? 'line-through' : 'none'}}>{text}</span>
		</div>
		<div style={{marginLeft: 30, display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
			<SourceChip label={source} />
			<span style={{fontSize: 11.5, color: K.tmuted}}>{date}</span>
		</div>
	</div>
);
// idea / decision cards, matching the Roborazzi renders in app/src/test/screenshots/insights
export const IdeaCard: React.FC<{text: string; date?: string}> = ({text, date = 'Today, 9:12 AM'}) => (
	<div style={{background: '#fff', borderRadius: 16, border: '1px solid #E2E8F0', padding: 14, display: 'flex', flexDirection: 'column', gap: 10}}>
		<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
			<span style={{fontSize: 11.5, fontWeight: 600, color: '#7C3AED', background: '#F3E8FF', border: '1px solid #E9D5FF', borderRadius: 8, padding: '3px 8px'}}>💡 Idea</span>
			<span style={{fontSize: 11.5, color: K.tmuted}}>{date}</span>
		</div>
		<div style={{fontSize: 15, fontWeight: 500, color: K.ink, lineHeight: '20px'}}>{text}</div>
		<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
			<SourceChip />
			<span style={{display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 12, fontWeight: 600, color: K.cobalt, background: '#fff', border: '1px solid #BFDBFE', borderRadius: 8, padding: '5px 10px'}}><MI n="add" s={14} c={K.cobalt} />Next step</span>
		</div>
	</div>
);
export const DecisionCard: React.FC<{text: string; why: string}> = ({text, why}) => (
	<div style={{background: '#fff', borderRadius: 16, border: '1px solid #E2E8F0', padding: 14, display: 'flex', flexDirection: 'column', gap: 8}}>
		<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
			<span style={{fontSize: 11.5, fontWeight: 600, color: '#0F766E', background: '#F0FDFA', border: '1px solid #99F6E4', borderRadius: 8, padding: '3px 8px'}}>⚖ Decision</span>
			<span style={{fontSize: 11.5, color: K.tmuted}}>Today, 9:12 AM</span>
		</div>
		<div style={{fontSize: 15, fontWeight: 500, color: K.ink}}>{text}</div>
		<div style={{fontSize: 13, color: K.sec}}>{why}</div>
		<div><SourceChip /></div>
	</div>
);
export const ReminderRow: React.FC<{when: string; text: string}> = ({when, text}) => (
	<div style={{background: '#fff', borderRadius: 14, border: '1px solid #E2E8F0', padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 8}}>
		<span style={{width: 26, height: 26, borderRadius: 8, background: K.cobaltC, display: 'grid', placeItems: 'center'}}><MI n="bell" s={15} c={K.cobalt} /></span>
		<span style={{fontSize: 13, fontWeight: 500, color: K.cobalt}}>{when}</span>
		<span style={{fontSize: 13, color: K.tmuted}}>·</span>
		<span style={{fontSize: 13, color: K.ink, flex: 1}}>{text}</span>
		<MI n="chev" s={18} c={K.tmuted} />
	</div>
);
export const ThemeCard: React.FC = () => (
	<div style={{background: '#fff', borderRadius: 16, border: '1px solid #E2E8F0', padding: 14}}>
		<div style={{fontSize: 15, fontWeight: 600, color: K.ink}}>Onboarding</div>
		<div style={{fontSize: 12.5, color: K.sec, margin: '2px 0 10px'}}>Surfaced across 3 notes</div>
		<div style={{display: 'flex', gap: 6, flexWrap: 'wrap'}}><SourceChip label="Product thoughts" /><SourceChip label="Standup recap" /><SourceChip label="Pricing idea" /></div>
	</div>
);
const DateStrip: React.FC = () => (
	<div style={{display: 'flex', alignItems: 'center', padding: '4px 16px 8px', gap: 14}}>
		<div style={{lineHeight: 1}}><div style={{fontFamily: F.play, fontWeight: 700, fontSize: 14, color: K.ink}}>Oct</div><div style={{fontSize: 10, color: K.sec}}>2026</div></div>
		<div style={{flex: 1, display: 'flex', justifyContent: 'space-around', fontSize: 15, color: K.sec}}>
			<span style={{opacity: 0.5}}>30</span><span>1</span><span>2</span><span style={{fontSize: 18, color: K.ink, fontWeight: 500}}>3</span><span style={{opacity: 0.4}}>4</span>
		</div>
		<span style={{background: K.cobalt, color: '#fff', fontSize: 13, fontWeight: 600, borderRadius: 12, padding: '9px 14px'}}>All</span>
	</div>
);
export const InsightsScreen: React.FC<{sel: 'ideas' | 'next' | 'decisions'; children: React.ReactNode}> = ({sel, children}) => (
	<Screen>
		<TopBar accent="Insights" actions={[<MI key="a" n="archive" />, <MI key="h" n="history" />, <MI key="s" n="settings" />]} />
		<Segments sel={sel} />
		<DateStrip />
		<div style={{padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12}}>{children}</div>
		<BottomBar tab="insights" />
	</Screen>
);

// ---------- settings: cloud + MCP ----------
export const Switch: React.FC<{on: boolean}> = ({on}) => (
	<span style={{width: 52, height: 32, borderRadius: 16, background: on ? K.cobalt : '#E2E8F0', border: on ? 'none' : '2px solid #94A3B8', position: 'relative', flex: 'none'}}>
		<span style={{position: 'absolute', top: on ? 4 : 6, left: on ? 24 : 6, width: on ? 24 : 16, height: on ? 24 : 16, borderRadius: '50%', background: on ? '#fff' : '#94A3B8'}} />
	</span>
);
export const CloudCard: React.FC<{keys?: number; scanPress?: number; showConnect?: boolean}> = ({keys = 1, scanPress = 0, showConnect = true}) => (
	<div style={{borderRadius: 20, background: 'rgba(255,255,255,.98)', border: '1px solid #E2E8F0', boxShadow: '0 1px 4px rgba(15,23,42,.06)', padding: 16}}>
		<div style={{borderRadius: 16, background: '#F1F5F9', border: '1px solid #E2E8F0', padding: 16, fontFamily: F.body}}>
			<div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
				<span style={{display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, fontWeight: 700, color: K.ink}}><span style={{width: 8, height: 8, borderRadius: 4, background: K.em}} />Srutam Cloud Active</span>
				<span style={{height: 30, borderRadius: 8, border: '1px solid rgba(239,68,68,.45)', padding: '0 10px', display: 'flex', alignItems: 'center', gap: 4, fontSize: 11, fontWeight: 500, color: K.crimson}}><MI n="exit" s={13} c={K.crimson} />Sign Out</span>
			</div>
			<div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 10}}>
				<span style={{display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: K.sec}}><MI n="account" s={16} c={K.sec} />alex@example.com</span>
				<span style={{height: 32, borderRadius: 8, background: '#FEF2F2', padding: '0 12px', display: 'flex', alignItems: 'center', gap: 4, fontSize: 11, color: '#991B1B'}}><MI n="sync" s={13} c="#991B1B" />Sync</span>
			</div>
			<div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 10}}>
				<div><div style={{fontSize: 13, fontWeight: 600, color: K.ink}}>Auto-Sync to Cloud</div><div style={{fontSize: 11, color: K.sec}}>Upload new notes and insights automatically</div></div>
				<Switch on />
			</div>
			{showConnect && (
				<>
					<div style={{height: 0.8, background: '#E2E8F0', margin: '14px 0'}} />
					<div style={{display: 'flex', alignItems: 'center', gap: 8}}><span style={{fontSize: 13, fontWeight: 700, color: K.ink}}>MCP Agent Keys</span><span style={{borderRadius: 6, background: '#E2E8F0', padding: '2px 6px', fontSize: 10, fontWeight: 700, color: K.sec}}>{keys}/3</span></div>
					<div style={{fontSize: 11, color: K.sec, marginTop: 2}}>Keys for Claude Code, Codex, Cursor, Gemini CLI &amp; more</div>
					<div style={{marginTop: 10, borderRadius: 14, background: '#EFF4FF', border: '1px solid rgba(37,99,235,.25)', padding: 14}}>
						<div style={{display: 'flex', alignItems: 'center', gap: 10}}>
							<span style={{width: 34, height: 34, borderRadius: 10, background: K.cobaltC, display: 'grid', placeItems: 'center'}}><MI n="qr" s={19} c={K.cobalt} /></span>
							<div><div style={{fontSize: 14, fontWeight: 700, color: K.ink}}>Connect a computer</div><div style={{fontSize: 11.5, color: K.sec}}>Run srutam-mcp init and scan the code it shows</div></div>
						</div>
						<div style={{display: 'flex', gap: 8, marginTop: 12}}>
							<span style={{flex: 1, height: 38, borderRadius: 10, background: K.cobalt, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, fontSize: 12.5, fontWeight: 600, color: '#fff', transform: `scale(${1 - scanPress * 0.05})`}}><MI n="qr" s={16} c="#fff" />Scan QR</span>
							<span style={{height: 38, borderRadius: 10, border: '1px solid #E2E8F0', padding: '0 14px', display: 'flex', alignItems: 'center', fontSize: 12.5, color: K.ink, background: '#fff'}}>Enter code</span>
						</div>
					</div>
				</>
			)}
		</div>
	</div>
);
export const SettingsScreen: React.FC<{scanPress?: number}> = ({scanPress}) => (
	<Screen bg="#FFFFFF">
		<div style={{height: 56, display: 'flex', alignItems: 'center', gap: 6, padding: '0 10px'}}><MI n="back" s={32} c={K.ink} /><span style={{fontSize: 20, fontWeight: 600, color: K.ink}}>Settings</span></div>
		<div style={{padding: '10px 16px'}}>
			<div style={{fontSize: 11, fontWeight: 700, color: K.sec, letterSpacing: 1, margin: '0 0 8px 6px'}}>CLOUD SYNC &amp; DEVELOPER BRAIN (MCP)</div>
			<CloudCard scanPress={scanPress} />
		</div>
	</Screen>
);

// ---------- pairing ----------
export const PairDialog: React.FC<{press?: number}> = ({press = 0}) => (
	<div style={{position: 'absolute', inset: 0, background: 'rgba(15,23,42,.45)', display: 'grid', placeItems: 'center'}}>
		<div style={{width: '86%', borderRadius: 26, background: 'rgba(255,255,255,.98)', boxShadow: '0 20px 50px rgba(0,0,0,.35)', padding: 20, fontFamily: F.body, textAlign: 'center'}}>
			<div style={{width: 64, height: 64, borderRadius: '50%', margin: '0 auto', background: 'radial-gradient(#DBEAFE,#EFF6FF)', border: '1.5px solid #BFDBFE', display: 'grid', placeItems: 'center'}}>
				<div style={{width: 44, height: 44, borderRadius: '50%', background: 'linear-gradient(#3B82F6,#1D4ED8)', display: 'grid', placeItems: 'center'}}><MI n="computer" s={20} c="#fff" /></div>
			</div>
			<div style={{fontSize: 18, fontWeight: 700, color: K.ink, letterSpacing: -0.3, marginTop: 12}}>Connect this computer?</div>
			<div style={{fontSize: 12, color: K.sec, marginTop: 4}}>MacBook Pro</div>
			<div style={{textAlign: 'left', marginTop: 14, borderRadius: 10, background: '#F1F5F9', border: '1px solid #E2E8F0', padding: 10, display: 'flex', flexDirection: 'column', gap: 4}}>
				{[['Computer', 'MacBook Pro'], ['srutam-mcp', '1.3.0'], ['Code expires', '4:52']].map(([a, b]) => (
					<div key={a} style={{display: 'flex', fontSize: 11.5}}><span style={{width: 96, color: K.sec}}>{a}</span><span style={{fontWeight: 500, color: K.ink}}>{b}</span></div>
				))}
			</div>
			<div style={{textAlign: 'left', fontSize: 12, lineHeight: '17px', color: K.sec, marginTop: 10}}>It will be able to read notes you have not marked private, complete tasks, and add work logs.</div>
			<div style={{textAlign: 'left', fontSize: 12, fontWeight: 700, color: '#F59E0B', marginTop: 8}}>Only continue if you just ran srutam-mcp on your own computer.</div>
			<div style={{display: 'flex', gap: 10, marginTop: 16}}>
				<span style={{flex: 1, height: 42, borderRadius: 21, background: '#F1F5F9', border: '1px solid #E2E8F0', display: 'grid', placeItems: 'center', fontSize: 14, fontWeight: 600, color: K.sec}}>Cancel</span>
				<span style={{flex: 1, height: 42, borderRadius: 21, background: 'linear-gradient(90deg,#3B82F6,#2563EB)', boxShadow: '0 4px 10px rgba(37,99,235,.35)', display: 'grid', placeItems: 'center', fontSize: 14, fontWeight: 600, color: '#fff', transform: `scale(${1 - press * 0.05})`}}>Connect</span>
			</div>
		</div>
	</div>
);
export const Toast: React.FC<{text: string; o: number}> = ({text, o}) => (
	<div style={{position: 'absolute', left: 0, right: 0, bottom: 110, display: 'flex', justifyContent: 'center', opacity: o, zIndex: 30}}>
		<span style={{background: 'rgba(32,33,36,.92)', color: '#fff', fontSize: 14, borderRadius: 18, padding: '10px 16px', fontFamily: F.body}}>{text}</span>
	</div>
);

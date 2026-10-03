import React from 'react';
import {AbsoluteFill} from 'remotion';
import {E, P, cl, rng, tf} from '../lib';

// ---------- background: starfield + drifting nebula ----------
const R = rng(7);
const STARS = [...Array(220)].map(() => ({x: R(), y: R(), r: R() * 1.4 + 0.3, p: R() * 6.28, s: R() * 0.6 + 0.2, d: R() * 0.5 + 0.1}));
export const Bg: React.FC<{t: number; energy: number; w: number; h: number}> = ({t, energy, w, h}) => (
	<AbsoluteFill style={{background: '#050714', overflow: 'hidden'}}>
		<svg width={w} height={h} style={{position: 'absolute'}}>
			{STARS.map((s, i) => (
				<circle key={i} cx={(((s.x * w - t * s.d * 12) % w) + w) % w} cy={s.y * h} r={s.r} fill="#E2E8F0" opacity={(0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * s.s * 2 + s.p))) * 0.8} />
			))}
		</svg>
		{[
			{c: '#7C3AED', sz: 1.0, x: 0.05 + 0.1 * Math.sin(t * 0.21), y: 0.1 + 0.15 * Math.sin(t * 0.17 + 1), o: 0.3 + 0.25 * energy},
			{c: '#2563EB', sz: 0.9, x: 0.68 + 0.08 * Math.sin(t * 0.19 + 2), y: 0.42 + 0.12 * Math.sin(t * 0.23), o: 0.25 + 0.25 * energy},
			{c: '#EC4899', sz: 0.75, x: 0.36 + 0.14 * Math.sin(t * 0.13 + 4), y: 0.7 + 0.08 * Math.sin(t * 0.29 + 3), o: 0.12 + 0.22 * energy},
		].map((b, i) => {
			const d = Math.max(w, h) * 0.5 * b.sz;
			return <div key={i} style={{position: 'absolute', left: b.x * w - d / 2, top: b.y * h - d / 2, width: d, height: d, borderRadius: '50%', opacity: b.o, background: `radial-gradient(circle,${b.c} 0%,${b.c}99 25%,${b.c}33 50%,transparent 70%)`}} />;
		})}
		<AbsoluteFill style={{background: 'radial-gradient(ellipse at center,transparent 55%,rgba(0,0,0,.55) 100%)'}} />
	</AbsoluteFill>
);

// ---------- text ----------
export const Kicker: React.FC<{children: React.ReactNode; style?: React.CSSProperties}> = ({children, style}) => (
	<div className="kicker" style={style}>{children}</div>
);

// words that blur-rise in, staggered; supports <i> segments via [text, italic] pairs
export const Rise: React.FC<{t: number; start: number; parts: Array<[string, boolean?]>; stag?: number; className?: string; style?: React.CSSProperties; br?: number[]}> = ({t, start, parts, stag = 0.09, className, style, br = []}) => {
	let k = 0;
	return (
		<div className={className} style={style}>
			{parts.map(([txt, it], pi) => (
				<React.Fragment key={pi}>
					{br.includes(pi) && <br />}
					{txt.split(' ').filter(Boolean).map((w, wi) => {
						const st = start + k++ * stag;
						const e = E.out(P(t, st, st + 0.6));
						return (
							<span key={wi} style={{display: 'inline-block', marginRight: '0.22em', ...tf({y: (1 - e) * 40, o: e, b: (1 - e) * 12})}}>
								{it ? <i className="grad" style={{paddingRight: '0.08em'}}>{w}</i> : w}
							</span>
						);
					})}
				</React.Fragment>
			))}
		</div>
	);
};

export const Chips: React.FC<{t: number; start: number; items: Array<[string, string]>; style?: React.CSSProperties; size?: number}> = ({t, start, items, style, size = 22}) => (
	<div style={{display: 'flex', flexWrap: 'wrap', gap: 14, ...style}}>
		{items.map(([c, col], i) => {
			const k = E.back(P(t, start + i * 0.16, start + i * 0.16 + 0.5));
			return (
				<span key={i} className="chip" style={{fontSize: size, ...tf({y: (1 - cl(k)) * 24, o: cl(k), s: 0.9 + 0.1 * cl(k)})}}>
					<span className="d" style={{background: col}} />
					{c}
				</span>
			);
		})}
	</div>
);

// ---------- icons ----------
export const Icon: React.FC<{n: 'mic' | 'app' | 'x' | 'pause' | 'stop' | 'chev'; c?: string; s?: number}> = ({n, c = '#fff', s = 20}) => (
	<svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke={c} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
		{n === 'mic' && (<><rect x="9" y="3" width="6" height="11" rx="3" fill={c} stroke="none" /><path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21" /></>)}
		{n === 'app' && (<><rect x="4" y="4" width="6.5" height="6.5" rx="2" /><rect x="13.5" y="4" width="6.5" height="6.5" rx="2" /><rect x="4" y="13.5" width="6.5" height="6.5" rx="2" /><rect x="13.5" y="13.5" width="6.5" height="6.5" rx="2" /></>)}
		{n === 'x' && <path d="M6 6l12 12M18 6L6 18" />}
		{n === 'pause' && (<><rect x="6" y="5" width="4" height="14" rx="1.5" fill={c} stroke="none" /><rect x="14" y="5" width="4" height="14" rx="1.5" fill={c} stroke="none" /></>)}
		{n === 'stop' && <rect x="6" y="6" width="12" height="12" rx="2.5" fill={c} stroke="none" />}
		{n === 'chev' && <path d="M9 6l6 6-6 6" />}
	</svg>
);

// touch ripple
export const Tap: React.FC<{t: number; at: number; x: number; y: number}> = ({t, at, x, y}) => {
	const p = P(t, at, at + 0.5);
	if (p <= 0 || p >= 1) return null;
	return (
		<div style={{position: 'absolute', left: x - 40, top: y - 40, width: 80, height: 80, borderRadius: '50%', border: '3px solid rgba(255,255,255,.9)', background: 'rgba(255,255,255,.25)', zIndex: 30, ...tf({s: 0.4 + p * 0.9, o: 1 - p})}} />
	);
};

// ---------- QR ----------
export const QR: React.FC<{n?: number; seed?: number}> = ({n = 25, seed = 42}) => {
	const r = rng(seed);
	const fin = (x: number, y: number, ox: number, oy: number) => {
		const dx = x - ox, dy = y - oy;
		if (dx < 0 || dy < 0 || dx > 6 || dy > 6) return null;
		return dx === 0 || dy === 0 || dx === 6 || dy === 6 || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4);
	};
	const cells: boolean[] = [];
	for (let y = 0; y < n; y++)
		for (let x = 0; x < n; x++) {
			let v = fin(x, y, 0, 0);
			if (v === null) v = fin(x, y, n - 7, 0);
			if (v === null) v = fin(x, y, 0, n - 7);
			if (v === null) v = (x < 8 && y < 8) || (x > n - 9 && y < 8) || (x < 8 && y > n - 9) ? false : r() < 0.48;
			cells.push(v);
		}
	return (
		<div className="qr" style={{gridTemplateColumns: `repeat(${n},1fr)`}}>
			{cells.map((v, i) => <i key={i} style={{background: v ? '#0B0F1E' : '#fff'}} />)}
		</div>
	);
};

// ---------- typed text ----------
export const typed = (s: string, t: number, a: number, b: number) => s.slice(0, Math.floor(s.length * P(t, a, b)));

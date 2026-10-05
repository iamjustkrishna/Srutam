// Shared building blocks for the ~20s social cuts (X, Instagram, LinkedIn).
import React from 'react';
import {Audio} from '@remotion/media';
import {AbsoluteFill, Img, Sequence, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import '../v1.css';
import {MI} from '../components/App';
import {Bg} from '../components/ui';
import {E, FPS, L, P, cl, tf} from '../lib';

// Scene window: renders children with local time, fades/zooms in and out.
export const Sc: React.FC<{t: number; a: number; b: number; children: (lt: number, d: number) => React.ReactNode; fade?: number}> = ({t, a, b, children, fade = 0.3}) => {
	if (t < a - 0.01 || t >= b) return null;
	const lt = t - a, d = b - a;
	const ki = E.out(P(lt, 0, fade)), ko = E.in(P(lt, d - fade, d));
	return <AbsoluteFill style={tf({s: (0.97 + 0.03 * ki) * (1 + 0.04 * ko), o: ki * (1 - ko), b: (1 - ki) * 8 + ko * 10})}>{children(lt, d)}</AbsoluteFill>;
};

// Kinetic headline: words pop in (scale + blur); [text, accent?] parts; accent = gradient italic serif.
export const Pop: React.FC<{t: number; at: number; parts: Array<[string, boolean?]>; size: number; style?: React.CSSProperties; stag?: number; serif?: boolean; br?: number[]; weight?: number}> = ({t, at, parts, size, style, stag = 0.07, serif = true, br = [], weight = 800}) => {
	let k = 0;
	return (
		<div style={{fontFamily: serif ? "'Instrument Serif', serif" : 'Inter', fontWeight: serif ? 400 : weight, fontSize: size, lineHeight: 1.04, letterSpacing: serif ? '-0.01em' : '-0.03em', color: '#F8FAFC', ...style}}>
			{parts.map(([txt, acc], pi) => (
				<React.Fragment key={pi}>
					{br.includes(pi) && <br />}
					{txt.split(' ').filter(Boolean).map((w, wi) => {
						const st = at + k++ * stag;
						const e = E.back(P(t, st, st + 0.38));
						const o = P(t, st, st + 0.12);
						return (
							<span key={wi} style={{display: 'inline-block', marginRight: '0.24em', ...tf({s: L(0.6, 1, cl(e)), y: (1 - cl(e)) * 24, o, b: (1 - o) * 10})}}>
								{acc ? <i className="grad" style={{fontFamily: "'Instrument Serif', serif", fontWeight: 400, paddingRight: '0.08em'}}>{w}</i> : w}
							</span>
						);
					})}
				</React.Fragment>
			))}
		</div>
	);
};

export const Tag: React.FC<{children: React.ReactNode; color?: string; style?: React.CSSProperties}> = ({children, color = '#A5B4FC', style}) => (
	<div style={{fontFamily: 'Inter', fontWeight: 700, fontSize: 24, letterSpacing: '.28em', textTransform: 'uppercase', color, ...style}}>{children}</div>
);

export const Term: React.FC<{title: string; children: React.ReactNode; style: React.CSSProperties; font?: number}> = ({title, children, style, font = 24}) => (
	<div className="term" style={style}>
		<div className="bar"><i style={{background: '#F87171'}} /><i style={{background: '#FBBF24'}} /><i style={{background: '#34D399'}} /><span>{title}</span></div>
		<div className="body" style={{fontSize: font}}>{children}</div>
	</div>
);

// Real Floating Dock pill (res/layout/floating_record_button.xml), recording state.
export const DockPill: React.FC<{t: number; rec: boolean; sec?: number}> = ({t, rec, sec = 0}) => {
	const c = (bg: string, ch: React.ReactNode, border = true) => <span style={{width: 38, height: 38, borderRadius: 19, background: bg, border: border ? '1px solid #475569' : 'none', display: 'grid', placeItems: 'center'}}>{ch}</span>;
	return (
		<div style={{display: 'inline-flex', alignItems: 'center', gap: 8, padding: rec ? '6px 8px 6px 12px' : '6px 8px', borderRadius: 22, background: '#0F172A', border: '1px solid #475569', boxShadow: rec ? `0 0 ${18 + 10 * Math.sin(t * 8)}px rgba(225,29,72,.55)` : '0 10px 28px rgba(0,0,0,.35)'}}>
			{rec ? (
				<>
					<span style={{color: '#EF4444', fontWeight: 700, fontSize: 13, fontFamily: 'Roboto', marginRight: 2}}>0:{String(sec).padStart(2, '0')}</span>
					{c('#1E293B', <MI n="pause" s={18} c="#fff" />)}
					{c('#E11D48', <MI n="stop" s={18} c="#fff" />, false)}
					{c('#1E293B', <MI n="close" s={16} c="#fff" />)}
				</>
			) : (
				<>
					{c('#E11D48', <MI n="mic" s={18} c="#fff" />, false)}
					{c('#1E293B', <Img src={staticFile('logo.png')} style={{width: 26, height: 26}} />)}
					{c('#1E293B', <MI n="close" s={16} c="#fff" />)}
				</>
			)}
		</div>
	);
};

export const Logo: React.FC<{size: number; style?: React.CSSProperties}> = ({size, style}) => <Img src={staticFile('logo.png')} style={{width: size, height: size, ...style}} />;

export const PlayBadge: React.FC<{size?: number}> = ({size = 26}) => (
	<span className="chip" style={{fontSize: size, padding: `${size * 0.6}px ${size * 1.15}px`, background: '#F8FAFC', color: '#0F172A', border: 'none', fontWeight: 700, fontFamily: 'Inter'}}>
		<svg width={size} height={size * 1.08} viewBox="0 0 26 28"><path d="M2 2 L24 14 L2 26 Z" fill="#0F172A" /></svg>
		Get it on Google Play
	</span>
);

export type Cue = [string, number, number]; // sfx file (in public/sfx), time, volume

// Transition whooshes, timed so the whoosh's peak (0.16s into whoosh_soft.mp3) lands on the
// moment the next scene is mid-fade-in. One per cut, all at the same soft level.
const WHOOSH_PEAK = 0.16;
export const whooshes = (cuts: number[], fade: number, vol = 0.12): Cue[] => cuts.map((c) => ['whoosh_soft', c + fade / 2 - WHOOSH_PEAK, vol]);

// Frame shell: background, content, music bed (looped, faded), sfx cues.
export const Shell: React.FC<{music: string; musicVol?: number; cues: Cue[]; energy?: (t: number) => number; children: (t: number) => React.ReactNode}> = ({music, musicVol = 0.55, cues, energy = () => 0.7, children}) => {
	const frame = useCurrentFrame();
	const {width, height, durationInFrames} = useVideoConfig();
	const t = frame / FPS, total = durationInFrames / FPS;
	return (
		<AbsoluteFill style={{background: '#050714', fontFamily: 'Inter', color: '#F8FAFC'}}>
			<Bg t={t} energy={energy(t)} w={width} h={height} />
			{children(t)}
			<AbsoluteFill style={{background: '#000', opacity: Math.max(1 - t / 0.15, P(t, total - 0.5, total))}} />
			<Audio src={staticFile(`music-samples/${music}.mp3`)} loop volume={(f) => musicVol * Math.min(1, f / FPS / 0.4) * Math.min(1, (total - f / FPS) / 1.5)} />
			{cues.map(([n, at, v], i) => (
				<Sequence key={i} from={Math.max(0, Math.round(at * FPS))} premountFor={FPS}>
					<Audio src={staticFile(`sfx/${n}.mp3`)} volume={v} />
				</Sequence>
			))}
		</AbsoluteFill>
	);
};

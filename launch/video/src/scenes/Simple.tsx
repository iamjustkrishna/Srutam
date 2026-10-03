import React from 'react';
import {Img, staticFile} from 'remotion';
import {Chips, Rise} from '../components/ui';
import {E, L, P, cl, tf, useLayout} from '../lib';

type SP = {t: number; dur: number};

// 1. Hook: "Ideas don't wait." A record dot pulses below.
export const Hook: React.FC<SP> = ({t}) => {
	const {portrait: pt, w} = useLayout();
	const d = E.back(P(t, 1.2, 1.6));
	const cy = pt ? 1080 : 640;
	return (
		<>
			<Rise t={t} start={0.25} parts={[["Ideas don't"], ['wait.', true]]} stag={0.16} className="serif" br={pt ? [1] : []}
				style={{position: 'absolute', left: 0, right: 0, top: pt ? 560 : 330, textAlign: 'center', fontSize: pt ? 150 : 160, lineHeight: 1.05}} />
			{[0, 1, 2].map((i) => {
				const rp = (((t - 1.3 - i * 0.45) % 1.35) + 1.35) % 1.35 / 1.35;
				return t > 1.3 + i * 0.45 ? <div key={i} style={{position: 'absolute', left: w / 2 - 60, top: cy - 60, width: 120, height: 120, borderRadius: '50%', border: '3px solid #EF4444', ...tf({s: L(0.3, 2.4, rp), o: (1 - rp) * 0.7})}} /> : null;
			})}
			<div style={{position: 'absolute', left: w / 2 - 22, top: cy - 22, width: 44, height: 44, borderRadius: '50%', background: '#EF4444', boxShadow: '0 0 60px #EF4444', ...tf({s: cl(d) * (1 + 0.1 * Math.sin(t * 8)), o: cl(d * 3)})}} />
		</>
	);
};

// 2. Title: logo + Srutam 2.5
export const Title: React.FC<SP> = ({t}) => {
	const {portrait: pt, w} = useLayout();
	const lg = E.back(P(t, 0.1, 0.8));
	return (
		<>
			<Img src={staticFile('logo.png')} style={{position: 'absolute', left: w / 2 - 150, top: pt ? 420 : 90, width: 300, height: 300, ...tf({s: L(0.6, 1, cl(lg)), o: P(t, 0.1, 0.4), b: (1 - cl(lg)) * 10})}} />
			<div style={{position: 'absolute', left: 0, right: 0, top: pt ? 770 : 410, textAlign: 'center'}}>
				<Rise t={t} start={0.45} parts={[['Srutam'], ['2.5', true]]} stag={0.18} className="serif" style={{fontSize: pt ? 190 : 230, lineHeight: 1}} />
				<div style={{fontSize: pt ? 40 : 38, color: '#A5B4FC', letterSpacing: '.04em', marginTop: 24, fontWeight: 500, ...tf({o: E.out(P(t, 1.1, 1.6)), y: (1 - E.out(P(t, 1.1, 1.6))) * 16})}}>
					Pure Voice, Crystallized Thought.
				</div>
			</div>
		</>
	);
};

// 6. "New in 2.5" slam
export const NewIn: React.FC<SP> = ({t}) => {
	const {portrait: pt, w, h} = useLayout();
	const k = E.expo(P(t, 0.05, 0.45));
	const r = E.out(P(t, 0.1, 0.9));
	const cy = h / 2 - (pt ? 60 : 30);
	return (
		<>
			<div style={{position: 'absolute', left: w / 2 - 300, top: cy - 300, width: 600, height: 600, borderRadius: '50%', border: '4px solid #A78BFA', ...tf({s: L(0.4, 2.6, r), o: (1 - r) * 0.9})}} />
			<div style={{position: 'absolute', left: 0, right: 0, top: cy - 290, textAlign: 'center', fontSize: 44, fontWeight: 600, letterSpacing: '.4em', textTransform: 'uppercase', color: '#C7D2FE', ...tf({o: P(t, 0.35, 0.6)})}}>New in</div>
			<div className="serif" style={{position: 'absolute', left: 0, right: 0, top: cy - 230, textAlign: 'center', fontSize: pt ? 400 : 420, lineHeight: 1, ...tf({s: L(2.4, 1, k), o: P(t, 0.05, 0.2), b: (1 - k) * 24})}}>
				<i className="grad" style={{paddingRight: 30, backgroundPosition: `${100 - 200 * P(t, 0.1, 2)}% 0`}}>2.5</i>
			</div>
			<Chips t={t} start={0.85} size={pt ? 30 : 30} items={[['Cloud Sync', '#60A5FA'], ['Srutam MCP', '#C084FC'], ['QR Pairing', '#34D399']]}
				style={{position: 'absolute', left: 60, right: 60, top: cy + 230, justifyContent: 'center'}} />
		</>
	);
};

// 10. Trust
const TR: Array<Array<[string, boolean?]>> = [
	[['Key-bound authorization.']],
	[['Private notes,'], ['enforced.', true]],
	[["Agents can't delete your notes."]],
	[['Account deletion,'], ['on request.', true]],
];
export const Trust: React.FC<SP> = ({t}) => {
	const {portrait: pt} = useLayout();
	return (
		<>
			<div className="kicker" style={{position: 'absolute', left: 0, right: 0, top: pt ? 480 : 200, textAlign: 'center', ...tf({o: P(t, 0.1, 0.5)})}}>Built to be trusted</div>
			<div style={{position: 'absolute', left: 40, right: 40, top: pt ? 580 : 290}}>
				{TR.map((parts, i) => {
					const st = 0.35 + i * 0.75;
					const k = E.expo(P(t, st, st + 0.35));
					const dim = i < 3 ? 1 - 0.5 * P(t, st + 0.75, st + 0.95) : 1;
					return (
						<div key={i} className="trow serif" style={{fontSize: pt ? 64 : 78, marginBottom: pt ? 34 : 22, ...tf({x: (1 - k) * -60, o: k * dim, b: (1 - k) * 16, s: L(1.08, 1, k)})}}>
							<span className="tick" />
							<span>{parts.map(([s, it], j) => (it ? <i key={j} className="grad"> {s}</i> : <span key={j}>{s}</span>))}</span>
						</div>
					);
				})}
			</div>
		</>
	);
};

// 11. End card
export const End: React.FC<SP> = ({t, dur}) => {
	const {portrait: pt} = useLayout();
	const k = E.out(P(t, 0.1, 0.9));
	const a = E.out(P(t, 0.9, 1.5));
	const b = E.back(P(t, 1.7, 2.3));
	return (
		<div style={{position: 'absolute', inset: 0, ...tf({o: 1 - P(t, dur - 0.9, dur)})}}>
			<div style={{position: 'absolute', left: 0, right: 0, top: pt ? 380 : 120, textAlign: 'center', ...tf({s: L(1.06, 1, k), o: k, b: (1 - k) * 16})}}>
				<Img src={staticFile('logo.png')} style={{width: pt ? 260 : 200, height: pt ? 260 : 200}} />
				<div className="serif" style={{fontSize: pt ? 170 : 190, lineHeight: 1}}>Srutam <i className="grad" style={{paddingRight: 16}}>2.5</i></div>
			</div>
			<div className="serif" style={{position: 'absolute', left: 40, right: 40, top: pt ? 920 : 560, textAlign: 'center', fontSize: pt ? 64 : 64, color: '#E2E8F0', ...tf({y: (1 - a) * 20, o: a})}}>
				Say it once.{pt ? <br /> : ' '}<i className="grad">Use it everywhere.</i>
			</div>
			<div style={{position: 'absolute', left: 0, right: 0, top: pt ? 1150 : 690, display: 'flex', flexDirection: pt ? 'column' : 'row', alignItems: 'center', justifyContent: 'center', gap: 20, ...tf({y: (1 - cl(b)) * 24, o: cl(b)})}}>
				<span className="chip" style={{fontSize: 28, padding: '16px 30px', background: '#F8FAFC', color: '#0F172A', border: 'none', fontWeight: 700}}>
					<svg width="26" height="28" viewBox="0 0 26 28"><path d="M2 2 L24 14 L2 26 Z" fill="#0F172A" /></svg>
					Get it on Google Play
				</span>
				<span className="chip mono" style={{fontSize: 24, padding: '16px 30px', color: '#C4B5FD'}}><span className="p">$</span>&nbsp;npx -y srutam-mcp init</span>
			</div>
			<div style={{position: 'absolute', left: 0, right: 0, top: pt ? 1380 : 800, textAlign: 'center', fontSize: pt ? 30 : 30, letterSpacing: '.12em', color: '#94A3B8', fontWeight: 500, ...tf({o: P(t, 2.3, 2.8)})}}>SRUTAM.IAMJUSTKRISHNA.SPACE</div>
		</div>
	);
};

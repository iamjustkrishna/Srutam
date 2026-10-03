import React from 'react';
import {Img, staticFile} from 'remotion';
import {DecisionCard, DetailScreen, FeedScreen, IdeaCard, InsightsScreen, MI, NoteCard, Phone, ReminderRow, TaskRow, ThemeCard, Toast} from '../components/App';
import {Chips, Kicker, Rise, Tap} from '../components/ui';
import {E, L, P, cl, tf, useLayout} from '../lib';

type SP = {t: number; dur: number};

const Side: React.FC<{t: number; dur: number; kicker: string; parts: Array<[string, boolean?]>; sub: string; br?: number[]}> = ({t, dur, kicker, parts, sub, br}) => {
	const {portrait: pt} = useLayout();
	const o = 1 - P(t, dur - 0.5, dur - 0.1);
	return (
		<div style={{position: 'absolute', left: pt ? 70 : 170, right: pt ? 70 : undefined, top: pt ? 150 : 250, width: pt ? undefined : 820, textAlign: pt ? 'center' : 'left', ...tf({o})}}>
			<Kicker style={tf({o: P(t, 0.1, 0.5)})}>{kicker}</Kicker>
			<Rise t={t} start={0.2} parts={parts} br={br} className="serif h1" style={{marginTop: 22, fontSize: 104}} />
			<div className="sub" style={{marginTop: 26, maxWidth: pt ? undefined : 700, fontSize: pt ? 32 : 30, ...tf({o: E.out(P(t, 0.8, 1.3)), y: (1 - E.out(P(t, 0.8, 1.3))) * 14})}}>{sub}</div>
		</div>
	);
};
const phonePos = (pt: boolean, w: number): React.CSSProperties => (pt ? {left: w / 2 - 205, top: 700} : {left: 1175, top: 110});

// a third-party app, to show the dock works anywhere
export const OtherApp: React.FC = () => (
	<div style={{position: 'absolute', inset: 0, paddingTop: 52, padding: '52px 18px 0', background: '#FFFBF5', fontFamily: 'Roboto'}}>
		<div style={{display: 'flex', alignItems: 'center', gap: 12, paddingBottom: 18}}>
			<div style={{width: 40, height: 40, borderRadius: 12, background: '#F97316'}} />
			<div style={{fontWeight: 700, fontSize: 19, color: '#1F2937'}}>Morning Read</div>
		</div>
		<div style={{height: 190, borderRadius: 22, background: 'linear-gradient(135deg,#FED7AA,#FDBA74 40%,#FB923C)', marginBottom: 18}} />
		<div style={{fontFamily: "'Instrument Serif'", fontSize: 32, lineHeight: 1.1, color: '#111827', marginBottom: 14}}>The quiet power of thinking out loud</div>
		{[92, 100, 96, 84, 98, 70, 94, 88, 76, 90].map((wd, i) => <div key={i} style={{height: 11, width: `${wd}%`, borderRadius: 6, background: '#E5E7EB', marginBottom: 12}} />)}
	</div>
);

// real dock: res/layout/floating_record_button.xml (docked left by default)
const circle = (bg: string, child: React.ReactNode, s = 1, border = true): React.ReactNode => (
	<span style={{width: 38, height: 38, borderRadius: 19, background: bg, border: border ? '1px solid #475569' : 'none', display: 'grid', placeItems: 'center', flex: 'none', transform: `scale(${s})`}}>{child}</span>
);
export const Dock: React.FC<SP> = ({t, dur}) => {
	const {portrait: pt, w} = useLayout();
	const ki = E.out(P(t, 0.1, 0.8));
	const expand = E.back(P(t, 1.35, 1.7)) * (1 - P(t, 6.0, 6.2));
	const rec = t >= 2.55 && t < 6.0;
	const sec = Math.max(0, Math.floor(t - 2.55));
	const recPress = 1 - 0.12 * P(t, 2.35, 2.45) * (1 - P(t, 2.45, 2.55));
	const stopPress = 1 - 0.12 * P(t, 5.85, 5.95) * (1 - P(t, 5.95, 6.05));
	const toast = P(t, 6.1, 6.3) * (1 - P(t, 7.6, 7.9));
	return (
		<>
			<Side t={t} dur={dur} kicker="01  —  Capture" parts={[['One tap.'], ['From any app.', true]]} br={[1]} sub="The Floating Dock rests on the edge of your screen. Tap, speak, done." />
			{!pt && <Chips t={t} start={4.2} items={[['Floating Dock', '#EF4444'], ['Quick Settings tile', '#A78BFA'], ['Notification shortcut', '#34D399']]} style={{position: 'absolute', left: 170, top: 770, width: 900}} />}
			<Phone style={{...phonePos(pt, w), ...tf({y: (1 - ki) * 120, o: ki * (1 - P(t, dur - 0.4, dur)), s: pt ? 1.02 : 1})}}>
				<OtherApp />
				{/* collapsed edge tab: 52dp, logo, embedded 8dp into the left edge */}
				<div style={{position: 'absolute', left: -8, top: 300, width: 52, height: 52, padding: 8, borderRadius: '0 22px 22px 0', background: '#0F172A', border: '1.2px solid rgba(255,255,255,.3)', opacity: expand > 0.05 ? 0 : 1, zIndex: 5}}>
					<Img src={staticFile('logo.png')} style={{width: 36, height: 36, marginLeft: 4}} />
				</div>
				{expand > 0.02 && (
					<div style={{position: 'absolute', left: 8, top: 300, padding: rec ? '6px 8px 6px 12px' : '6px 8px', borderRadius: 22, background: '#0F172A', border: '1px solid #475569', display: 'flex', alignItems: 'center', gap: 8, transformOrigin: 'left center', boxShadow: '0 10px 28px rgba(0,0,0,.35)', zIndex: 5, ...tf({sx: cl(expand), o: cl(expand * 3)})}}>
						{rec ? (
							<>
								<span style={{color: '#EF4444', fontWeight: 700, fontSize: 13, marginRight: 2, fontFamily: 'Roboto', fontVariantNumeric: 'tabular-nums'}}>0:{String(sec).padStart(2, '0')}</span>
								{circle('#1E293B', <MI n="pause" s={18} c="#fff" />)}
								{circle('#E11D48', <MI n="stop" s={18} c="#fff" />, stopPress, false)}
								{circle('#1E293B', <MI n="close" s={16} c="#fff" />)}
							</>
						) : (
							<>
								{circle('#E11D48', <MI n="mic" s={18} c="#fff" />, recPress, false)}
								{circle('#1E293B', <Img src={staticFile('logo.png')} style={{width: 26, height: 26}} />)}
								{circle('#1E293B', <MI n="close" s={16} c="#fff" />)}
							</>
						)}
					</div>
				)}
				<Tap t={t} at={1.15} x={18} y={326} />
				<Tap t={t} at={2.3} x={35} y={326} />
				<Tap t={t} at={5.8} x={148} y={326} />
				<Toast text="Voice note saved" o={toast} />
			</Phone>
		</>
	);
};

// 4. Transcription: the new note analyzes in the feed, then its transcript
const TRANSCRIPT = "Quick thought on the app. Onboarding should let people pair a laptop by scanning a QR code instead of pasting keys. Fix the sync retry bug before release. Remind me: demo with Priya, Friday at four. And we've decided, transcription stays on device.";
export const Transcribe: React.FC<SP> = ({t, dur}) => {
	const {portrait: pt, w} = useLayout();
	const ki = E.out(P(t, 0.0, 0.5));
	const toDetail = P(t, 1.7, 1.95);
	const shown = TRANSCRIPT.slice(0, Math.floor(TRANSCRIPT.length * P(t, 2.1, Math.min(dur - 0.5, 4.3))));
	return (
		<>
			<Side t={t} dur={dur} kicker="02  —  Transcribe" parts={[['Transcribed'], ['on your phone.', true]]} br={[1]} sub="On-device speech recognition turns your voice into text. No upload needed." />
			<Phone style={{...phonePos(pt, w), ...tf({o: ki, s: pt ? 1.02 : 1})}}>
				<div style={{position: 'absolute', inset: 0, opacity: 1 - toDetail}}>
					<FeedScreen top={<NoteCard title="Voice note" date="Today, 9:41 AM" dur="0:07" state="analyzing" shimmer={t * 0.9} />} />
				</div>
				<Tap t={t} at={1.45} x={190} y={200} />
				<div style={{position: 'absolute', inset: 0, opacity: toDetail, transform: `translateX(${(1 - E.out(toDetail)) * 60}px)`}}>
					<DetailScreen tab="transcript" transcript={shown} caret={Math.floor(t * 3) % 2 === 0 && t < dur - 0.6} />
				</div>
			</Phone>
		</>
	);
};

// 5. Organize: the note's Insights, as they look in the app
export const Organize: React.FC<SP> = ({t, dur}) => {
	const {portrait: pt, w} = useLayout();
	const out = E.in(P(t, dur - 0.45, dur));
	const ex = pt ? w / 2 : 355, ey = pt ? 380 : 560;
	const CARDS = [
		<IdeaCard key="i" text="Pair a laptop by scanning a QR code" />,
		<TaskRow key="t" text="Fix the sync retry bug" date="Today, 9:12 AM" />,
		<ReminderRow key="r" when="Fri, 4:00 PM" text="Demo with Priya" />,
		<DecisionCard key="d" text="Transcription stays on device" why="Privacy and offline access" />,
		<ThemeCard key="th" />,
	];
	const pos = (i: number) => (pt ? {x: 140, y: [520, 736, 875, 965, 1195][i], wd: 800} : i < 4 ? {x: 760 + (i % 2) * 540, y: 330 + Math.floor(i / 2) * 230, wd: 500} : {x: 760, y: 790, wd: 500});
	return (
		<>
			<div style={{position: 'absolute', left: pt ? 60 : 760, right: pt ? 60 : undefined, top: pt ? 150 : 110, textAlign: pt ? 'center' : 'left', ...tf({o: E.out(P(t, 0.1, 0.6)) * (1 - out)})}}>
				<Kicker>03  —  Organize</Kicker>
				<Rise t={t} start={0.2} parts={[['Rambles in.'], ['Clarity out.', true]]} br={pt ? [1] : []} className="serif h1" style={{marginTop: 18, fontSize: pt ? 104 : 92}} />
			</div>
			{!pt && (
				<Phone style={{left: 150, top: 110, ...tf({s: 0.86, o: 1 - out})}}>
					<InsightsScreen sel="next">
						<TaskRow text="Fix the sync retry bug" date="Today, 9:12 AM" />
						<ReminderRow when="Fri, 4:00 PM" text="Demo with Priya" />
						<TaskRow text="Send the revised proposal" date="Yesterday, 6:40 PM" source="Standup recap" />
					</InsightsScreen>
				</Phone>
			)}
			{CARDS.map((c, i) => {
				const st = 0.9 + i * 0.45;
				const k = E.back(P(t, st, st + 0.6));
				const kf = P(t, st, st + 0.3);
				const p = pos(i);
				// cards render at app scale x1.35 so they read on video
				const s = pt ? 1.4 : 1.3;
				return (
					<div key={i} style={{position: 'absolute', left: p.x, top: p.y, width: p.wd / s, transformOrigin: 'top left', fontFamily: 'Roboto', ...tf({x: (ex - p.x - p.wd / 2) * (1 - k), y: (ey - p.y - 60) * (1 - k) + Math.sin(t * 1.3 + i) * 4 - out * 30, s: L(0.2, 1, k) * s, o: kf * (1 - out)}), filter: 'drop-shadow(0 18px 40px rgba(0,0,0,.45))'}}>
						{c}
					</div>
				);
			})}
		</>
	);
};

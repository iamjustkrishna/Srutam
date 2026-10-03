import React from 'react';
import {CloudCard, FeedScreen, InsightsScreen, PairDialog, Phone, ReminderRow, Screen, SettingsScreen, TaskRow, Toast} from '../components/App';
import {Chips, Kicker, QR, Rise, Tap, typed} from '../components/ui';
import {E, L, P, cl, tf, useLayout} from '../lib';

type SP = {t: number; dur: number};

const Head: React.FC<{t: number; kicker?: string; parts: Array<[string, boolean?]>; br?: number[]; top: number; left?: number; size: number; align?: 'center' | 'left'; width?: number}> = ({t, kicker, parts, br, top, left = 0, size, align = 'center', width}) => (
	<div style={{position: 'absolute', left, right: align === 'center' ? left : undefined, width, top, textAlign: align}}>
		{kicker && <Kicker style={tf({o: P(t, 0.05, 0.4)})}>{kicker}</Kicker>}
		<Rise t={t} start={0.1} parts={parts} br={br} className="serif" style={{fontSize: size, lineHeight: 1.02, marginTop: kicker ? 16 : 0}} />
	</div>
);
const bez = (p: number, a: number[], b: number[], c: number[], d: number[]) => {
	const u = 1 - p;
	return [0, 1].map((k) => u * u * u * a[k] + 3 * u * u * p * b[k] + 3 * u * p * p * c[k] + p * p * p * d[k]);
};

// 7. Cloud Sync: feed syncs (top-bar Sync -> CloudDone), Settings shows the cloud card
export const Cloud: React.FC<SP> = ({t}) => {
	const {portrait: pt} = useLayout();
	const path = pt ? {a: [300, 1010], b: [360, 1180], c: [470, 1180], d: [540, 1100]} : {a: [690, 600], b: [860, 440], c: [1000, 440], d: [1100, 560]};
	const synced = t > 3.0;
	const cs = pt ? 1.25 : 1.45;
	return (
		<>
			<Head t={t} kicker="New  ·  Cloud Sync" parts={[['Every note,'], ['everywhere you work.', true]]} br={pt ? [1] : []} top={pt ? 170 : 80} size={84} />
			<svg width={pt ? 1080 : 1920} height={pt ? 1920 : 1080} style={{position: 'absolute', left: 0, top: 0}}>
				<defs><linearGradient id="lg1" x1="0" x2="1"><stop offset="0" stopColor="#60A5FA" /><stop offset="1" stopColor="#C084FC" /></linearGradient></defs>
				<path d={`M ${path.a} C ${path.b}, ${path.c}, ${path.d}`} fill="none" stroke="url(#lg1)" strokeWidth={4} strokeDasharray="10 14" strokeLinecap="round" strokeDashoffset={-t * 60} opacity={P(t, 0.5, 0.9) * (1 - P(t, 3.2, 3.6))} />
			</svg>
			<Phone style={{left: pt ? 70 : 290, top: pt ? 470 : 150, ...tf({s: pt ? 0.72 : 0.86, y: (1 - E.out(P(t, 0.1, 0.7))) * 80, o: P(t, 0.1, 0.5)})}}>
				<FeedScreen sync={synced ? 'synced' : 'syncing'} spin={t} />
			</Phone>
			{[0, 1, 2, 3].map((i) => {
				const p = E.io(P(t, 0.9 + i * 0.4, 1.7 + i * 0.4));
				if (p <= 0 || p >= 1) return null;
				const [x, y] = bez(p, path.a, path.b, path.c, path.d);
				return <div key={i} style={{position: 'absolute', left: x - 18, top: y - 12, width: 36, height: 24, borderRadius: 7, background: 'linear-gradient(135deg,#60A5FA,#A78BFA)', boxShadow: '0 0 24px rgba(129,140,248,.9)'}} />;
			})}
			<div style={{position: 'absolute', left: pt ? 120 : 1110, top: pt ? 1120 : 360, width: pt ? 840 / cs : 640 / cs, transformOrigin: 'top left', fontFamily: 'Roboto', ...tf({x: (1 - E.out(P(t, 0.3, 0.9))) * 80, o: P(t, 0.3, 0.7), s: cs}), filter: 'drop-shadow(0 20px 40px rgba(0,0,0,.45))'}}>
				<CloudCard showConnect={false} />
			</div>
			{!pt && <Chips t={t} start={3.1} items={[['Background sync', '#60A5FA'], ['Google One Tap sign-in', '#FBBF24'], ['Private notes stay private', '#C084FC']]} style={{position: 'absolute', left: 0, right: 0, top: 930, justifyContent: 'center'}} />}
		</>
	);
};

// 8. Connect: `srutam-mcp init` shows a code, the phone approves (mcp-server/src/cli/pairPrompt.ts)
const T8: Array<[number, React.ReactNode, number?, string?]> = [
	[0.5, <span className="p">$</span>, 0.8, 'npx -y srutam-mcp init'],
	[1.4, <span className="w" style={{fontWeight: 600}}>Srutam MCP - Developer Setup</span>],
	[1.6, <span className="m">To connect this computer, Srutam shows a code here and you scan it with your phone.</span>],
	[1.85, <><span className="m">Connect by scanning a code? [Y/n]</span> <span className="w">y</span></>],
	[2.0, 'QR'],
	[4.3, <>Your phone (<span className="b">k***@gmail.com</span>) approved <span className="w">&quot;MacBook Pro&quot;</span>.</>],
	[4.6, <><span className="m">Connect this computer to k***@gmail.com? [Y/n]:</span> <span className="w">y</span></>],
	[4.95, <><span className="g">✓</span> Configured Claude Code   <span className="g">✓</span> Configured Cursor</>],
	[5.25, <span className="g" style={{fontWeight: 600}}>Setup complete!</span>],
];
export const TermLines: React.FC<{t: number; lines: Array<[number, React.ReactNode, number?, string?]>; extra?: (i: number) => React.ReactNode}> = ({t, lines, extra}) => (
	<>
		{lines.map(([at, node, tdur, ty], i) => {
			const a = P(t, at, at + 0.2);
			return (
				<div key={i} className="tl" style={{opacity: a, transform: `translateY(${(1 - E.out(a)) * 10}px)`}}>
					{extra?.(i) ?? (
						<>
							{node}
							{ty !== undefined && <> <span className="w">{typed(ty, t, at + 0.05, at + (tdur ?? 0.6))}</span></>}
						</>
					)}
				</div>
			);
		})}
	</>
);
const Scanner: React.FC<{t: number}> = ({t}) => (
	<Screen bg="#000">
		<div style={{position: 'absolute', inset: 0, background: 'radial-gradient(ellipse at 50% 45%,#2a2f3a,#000 75%)'}} />
		<div style={{position: 'absolute', left: 0, right: 0, top: 70, textAlign: 'center', color: '#fff', fontSize: 16, fontFamily: 'Roboto'}}>Scan code</div>
		<div style={{position: 'absolute', left: 69, top: 250, width: 250, height: 250}}>
			<div style={{position: 'absolute', inset: 30, opacity: 0.9, transform: 'rotate(-4deg)'}}><QR /></div>
			{[{left: 0, top: 0, borderWidth: '4px 0 0 4px', borderRadius: '16px 0 0 0'}, {right: 0, top: 0, borderWidth: '4px 4px 0 0', borderRadius: '0 16px 0 0'}, {left: 0, bottom: 0, borderWidth: '0 0 4px 4px', borderRadius: '0 0 0 16px'}, {right: 0, bottom: 0, borderWidth: '0 4px 4px 0', borderRadius: '0 0 16px 0'}].map((s, i) => (
				<div key={i} style={{position: 'absolute', width: 48, height: 48, borderStyle: 'solid', borderColor: '#fff', ...s}} />
			))}
			<div style={{position: 'absolute', inset: 0, borderRadius: 16, background: '#fff', opacity: 0.6 * P(t, 3.4, 3.48) * (1 - P(t, 3.48, 3.6))}} />
		</div>
	</Screen>
);
export const Connect: React.FC<SP> = ({t}) => {
	const {portrait: pt} = useLayout();
	const q = E.back(P(t, 2.0, 2.5));
	const pk = E.out(P(t, 2.1, 2.8));
	const scanning = t >= 2.8 && t < 3.55;
	const dialog = t >= 3.55 && t < 4.3;
	const done = t >= 4.3;
	return (
		<>
			<Head t={t} kicker="New  ·  Srutam MCP" parts={[['No keys to paste.'], ['Just scan.', true]]} br={[1]} top={pt ? 150 : 170} left={pt ? 0 : 150} width={pt ? undefined : 680} size={pt ? 100 : 96} align={pt ? 'center' : 'left'} />
			<div className="term" style={{...(pt ? {left: 50, top: 520, width: 980, height: 700} : {left: 830, top: 150, width: 960, height: 760}), ...tf({x: (1 - E.out(P(t, 0.1, 0.7))) * 90, o: P(t, 0.1, 0.5)})}}>
				<div className="bar"><i style={{background: '#F87171'}} /><i style={{background: '#FBBF24'}} /><i style={{background: '#34D399'}} /><span>~/code — zsh</span></div>
				<div className="body" style={{fontSize: pt ? 19 : 20}}>
					<TermLines t={t} lines={T8} extra={(i) => i === 4 ? (
						<div style={{display: 'flex', gap: 30, alignItems: 'center', margin: '12px 0 14px'}}>
							<div style={{width: 210, ...tf({s: L(0.6, 1, cl(q)), o: cl(q)})}}><QR /></div>
							<div className="m" style={{fontSize: 17, lineHeight: 1.7, opacity: t > 4.3 ? 0.35 : 1}}>Waiting for your phone{'.'.repeat(1 + (Math.floor(t * 3) % 3))}</div>
						</div>
					) : undefined} />
				</div>
			</div>
			<Phone style={{left: pt ? 590 : 330, top: pt ? 960 : 245, ...tf({x: (1 - pk) * -60, y: (1 - pk) * 300, s: pt ? 0.55 : 0.6, o: P(t, 2.1, 2.4), r: L(-6, -2, pk)})}}>
				{!scanning && <SettingsScreen scanPress={P(t, 2.5, 2.6) * (1 - P(t, 2.62, 2.75))} />}
				{!scanning && !dialog && !done && <Tap t={t} at={2.45} x={130} y={386} />}
				{scanning && <Scanner t={t} />}
				{dialog && <PairDialog press={P(t, 4.0, 4.1) * (1 - P(t, 4.12, 4.25))} />}
				{dialog && <Tap t={t} at={3.95} x={278} y={606} />}
				<Toast text={'Connected "MacBook Pro"'} o={P(t, 4.3, 4.45)} />
			</Phone>
		</>
	);
};

// 9. Agent: Claude Code reads tasks via MCP, does the work, completes it; the phone ticks
const T9: Array<[number, React.ReactNode, number?, string?]> = [
	[0.4, <span className="p">&gt;</span>, 0.9, "What's on my plate from my voice notes?"],
	[1.5, <><span className="g">●</span> <span className="v">srutam</span> · <span className="b">list_action_items</span><span className="m">(status: &quot;pending&quot;)</span></>],
	[1.75, <span className="m">{'  ⎿  2 open tasks · 3 related notes'}</span>],
	[2.05, <span className="w" style={{color: '#E2E8F0'}} />, 1.3, "From this morning's note: fix the sync retry bug before release. You also want laptops to pair by scanning a QR code."],
	[3.6, <span className="p">&gt;</span>, 0.6, 'Fix it, then mark it done.'],
	[4.4, <><span className="g">●</span> <span className="w">Edit</span> <span className="m">cloud/CloudSyncWorker.kt</span> <span className="g">+12</span> <span className="p">−3</span></>],
	[4.8, <><span className="g">●</span> <span className="w">Bash</span> <span className="m">./gradlew test</span>  <span className="g">✓ passed</span></>],
	[5.25, <><span className="g">●</span> <span className="v">srutam</span> · <span className="b">update_action_item</span><span className="m">(done, agent:claude-code)</span></>],
	[5.6, <span className="g">{'  ⎿  ✓ Completed · synced to your phone'}</span>],
];
const CLIENTS = ['Claude Code', 'Codex', 'Gemini CLI', 'Cursor', 'VS Code', 'Windsurf', 'OpenCode', 'Zed', 'Kiro', 'Cline', 'Claude Desktop', 'Antigravity', 'Qwen Code', 'Crush', 'Amp', 'Copilot CLI'];
const DOTS = ['#60A5FA', '#A78BFA', '#F472B6', '#34D399', '#FBBF24'];
export const Agent: React.FC<SP> = ({t}) => {
	const {portrait: pt} = useLayout();
	const c = P(t, 5.65, 5.85);
	const term = pt ? {left: 40, top: 480, width: 1000, height: 640} : {left: 700, top: 180, width: 1100, height: 670};
	return (
		<>
			<Head t={t} parts={[['Your voice notes,'], ['inside your AI coding agent.', true]]} br={pt ? [1] : []} top={pt ? 140 : 50} size={pt ? 72 : 76} />
			<div className="term" style={{...term, ...tf({x: (1 - E.out(P(t, 0.1, 0.7))) * 90, o: P(t, 0.1, 0.5)})}}>
				<div className="bar"><i style={{background: '#F87171'}} /><i style={{background: '#FBBF24'}} /><i style={{background: '#34D399'}} /><span>claude — ~/srutam</span></div>
				<div className="body" style={{fontSize: pt ? 20 : 22}}><TermLines t={t} lines={T9} /></div>
			</div>
			{pt ? (
				<div style={{position: 'absolute', left: 90, top: 1160, width: 900 / 1.6, transformOrigin: 'top left', fontFamily: 'Roboto', ...tf({s: 1.6, o: P(t, 0.8, 1.2)}), filter: 'drop-shadow(0 18px 40px rgba(0,0,0,.45))'}}>
					<TaskRow text="Fix the sync retry bug" date="Today, 9:12 AM" done={c} />
				</div>
			) : (
				<Phone style={{left: 170, top: 125, ...tf({y: (1 - E.out(P(t, 0.2, 0.9))) * 100, o: P(t, 0.2, 0.6), s: 0.76})}}>
					<InsightsScreen sel="next">
						<TaskRow text="Fix the sync retry bug" date="Today, 9:12 AM" done={c} />
						<ReminderRow when="Fri, 4:00 PM" text="Demo with Priya" />
						<TaskRow text="Send the revised proposal" date="Yesterday, 6:40 PM" source="Standup recap" />
					</InsightsScreen>
				</Phone>
			)}
			<div style={{position: 'absolute', left: 0, right: 0, top: pt ? 1420 : 868, textAlign: 'center', fontSize: 20, fontWeight: 600, letterSpacing: '.3em', textTransform: 'uppercase', color: '#818CF8', opacity: P(t, 0.8, 1.3)}}>Works with 16+ assistants</div>
			<div className="marq" style={{top: pt ? 1465 : 905, opacity: P(t, 0.8, 1.3), transform: `translateX(${-((t * 140) % 2300)}px)`}}>
				{[...CLIENTS, ...CLIENTS].map((cn, i) => (
					<span key={i} className="chip" style={{fontSize: 22, padding: '10px 22px'}}><span className="d" style={{background: DOTS[i % 5]}} />{cn}</span>
				))}
			</div>
		</>
	);
};

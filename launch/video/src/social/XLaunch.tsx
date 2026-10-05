// X / Twitter, 16:9, 20s. Audience: developers & AI-tool early adopters.
// Stop-scroll: a terminal answering from your voice note in frame 1. Feature: Srutam MCP + QR pairing.
import React from 'react';
import {InsightsScreen, PairDialog, Phone, ReminderRow, SettingsScreen, TaskRow, Toast} from '../components/App';
import {QR, Tap, typed} from '../components/ui';
import {E, L, P, cl, tf} from '../lib';
import {OtherApp} from '../scenes/Capture';
import {TermLines} from '../scenes/NewFeatures';
import {Cue, DockPill, Logo, Pop, Sc, Shell, Tag, Term, whooshes} from './kit';

const ASK: Array<[number, React.ReactNode, number?, string?]> = [
	[0.25, <span className="p">&gt;</span>, 0.9, 'what did I say about the sync bug?'],
	[1.3, <><span className="g">●</span> <span className="v">srutam</span> · <span className="b">search_notes</span><span className="m">(&quot;sync bug&quot;)</span></>],
	[1.55, <span className="m">{'  ⎿  1 voice note · today, 9:12 AM'}</span>],
	[1.8, <span className="w" />, 1.2, "You said: fix the sync retry bug before release. It's still an open task."],
];
const SHIP: Array<[number, React.ReactNode, number?, string?]> = [
	[0.2, <span className="p">&gt;</span>, 0.5, 'fix it, then mark it done'],
	[0.9, <><span className="g">●</span> <span className="w">Edit</span> <span className="m">cloud/CloudSyncWorker.kt</span> <span className="g">+12</span> <span className="p">−3</span></>],
	[1.3, <><span className="g">●</span> <span className="w">Bash</span> <span className="m">./gradlew test</span>  <span className="g">✓ passed</span></>],
	[1.8, <><span className="g">●</span> <span className="v">srutam</span> · <span className="b">update_action_item</span><span className="m">(done, agent:claude-code)</span></>],
	[2.15, <span className="g">{'  ⎿  ✓ Completed · synced to your phone'}</span>],
];
const INIT: Array<[number, React.ReactNode, number?, string?]> = [
	[0.2, <span className="p">$</span>, 0.6, 'npx -y srutam-mcp init'],
	[0.95, <span className="w" style={{fontWeight: 600}}>Srutam MCP - Developer Setup</span>],
	[1.15, 'QR'],
	[2.6, <>Your phone approved <span className="w">&quot;MacBook Pro&quot;</span>. <span className="g">✓ Setup complete!</span></>],
];
const CLIENTS = ['Claude Code', 'Cursor', 'Codex', 'Gemini CLI', 'VS Code', 'Windsurf', 'Zed', '+9 more'];

export const XLaunch: React.FC = () => {
	const cues: Cue[] = [
		['typing', 0.3, 0.14], ['pop', 1.35, 0.12], ['recstart', 4.9, 0.25],
		['typing', 8.2, 0.14], ['success', 10.4, 0.22], ['typing', 12.9, 0.12], ['scan', 14.0, 0.16],
		['success', 15.4, 0.2], ['impact', 16.6, 0.18], ...whooshes([4.1, 8.0, 12.4, 16.6], 0.3),
	];
	return (
		<Shell music="chill-house" musicVol={0.6} cues={cues} energy={(t) => (t < 16.5 ? 0.6 : 0.95)}>
			{(t) => (
				<>
					{/* A — hook: the agent answers from a voice note */}
					<Sc t={t} a={0} b={4.2}>
						{(lt) => (
							<>
								<div style={{position: 'absolute', left: 120, top: 250, width: 720}}>
									<Tag style={tf({o: P(lt, 0.05, 0.3)})}>Srutam 2.5 · MCP</Tag>
									<Pop t={lt} at={0.1} parts={[['Your coding agent'], ['can hear you now.', true]]} br={[1]} size={104} style={{marginTop: 22}} />
								</div>
								<Term title="claude — ~/srutam" font={30} style={{left: 880, top: 280, width: 940, height: 420, ...tf({x: (1 - E.out(P(lt, 0, 0.4))) * 60, o: P(lt, 0, 0.25)})}}>
									<TermLines t={lt} lines={ASK} />
								</Term>
							</>
						)}
					</Sc>
					{/* B — capture: say it anywhere */}
					<Sc t={t} a={4.1} b={8.1}>
						{(lt) => (
							<>
								<div style={{position: 'absolute', left: 120, top: 330, width: 820}}>
									<Pop t={lt} at={0.05} parts={[['Say it on a walk.'], ['One tap, any app.', true]]} br={[1]} size={100} />
									<div style={{fontFamily: 'Inter', fontSize: 30, color: '#94A3B8', marginTop: 26, ...tf({o: P(lt, 0.7, 1.1)})}}>The Floating Dock records from anywhere. Srutam turns it into tasks.</div>
								</div>
								<Phone style={{left: 1220, top: 110, ...tf({y: (1 - E.out(P(lt, 0, 0.5))) * 80, o: P(lt, 0, 0.3)})}}>
									<OtherApp />
									<div style={{position: 'absolute', left: 8, top: 300, transformOrigin: 'left center', ...tf({sx: cl(E.back(P(lt, 0.3, 0.6)))})}}>
										<DockPill t={lt} rec={lt > 0.8} sec={Math.max(0, Math.floor((lt - 0.8) * 1.6))} />
									</div>
									<Tap t={lt} at={0.65} x={30} y={326} />
								</Phone>
							</>
						)}
					</Sc>
					{/* C — the agent ships it, phone ticks */}
					<Sc t={t} a={8.0} b={12.5}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.05} parts={[['Your agent'], ['ships it.', true]]} size={92} style={{position: 'absolute', left: 120, top: 90}} />
								<Term title="claude — ~/srutam" font={28} style={{left: 120, top: 280, width: 1060, height: 400, ...tf({o: P(lt, 0, 0.25)})}}>
									<TermLines t={lt} lines={SHIP} />
								</Term>
								<Phone style={{left: 1260, top: 110, ...tf({s: 0.92, o: P(lt, 0.1, 0.4)})}}>
									<InsightsScreen sel="next">
										<TaskRow text="Fix the sync retry bug" date="Today, 9:12 AM" done={P(lt, 2.2, 2.45)} />
										<ReminderRow when="Fri, 4:00 PM" text="Demo with Priya" />
										<TaskRow text="Send the revised proposal" date="Yesterday, 6:40 PM" source="Standup recap" />
									</InsightsScreen>
								</Phone>
							</>
						)}
					</Sc>
					{/* D — one-scan pairing */}
					<Sc t={t} a={12.4} b={16.7}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.05} parts={[['Connect in'], ['one scan.', true]]} size={92} style={{position: 'absolute', left: 120, top: 90}} />
								<div style={{position: 'absolute', left: 124, top: 220, fontFamily: 'Inter', fontSize: 28, color: '#94A3B8', ...tf({o: P(lt, 0.5, 0.9)})}}>No API keys to paste. Your key never leaves your computer.</div>
								<Term title="~/code — zsh" font={28} style={{left: 120, top: 290, width: 1060, height: 520, ...tf({o: P(lt, 0, 0.25)})}}>
									<TermLines t={lt} lines={INIT} extra={(i) => i === 2 ? <div style={{width: 200, margin: '12px 0', ...tf({s: L(0.6, 1, cl(E.back(P(lt, 1.15, 1.5)))), o: P(lt, 1.15, 1.3)})}}><QR /></div> : undefined} />
								</Term>
								<Phone style={{left: 1260, top: 110, ...tf({s: 0.92, o: P(lt, 0.3, 0.6), y: (1 - E.out(P(lt, 0.3, 0.8))) * 120})}}>
									<SettingsScreen />
									{lt > 1.7 && lt < 2.65 && <PairDialog press={P(lt, 2.35, 2.45) * (1 - P(lt, 2.47, 2.6))} />}
									<Toast text={'Connected "MacBook Pro"'} o={P(lt, 2.65, 2.8)} />
								</Phone>
							</>
						)}
					</Sc>
					{/* E — end card */}
					<Sc t={t} a={16.6} b={20.5}>
						{(lt) => (
							<div style={{position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 22}}>
								<div style={{display: 'flex', alignItems: 'center', gap: 28, ...tf({s: L(1.1, 1, E.out(P(lt, 0, 0.6))), o: P(lt, 0, 0.3)})}}>
									<Logo size={150} />
									<div className="serif" style={{fontSize: 170, lineHeight: 1}}>Srutam <i className="grad" style={{paddingRight: 12}}>2.5</i></div>
								</div>
								<div style={{fontFamily: 'Inter', fontSize: 34, color: '#CBD5E1', ...tf({o: P(lt, 0.4, 0.8)})}}>Your voice notes, inside your coding agent.</div>
								<div style={{display: 'flex', gap: 12, flexWrap: 'wrap', justifyContent: 'center', width: 1300, ...tf({o: P(lt, 0.7, 1.1)})}}>
									{CLIENTS.map((c, i) => <span key={c} className="chip" style={{fontSize: 22, ...tf({s: L(0.8, 1, cl(E.back(P(lt, 0.7 + i * 0.06, 1.1 + i * 0.06))))})}}><span className="d" style={{background: ['#60A5FA', '#A78BFA', '#F472B6', '#34D399'][i % 4]}} />{c}</span>)}
								</div>
								<span className="chip mono" style={{fontSize: 30, padding: '18px 34px', color: '#C4B5FD', marginTop: 10, ...tf({o: P(lt, 1.1, 1.4)})}}><span className="p">$</span>&nbsp;{typed('npx -y srutam-mcp init', lt, 1.15, 1.9)}</span>
							</div>
						)}
					</Sc>
				</>
			)}
		</Shell>
	);
};

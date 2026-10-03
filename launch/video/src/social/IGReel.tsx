// Instagram Reel, 9:16, ~21s. Audience: students, creators, busy people with more ideas than time.
// Stop-scroll: a relatable POV in second one, value shown by second three. Features: Floating Dock, Insights, Cloud Sync.
import React from 'react';
import {DetailScreen, FeedScreen, IdeaCard, Phone, ReminderRow, TaskRow, DecisionCard, CloudCard} from '../components/App';
import {Tap} from '../components/ui';
import {E, L, P, cl, tf} from '../lib';
import {OtherApp} from '../scenes/Capture';
import {Cue, DockPill, Logo, PlayBadge, Pop, Sc, Shell} from './kit';

const T = "Okay idea: a weekly 'brain dump' video series. Film Sundays, post Monday 9 AM. Ask Maya to do the thumbnails. Decided: keep it under 60 seconds.";

export const IGReel: React.FC = () => {
	const cues: Cue[] = [
		['tap', 2.55, 0.3], ['recstart', 2.75, 0.3], ['whoosh', 5.85, 0.1], ['typing', 6.3, 0.12],
		['whoosh', 9.0, 0.1], ['pop', 9.7, 0.2], ['pop', 10.4, 0.2], ['pop', 11.1, 0.2], ['pop', 11.8, 0.2],
		['whoosh', 14.3, 0.1], ['success', 15.6, 0.22], ['impact', 17.6, 0.2],
	];
	const ph = (extra: React.CSSProperties = {}): React.CSSProperties => ({left: 540 - 205, top: 760, ...extra});
	return (
		<Shell music="future-pop" musicVol={0.7} cues={cues} energy={(t) => 0.7 + 0.2 * Math.sin(t)}>
			{(t) => (
				<>
					{/* A — POV hook while scrolling */}
					<Sc t={t} a={0} b={2.5} fade={0.15}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.05} serif={false} parts={[['POV:'], ['a genius idea'], ['hits mid-scroll', true]]} br={[1, 2]} size={108} style={{position: 'absolute', left: 70, right: 70, top: 200, textAlign: 'center'}} />
								<Phone style={ph(tf({s: 1.08}))}>
									<div style={{position: 'absolute', inset: 0, transform: `translateY(${-lt * 120}px)`}}><OtherApp /></div>
								</Phone>
							</>
						)}
					</Sc>
					{/* B — one tap on the Floating Dock */}
					<Sc t={t} a={2.4} b={6.0} fade={0.15}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.05} serif={false} parts={[['1 tap.'], ['Any app.', true]]} br={[1]} size={150} style={{position: 'absolute', left: 60, right: 60, top: 190, textAlign: 'center'}} />
								<div style={{position: 'absolute', left: 0, right: 0, top: 560, textAlign: 'center', fontFamily: 'Inter', fontSize: 36, color: '#CBD5E1', ...tf({o: P(lt, 0.8, 1.1)})}}>The Floating Dock lives on your screen edge</div>
								<Phone style={ph(tf({s: 1.08}))}>
									<OtherApp />
									<div style={{position: 'absolute', left: 8, top: 300, transformOrigin: 'left center', ...tf({sx: cl(E.back(P(lt, 0.05, 0.3)))})}}>
										<DockPill t={lt} rec={lt > 0.35} sec={Math.max(0, Math.floor((lt - 0.35) * 2.2))} />
									</div>
									<Tap t={lt} at={0.2} x={30} y={326} />
								</Phone>
								{lt > 0.35 && <div style={{position: 'absolute', left: 540 - 60, top: 1720, width: 120, height: 120, borderRadius: 60, border: '4px solid #EF4444', ...tf({s: 0.5 + ((lt * 1.2) % 1) * 1.6, o: 1 - ((lt * 1.2) % 1)})}} />}
							</>
						)}
					</Sc>
					{/* C — it writes it down */}
					<Sc t={t} a={5.9} b={9.1} fade={0.15}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.05} serif={false} parts={[['It writes it'], ['down for you', true]]} br={[1]} size={112} style={{position: 'absolute', left: 60, right: 60, top: 200, textAlign: 'center'}} />
								<Phone style={ph(tf({s: 1.08}))}>
									<DetailScreen title="Content ideas" tab="transcript" transcript={T.slice(0, Math.floor(T.length * P(lt, 0.3, 2.6)))} caret={Math.floor(lt * 4) % 2 === 0} />
								</Phone>
							</>
						)}
					</Sc>
					{/* D — and sorts the chaos */}
					<Sc t={t} a={9.0} b={14.4} fade={0.15}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.05} serif={false} parts={[['…and sorts'], ['the chaos.', true]]} br={[1]} size={124} style={{position: 'absolute', left: 60, right: 60, top: 190, textAlign: 'center'}} />
								{[
									<IdeaCard key="i" text="Weekly 'brain dump' video series" />,
									<TaskRow key="t" text="Ask Maya to do the thumbnails" date="Today, 8:40 PM" source="Voice note" />,
									<ReminderRow key="r" when="Mon, 9:00 AM" text="Post episode 1" />,
									<DecisionCard key="d" text="Keep every episode under 60s" why="Made for short attention spans" />,
								].map((c, i) => {
									const st = 0.65 + i * 0.7;
									const k = E.back(P(lt, st, st + 0.45));
									const y = [600, 880, 1080, 1250][i];
									return (
										<div key={i} style={{position: 'absolute', left: 90, top: y, width: 900 / 1.45, transformOrigin: 'top left', fontFamily: 'Roboto', ...tf({s: L(0.4, 1, cl(k)) * 1.45, o: P(lt, st, st + 0.15), r: (1 - cl(k)) * (i % 2 ? 6 : -6), y: (1 - cl(k)) * 80}), filter: 'drop-shadow(0 18px 40px rgba(0,0,0,.5))'}}>{c}</div>
									);
								})}
							</>
						)}
					</Sc>
					{/* E — synced everywhere */}
					<Sc t={t} a={14.3} b={17.6} fade={0.15}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.05} serif={false} parts={[['Synced.'], ['Everywhere.', true]]} br={[1]} size={140} style={{position: 'absolute', left: 60, right: 60, top: 190, textAlign: 'center'}} />
								<Phone style={{left: 70, top: 640, ...tf({s: 0.86, r: -4})}}>
									<FeedScreen sync={lt > 1.3 ? 'synced' : 'syncing'} spin={lt} />
								</Phone>
								<div style={{position: 'absolute', left: 470, top: 1080, width: 560 / 1.2, transformOrigin: 'top left', fontFamily: 'Roboto', ...tf({s: 1.2, o: P(lt, 0.4, 0.7), x: (1 - E.out(P(lt, 0.4, 0.9))) * 80}), filter: 'drop-shadow(0 20px 40px rgba(0,0,0,.5))'}}>
									<CloudCard showConnect={false} />
								</div>
							</>
						)}
					</Sc>
					{/* F — end card */}
					<Sc t={t} a={17.5} b={21.5} fade={0.2}>
						{(lt) => (
							<div style={{position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 30, textAlign: 'center'}}>
								<Logo size={240} style={tf({s: L(1.2, 1, E.back(P(lt, 0, 0.5))), o: P(lt, 0, 0.2)})} />
								<div className="serif" style={{fontSize: 170, lineHeight: 1, ...tf({o: P(lt, 0.2, 0.5)})}}>Srutam <i className="grad" style={{paddingRight: 12}}>2.5</i></div>
								<Pop t={lt} at={0.5} serif={false} weight={700} parts={[['Your ideas,'], ['handled.', true]]} size={64} />
								<div style={tf({o: P(lt, 1.0, 1.3), y: (1 - E.out(P(lt, 1.0, 1.4))) * 20})}><PlayBadge size={34} /></div>
								<div style={{fontFamily: 'Inter', fontSize: 30, letterSpacing: '.12em', color: '#94A3B8', ...tf({o: P(lt, 1.3, 1.6)})}}>SRUTAM.IAMJUSTKRISHNA.SPACE</div>
							</div>
						)}
					</Sc>
				</>
			)}
		</Shell>
	);
};

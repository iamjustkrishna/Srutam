// LinkedIn, 4:5 (1080x1350), 20s. Audience: founders, PMs, engineering leads, knowledge workers.
// Stop-scroll: a calm, true problem statement. Features: decisions & next steps, Cloud Sync, trust & privacy.
import React from 'react';
import {CloudCard, DecisionCard, FeedScreen, Phone, ReminderRow, TaskRow} from '../components/App';
import {E, L, P, cl, tf} from '../lib';
import {Cue, Logo, PlayBadge, Pop, Sc, Shell, Tag} from './kit';

const TRUST = ['Transcribed on your phone', 'Private notes stay private', 'Agents can’t edit or delete notes', 'Account deletion on request'];

export const LinkedIn: React.FC = () => {
	const cues: Cue[] = [
		['whoosh', 3.7, 0.07], ['pop', 5.0, 0.12], ['pop', 5.6, 0.12], ['pop', 6.2, 0.12], ['whoosh', 8.9, 0.07],
		['success', 10.6, 0.16], ['whoosh', 12.9, 0.07], ...[0, 1, 2, 3].map((i) => ['impact_soft', 13.6 + i * 0.6, 0.14] as Cue), ['impact', 17.1, 0.16],
	];
	return (
		<Shell music="ambient-glow" musicVol={0.6} cues={cues} energy={() => 0.5}>
			{(t) => (
				<>
					{/* A — the problem */}
					<Sc t={t} a={0} b={3.8}>
						{(lt) => (
							<div style={{position: 'absolute', left: 90, right: 90, top: 380}}>
								<Pop t={lt} at={0.1} stag={0.09} parts={[['The best ideas show up'], ['between meetings.', true]]} br={[1]} size={96} />
								<Pop t={lt} at={1.5} stag={0.06} serif={false} weight={500} parts={[['Most never make it to the task list.']]} size={40} style={{color: '#94A3B8', marginTop: 40}} />
							</div>
						)}
					</Sc>
					{/* B — voice note → decisions & next steps */}
					<Sc t={t} a={3.7} b={9.0}>
						{(lt) => (
							<>
								<Tag style={{position: 'absolute', left: 90, top: 110, ...tf({o: P(lt, 0, 0.3)})}}>Srutam 2.5</Tag>
								<Pop t={lt} at={0.1} parts={[['Talk for 40 seconds.'], ['Get decisions & next steps.', true]]} br={[1]} size={70} style={{position: 'absolute', left: 90, right: 90, top: 160}} />
								{[
									<DecisionCard key="d" text="Ship onboarding v2 before the Q4 freeze" why="Agreed in Monday's standup" />,
									<TaskRow key="t" text="Share the pricing draft with Finance" date="Today, 10:05 AM" source="Standup recap" />,
									<ReminderRow key="r" when="Thu, 3:00 PM" text="Design review" />,
								].map((c, i) => {
									const st = 1.25 + i * 0.6;
									const k = E.back(P(lt, st, st + 0.5));
									return (
										<div key={i} style={{position: 'absolute', left: 120, top: [470, 760, 960][i], width: 840 / 1.45, transformOrigin: 'top left', fontFamily: 'Roboto', ...tf({s: 1.45 * L(0.85, 1, cl(k)), o: P(lt, st, st + 0.2), y: (1 - cl(k)) * 40}), filter: 'drop-shadow(0 18px 40px rgba(0,0,0,.45))'}}>{c}</div>
									);
								})}
							</>
						)}
					</Sc>
					{/* C — synced across devices */}
					<Sc t={t} a={8.9} b={13.0}>
						{(lt) => (
							<>
								<Pop t={lt} at={0.1} parts={[['On your phone.'], ['On your laptop.', true]]} br={[1]} size={78} style={{position: 'absolute', left: 90, right: 90, top: 110}} />
								<div style={{position: 'absolute', left: 94, top: 300, fontFamily: 'Inter', fontSize: 30, color: '#94A3B8', ...tf({o: P(lt, 0.6, 1.0)})}}>New in 2.5: Cloud Sync keeps every note with you.</div>
								<Phone style={{left: 60, top: 330, ...tf({s: 0.78, o: P(lt, 0.2, 0.5)})}}>
									<FeedScreen sync={lt > 1.7 ? 'synced' : 'syncing'} spin={lt} />
								</Phone>
								<div style={{position: 'absolute', left: 480, top: 660, width: 540 / 1.15, transformOrigin: 'top left', fontFamily: 'Roboto', ...tf({s: 1.15, o: P(lt, 0.6, 0.9), x: (1 - E.out(P(lt, 0.6, 1.1))) * 60}), filter: 'drop-shadow(0 20px 40px rgba(0,0,0,.45))'}}>
									<CloudCard showConnect={false} />
								</div>
							</>
						)}
					</Sc>
					{/* D — trust */}
					<Sc t={t} a={12.9} b={17.2}>
						{(lt) => (
							<div style={{position: 'absolute', left: 90, right: 90, top: 300}}>
								<Tag style={tf({o: P(lt, 0, 0.3)})}>Built to be trusted</Tag>
								{TRUST.map((s, i) => {
									const st = 0.5 + i * 0.6;
									const k = E.expo(P(lt, st, st + 0.35));
									return (
										<div key={s} className="trow serif" style={{justifyContent: 'flex-start', fontSize: 62, marginTop: 34, ...tf({x: (1 - k) * -50, o: k, b: (1 - k) * 12})}}>
											<span className="tick" />{s}
										</div>
									);
								})}
							</div>
						)}
					</Sc>
					{/* E — end card */}
					<Sc t={t} a={17.1} b={20.6}>
						{(lt) => (
							<div style={{position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 28, textAlign: 'center'}}>
								<Logo size={200} style={tf({s: L(1.12, 1, E.out(P(lt, 0, 0.6))), o: P(lt, 0, 0.3)})} />
								<div className="serif" style={{fontSize: 150, lineHeight: 1, ...tf({o: P(lt, 0.2, 0.5)})}}>Srutam <i className="grad" style={{paddingRight: 12}}>2.5</i></div>
								<Pop t={lt} at={0.5} parts={[['Capture. Organize.'], ['Act.', true]]} size={64} />
								<div style={tf({o: P(lt, 1.0, 1.3)})}><PlayBadge size={30} /></div>
								<div style={{fontFamily: 'Inter', fontSize: 28, letterSpacing: '.12em', color: '#94A3B8', ...tf({o: P(lt, 1.2, 1.5)})}}>SRUTAM.IAMJUSTKRISHNA.SPACE</div>
							</div>
						)}
					</Sc>
				</>
			)}
		</Shell>
	);
};

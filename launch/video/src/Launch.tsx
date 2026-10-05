import React from 'react';
import {Audio} from '@remotion/media';
import {AbsoluteFill, Sequence, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import './v1.css';
import {Bg} from './components/ui';
import {E, FPS, LayoutCtx, P, tf} from './lib';
import {Dock, Organize, Transcribe} from './scenes/Capture';
import {Agent, Cloud, Connect} from './scenes/NewFeatures';
import {End, Hook, NewIn, Title, Trust} from './scenes/Simple';
import {type Manifest, type SceneId, VO_DELAY, layoutTimeline, toCaption} from './timeline';

const SHOW_CAPTIONS = false;

export type LaunchProps = {manifest: Manifest | null};

const COMP: Record<SceneId, React.FC<{t: number; dur: number}>> = {
	hook: Hook, title: Title, dock: Dock, transcribe: Transcribe, organize: Organize, newin: NewIn,
	cloud: Cloud, connect: Connect, agent: Agent, trust: Trust, end: End,
};
const ENERGY: Record<SceneId, number> = {hook: 0.3, title: 0.6, dock: 0.5, transcribe: 0.5, organize: 0.6, newin: 1, cloud: 0.75, connect: 0.75, agent: 0.8, trust: 0.5, end: 0.85};

const SceneWrap: React.FC<{id: SceneId; dur: number}> = ({id, dur}) => {
	const frame = useCurrentFrame();
	const t = frame / FPS;
	const ki = E.out(P(t, 0, 0.35)), ko = E.in(P(t, dur - 0.4, dur));
	const C = COMP[id];
	return (
		<AbsoluteFill style={tf({s: (0.97 + 0.03 * ki) * (1 + 0.04 * ko), o: id === 'hook' ? 1 - ko : ki * (1 - ko), b: (1 - ki) * 10 + ko * 12})}>
			<C t={t} dur={dur} />
		</AbsoluteFill>
	);
};

// sound-effect cues, relative to scene starts (only played if the file exists in the manifest)
const cues = (tl: ReturnType<typeof layoutTimeline>) => {
	const at = (id: SceneId) => tl.find((s) => s.id === id)!.start;
	const c: Array<[string, number, number]> = []; // [sfx, time, volume]
	// soft whoosh per cut; its peak (0.16s into the file) lands mid fade-in (scenes fade in over 0.35s)
	tl.forEach((s, i) => i > 0 && s.id !== 'newin' && c.push(['whoosh_soft', s.start + 0.175 - 0.16, 0.12]));
	c.push(['impact', at('title') + 0.45, 0.2], ['tap', at('dock') + 1.15, 0.28], ['tap', at('dock') + 2.3, 0.28], ['recstart', at('dock') + 2.5, 0.28],
		['tap', at('dock') + 5.8, 0.28], ['success', at('dock') + 6.25, 0.2], ['typing', at('transcribe') + 0.5, 0.1],
		...[0, 1, 2, 3, 4].map((i) => ['pop', at('organize') + 1.05 + i * 0.45, 0.16] as [string, number, number]),
		['riser', at('newin') - 1.1, 0.22], ['braam', at('newin') + 0.02, 0.24], ['success', at('cloud') + 3.1, 0.2],
		['typing', at('connect') + 0.55, 0.14], ['scan', at('connect') + 2.6, 0.18], ['tap', at('connect') + 4.05, 0.28], ['success', at('connect') + 4.3, 0.22],
		['typing', at('agent') + 0.45, 0.14], ['typing', at('agent') + 3.65, 0.14], ['success', at('agent') + 5.65, 0.24], ['pop', at('agent') + 6.3, 0.16],
		...[0, 1, 2, 3].map((i) => ['impact_soft', at('trust') + 0.35 + i * 0.75, 0.18] as [string, number, number]),
		['impact', at('end') + 0.15, 0.2]);
	return c;
};

const Captions: React.FC<{tl: ReturnType<typeof layoutTimeline>; m: Manifest | null; t: number; portrait: boolean}> = ({tl, m, t, portrait}) => {
	const s = tl.find((x) => x.vo && t >= x.start + VO_DELAY - 0.1 && t < x.start + x.dur - 0.2);
	if (!s) return null;
	const local = t - s.start - VO_DELAY;
	const raw = s.vo.split(' ');
	const vo = m?.scenes[s.id];
	const words = vo?.words?.length ? vo.words : raw.map((w, i) => ({w, s: (i / raw.length) * (raw.length / 2.7), e: ((i + 1) / raw.length) * (raw.length / 2.7)}));
	// chunk into lines of up to N words, breaking after punctuation
	const N = portrait ? 4 : 7;
	const chunks: (typeof words)[] = [];
	let cur: typeof words = [];
	words.forEach((w) => {
		cur.push(w);
		if (cur.length >= N || /[.,:!?]$/.test(w.w)) {
			chunks.push(cur);
			cur = [];
		}
	});
	if (cur.length) chunks.push(cur);
	const ci = Math.max(0, chunks.findIndex((c) => local < c[c.length - 1].e + 0.15));
	const chunk = chunks[ci === -1 ? chunks.length - 1 : ci];
	const a = P(local, chunk[0].s - 0.15, chunk[0].s);
	return (
		<div style={{position: 'absolute', left: 0, right: 0, bottom: portrait ? 150 : 34, display: 'flex', justifyContent: 'center', zIndex: 50, opacity: Math.max(0.0, a)}}>
			<div style={{padding: portrait ? '16px 30px' : '12px 28px', borderRadius: 22, background: 'rgba(5,7,20,.62)', border: '1px solid rgba(148,163,184,.18)', fontFamily: 'Inter', fontWeight: 700, fontSize: portrait ? 54 : 40, letterSpacing: '-.01em', color: '#F1F5F9', maxWidth: portrait ? 940 : 1500, textAlign: 'center', lineHeight: 1.2}}>
				{chunk.map((w, i) => {
					const on = local >= w.s - 0.02;
					const txt = toCaption(w.w);
					if (!txt) return null;
					return <span key={i} style={{color: on ? '#F8FAFC' : 'rgba(241,245,249,.38)', textShadow: on && local < w.e + 0.1 ? '0 0 18px rgba(96,165,250,.8)' : undefined}}>{txt} </span>;
				})}
			</div>
		</div>
	);
};

export const Launch: React.FC<LaunchProps> = ({manifest}) => {
	const frame = useCurrentFrame();
	const {width, height, durationInFrames} = useVideoConfig();
	const portrait = height > width;
	const t = frame / FPS;
	const tl = layoutTimeline(manifest ?? undefined);
	const cur = [...tl].reverse().find((s) => t >= s.start) ?? tl[0];
	const total = durationInFrames / FPS;
	const voWin = (tt: number) => tl.some((s) => manifest?.scenes[s.id] && tt >= s.start + VO_DELAY - 0.15 && tt < s.start + VO_DELAY + manifest.scenes[s.id].dur + 0.2);
	return (
		<LayoutCtx.Provider value={{portrait, w: width, h: height}}>
				<AbsoluteFill style={{background: '#050714', fontFamily: 'Inter', color: '#F8FAFC'}}>
					<Bg t={t} energy={ENERGY[cur.id]} w={width} h={height} />
					{tl.map((s) => (
						<Sequence key={s.id} name={s.id} from={Math.round(s.start * FPS)} durationInFrames={Math.round(s.dur * FPS)} premountFor={FPS}>
							<SceneWrap id={s.id} dur={s.dur} />
						</Sequence>
					))}
					{SHOW_CAPTIONS && <Captions tl={tl} m={manifest} t={t} portrait={portrait} />}
					<AbsoluteFill style={{background: '#000', opacity: Math.max(1 - t / 0.3, P(t, total - 0.6, total))}} />
					{/* ---- audio ---- */}
					{manifest?.music && (
						<Audio src={staticFile(manifest.music)} volume={(f) => {
							const tt = f / FPS;
							const fade = Math.min(1, tt / 0.8) * Math.min(1, (total - tt) / 2);
							return (voWin(tt) ? 0.16 : 0.32) * fade;
						}} />
					)}
					{tl.map((s) => manifest?.scenes[s.id] && (
						<Sequence key={'vo' + s.id} from={Math.round((s.start + VO_DELAY) * FPS)} premountFor={FPS}>
							<Audio src={staticFile(manifest.scenes[s.id].file)} volume={1} />
						</Sequence>
					))}
					{manifest?.sfx && cues(tl).filter(([n]) => manifest.sfx![n]).map(([n, at, v], i) => (
						<Sequence key={'sfx' + i} from={Math.max(0, Math.round(at * FPS))} premountFor={FPS}>
							<Audio src={staticFile(manifest.sfx![n])} volume={v} />
						</Sequence>
					))}
				</AbsoluteFill>
		</LayoutCtx.Provider>
	);
};

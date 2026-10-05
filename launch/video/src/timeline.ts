// Scene list, narration and timing come from plan.json (edit it, or use the editor).
// Narration audio + word timings come from public/vo/manifest.json (written by `srutam_video.py voice`).
export type Word = {w: string; s: number; e: number};
export type VoScene = {file: string; dur: number; words: Word[]; env: number[]};
export type Manifest = {voice?: string; scenes: Record<string, VoScene>; sfx?: Record<string, string>; music?: string};

import plan from '../plan.json' with {type: 'json'};

export type Plan = typeof plan;
export const PLAN = plan;
export const SCENES: ReadonlyArray<{id: string; min: number; vo: string}> = plan.scenes;
export type SceneId = string;

export const VO_DELAY = 0.35; // narration starts this long after a scene starts
export const OVERLAP = 0.4; // scenes cross-dissolve by this much

// Caption display text (narration spells "two point five" so TTS reads it right)
export const toCaption = (w: string) => (/^two$/i.test(w) ? '2.5' : /^(point|five[.:,]?)$/i.test(w) ? '' : w);

export const sceneDur = (id: string, min: number, m?: Manifest) => {
	const vo = m?.scenes?.[id];
	const est = (SCENES.find((s) => s.id === id)?.vo ?? '').split(' ').filter(Boolean).length / 2.7;
	const voDur = vo ? vo.dur : est;
	return Math.max(min, voDur ? voDur + VO_DELAY + 0.7 : 0);
};

export const layoutTimeline = (m?: Manifest) => {
	let t = 0;
	return SCENES.map((s, i) => {
		const dur = sceneDur(s.id, s.min, m);
		const start = t;
		t += dur - (i < SCENES.length - 1 ? OVERLAP : 0);
		return {id: s.id as SceneId, start, dur, vo: s.vo};
	});
};
export const totalDur = (m?: Manifest) => {
	const tl = layoutTimeline(m);
	const last = tl[tl.length - 1];
	return last.start + last.dur;
};

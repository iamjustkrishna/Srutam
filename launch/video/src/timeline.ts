// Scene list, narration script and timing. Narration audio + word timings come from
// public/vo/manifest.json (written by gen/voice.mjs); without it, durations are estimated.
export type Word = {w: string; s: number; e: number};
export type VoScene = {file: string; dur: number; words: Word[]; env: number[]};
export type Manifest = {voice?: string; scenes: Record<string, VoScene>; sfx?: Record<string, string>; music?: string};

export const SCENES = [
	{id: 'hook', min: 3.6, vo: 'Your best ideas never wait for a good moment.'},
	{id: 'title', min: 3.2, vo: 'Meet Srutam two point five.'},
	{id: 'dock', min: 7.4, vo: "A floating dock lives on the edge of your screen. One tap, and you're recording, from any app."},
	{id: 'transcribe', min: 4.8, vo: 'Your words are transcribed right on your phone.'},
	{id: 'organize', min: 6.2, vo: 'Then Srutam sorts the ramble into ideas, next steps, decisions and reminders.'},
	{id: 'newin', min: 2.0, vo: ''},
	{id: 'cloud', min: 5.0, vo: 'New in two point five: Cloud Sync keeps every note with you.'},
	{id: 'connect', min: 6.6, vo: 'And with Srutam MCP, your voice notes live inside your AI coding agent. Scan once. No keys to paste.'},
	{id: 'agent', min: 7.6, vo: "Ask what's on your plate. Your agent fixes it, and checks it off on your phone."},
	{id: 'trust', min: 4.6, vo: "Private notes stay private. And agents can't delete a thing."},
	{id: 'end', min: 6.4, vo: 'Srutam two point five. Say it once. Use it everywhere. Get it on Google Play.'},
] as const;
export type SceneId = (typeof SCENES)[number]['id'];

export const VO_DELAY = 0.35; // narration starts this long after a scene starts
export const OVERLAP = 0.4; // scenes cross-dissolve by this much

// Caption display text (narration spells "two point five" so TTS reads it right)
export const toCaption = (w: string) => (/^two$/i.test(w) ? '2.5' : /^(point|five[.:,]?)$/i.test(w) ? '' : w);

export const sceneDur = (id: string, min: number, m?: Manifest) => {
	const vo = m?.scenes?.[id];
	const est = SCENES.find((s) => s.id === id)!.vo.split(' ').filter(Boolean).length / 2.7;
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

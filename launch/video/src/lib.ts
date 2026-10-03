import type React from 'react';
import {createContext, useContext} from 'react';

export const FPS = 30;
export const C = {
	bg: '#050714', purple: '#7C3AED', blue: '#3B82F6', pink: '#EC4899', green: '#10B981',
	gold: '#FBBF24', crimson: '#EF4444', cobalt: '#2563EB', ink: '#F8FAFC', muted: '#94A3B8',
	amber: '#F59E0B', dock: '#1A1C1E',
};

export const cl = (x: number) => (x < 0 ? 0 : x > 1 ? 1 : x);
export const P = (t: number, a: number, b: number) => cl((t - a) / (b - a));
export const L = (a: number, b: number, x: number) => a + (b - a) * x;
export const E = {
	out: (x: number) => 1 - Math.pow(1 - x, 3),
	in: (x: number) => x * x * x,
	io: (x: number) => (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2),
	back: (x: number) => {
		const c1 = 1.6, c3 = c1 + 1;
		return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
	},
	expo: (x: number) => (x >= 1 ? 1 : 1 - Math.pow(2, -10 * x)),
	// damped spring 0 -> 1 with overshoot
	spring: (x: number) => (x <= 0 ? 0 : x >= 1 ? 1 : 1 - Math.exp(-6 * x) * Math.cos(10 * x)),
};

type TF = {x?: number; y?: number; s?: number; sx?: number; sy?: number; r?: number; o?: number; b?: number};
export const tf = ({x = 0, y = 0, s = 1, sx = 1, sy = 1, r = 0, o = 1, b = 0}: TF): React.CSSProperties => ({
	transform: `translate(${x}px,${y}px) rotate(${r}deg) scale(${s * sx},${s * sy})`,
	opacity: o,
	filter: b > 0.05 ? `blur(${b}px)` : undefined,
});

export const rng = (seed: number) => {
	let s = seed >>> 0;
	return () => {
		s = (s * 1664525 + 1013904223) >>> 0;
		return s / 4294967296;
	};
};

// Layout context: portrait (9:16) vs landscape (16:9)
export const useLayout = () => {
	const v = useContext(LayoutCtx);
	return v;
};
export const LayoutCtx = createContext({portrait: false, w: 1920, h: 1080});

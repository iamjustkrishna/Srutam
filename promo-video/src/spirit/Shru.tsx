import { useId } from "react";

export type ShruPose = "sit" | "leap" | "curl";
export type ShruEyes = "open" | "closed" | "fierce";

const GLOW_LINE = "#E0F2FE";
const GOLD = "#FBBF24";

type Pt = [number, number];

/**
 * A tapered ribbon along a quadratic curve, with a sine "waveform" running
 * down its spine: Shru's tails are sound waves.
 */
const Tail: React.FC<{
  from: Pt;
  ctrl: Pt;
  to: Pt;
  w: number;
  phase?: number;
  grad: string;
}> = ({ from, ctrl, to, w, phase = 0, grad }) => {
  const N = 40;
  const pts: { x: number; y: number; nx: number; ny: number; t: number }[] = [];
  for (let i = 0; i <= N; i++) {
    const t = i / N;
    const x =
      (1 - t) ** 2 * from[0] + 2 * (1 - t) * t * ctrl[0] + t ** 2 * to[0];
    const y =
      (1 - t) ** 2 * from[1] + 2 * (1 - t) * t * ctrl[1] + t ** 2 * to[1];
    const dx = 2 * (1 - t) * (ctrl[0] - from[0]) + 2 * t * (to[0] - ctrl[0]);
    const dy = 2 * (1 - t) * (ctrl[1] - from[1]) + 2 * t * (to[1] - ctrl[1]);
    const len = Math.hypot(dx, dy) || 1;
    pts.push({ x, y, nx: -dy / len, ny: dx / len, t });
  }
  const half = (t: number) =>
    w *
    (0.55 + 0.45 * Math.sin(Math.PI * Math.min(1, t * 1.15))) *
    (1 - 0.65 * t);
  const left = pts.map(
    (p) => `${p.x + p.nx * half(p.t)},${p.y + p.ny * half(p.t)}`,
  );
  const right = pts
    .map((p) => `${p.x - p.nx * half(p.t)},${p.y - p.ny * half(p.t)}`)
    .reverse();
  const wave = pts
    .map((p, i) => {
      const a = Math.sin(p.t * Math.PI * 7 + phase) * half(p.t) * 0.55;
      return `${i ? "L" : "M"}${p.x + p.nx * a},${p.y + p.ny * a}`;
    })
    .join(" ");
  const tip = pts[N];
  return (
    <g>
      <polygon
        points={[...left, ...right].join(" ")}
        fill={grad}
        stroke={GLOW_LINE}
        strokeWidth={3}
        strokeLinejoin="round"
      />
      <path
        d={wave}
        stroke="#BAE6FD"
        strokeWidth={3.5}
        fill="none"
        opacity={0.9}
      />
      <circle cx={tip.x} cy={tip.y} r={w * 0.42} fill={GOLD} />
      <circle cx={tip.x} cy={tip.y} r={w * 0.9} fill={GOLD} opacity={0.25} />
    </g>
  );
};

/** Shru's head, origin at the head center, facing left. */
export const Head: React.FC<{ eyes: ShruEyes; body: string; id: string }> = ({
  eyes,
  body,
  id,
}) => (
  <g>
    <path
      d="M23 -42 L58 -116 L70 -28 Z"
      fill={body}
      stroke={GLOW_LINE}
      strokeWidth={3}
      strokeLinejoin="round"
    />
    <path d="M32 -40 L56 -94 L62 -34 Z" fill={GOLD} opacity={0.9} />
    <path
      d="M-36 -44 L-30 -112 L2 -52 Z"
      fill={body}
      stroke={GLOW_LINE}
      strokeWidth={3}
      strokeLinejoin="round"
    />
    <path d="M-30 -48 L-27 -94 L-6 -52 Z" fill={GOLD} opacity={0.75} />
    <path
      d="M28 18 L70 26 L50 36 L74 50 L44 52 L30 46 Z"
      fill="#93C5FD"
      stroke={GLOW_LINE}
      strokeWidth={3}
      strokeLinejoin="round"
    />
    <path
      d="M-44 -18 C-44 -70 50 -72 58 -12 C62 22 42 50 8 52 C-12 52 -30 46 -44 40 L-70 28 Q-80 20 -70 12 L-46 0 Z"
      fill={body}
      stroke={GLOW_LINE}
      strokeWidth={3.5}
      strokeLinejoin="round"
    />
    {/* Muzzle and cheek: lighter, like a fox's white */}
    <path
      d="M-46 0 L-70 12 Q-80 20 -70 28 L-44 40 C-30 46 -12 48 4 44 C-8 32 -24 22 -46 0 Z"
      fill="#DBEAFE"
      opacity={0.85}
    />
    {/* Forehead mark: three waveform bars */}
    <g fill={GOLD}>
      <rect x={-6} y={-46} width={6} height={10} rx={3} />
      <rect x={4} y={-50} width={6} height={18} rx={3} />
      <rect x={14} y={-46} width={6} height={10} rx={3} />
    </g>
    {eyes === "closed" ? (
      <g stroke="#0B1B4D" strokeWidth={4.5} fill="none" strokeLinecap="round">
        <path d="M-18 -4 Q-6 6 8 -4" />
        <path d="M-50 -2 Q-44 4 -38 -2" />
      </g>
    ) : (
      <g>
        <ellipse
          cx={-2}
          cy={-8}
          rx={17}
          ry={eyes === "fierce" ? 13 : 22}
          fill={`url(#eye${id})`}
          stroke="#0B1B4D"
          strokeWidth={3}
        />
        <ellipse
          cx={-5}
          cy={-6}
          rx={6}
          ry={eyes === "fierce" ? 9 : 14}
          fill="#0B1B4D"
        />
        <circle cx={-9} cy={-18} r={6} fill="#FFFFFF" />
        <circle cx={1} cy={4} r={2.2} fill="#FFFFFF" />
        <ellipse
          cx={-38}
          cy={-6}
          rx={8}
          ry={eyes === "fierce" ? 10 : 16}
          fill={`url(#eye${id})`}
          stroke="#0B1B4D"
          strokeWidth={2.5}
        />
        <ellipse
          cx={-40}
          cy={-5}
          rx={3.5}
          ry={eyes === "fierce" ? 6 : 10}
          fill="#0B1B4D"
        />
        {eyes === "fierce" && (
          <g stroke="#0B1B4D" strokeWidth={4} strokeLinecap="round">
            <path d="M-22 -26 L12 -18" />
            <path d="M-52 -20 L-36 -24" />
          </g>
        )}
      </g>
    )}
    <ellipse cx={-74} cy={18} rx={6.5} ry={5} fill="#0B1B4D" />
    <path
      d="M-66 31 Q-59 37 -52 31"
      stroke="#0B1B4D"
      strokeWidth={3}
      fill="none"
      strokeLinecap="round"
    />
  </g>
);

/**
 * Shru: the listening spirit that lives in Srutam's floating dock.
 * A fox made of light, cobalt to cyan, with gold inner ears and
 * waveform tails. Local box ~ 600 x 420.
 */
export const Shru: React.FC<{
  x: number;
  y: number;
  s: number;
  pose?: ShruPose;
  eyes?: ShruEyes;
  phase?: number;
  flip?: boolean;
}> = ({ x, y, s, pose = "sit", eyes = "open", phase = 0, flip = false }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const body = `url(#body${id})`;
  const tailFill = `url(#tail${id})`;
  return (
    <g transform={`translate(${x} ${y}) scale(${flip ? -s : s} ${s})`}>
      <defs>
        <linearGradient id={`body${id}`} x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0%" stopColor="#7DD3FC" />
          <stop offset="55%" stopColor="#3B82F6" />
          <stop offset="100%" stopColor="#1E3A8A" />
        </linearGradient>
        <linearGradient id={`tail${id}`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#2563EB" />
          <stop offset="100%" stopColor="#67E8F9" />
        </linearGradient>
        <radialGradient id={`eye${id}`} cx="50%" cy="65%">
          <stop offset="0%" stopColor="#FEF3C7" />
          <stop offset="60%" stopColor="#FBBF24" />
          <stop offset="100%" stopColor="#B45309" />
        </radialGradient>
        <radialGradient id={`aura${id}`}>
          <stop offset="0%" stopColor="#67E8F9" stopOpacity={0.45} />
          <stop offset="100%" stopColor="#67E8F9" stopOpacity={0} />
        </radialGradient>
        <filter id={`glow${id}`} x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation={8} result="b" />
          <feMerge>
            <feMergeNode in="b" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      {pose === "sit" && (
        <g filter={`url(#glow${id})`}>
          <ellipse
            cx={230}
            cy={230}
            rx={260}
            ry={200}
            fill={`url(#aura${id})`}
          />
          <Tail
            from={[270, 320]}
            ctrl={[400, 330]}
            to={[420, 170]}
            w={30}
            phase={phase}
            grad={tailFill}
          />
          <Tail
            from={[275, 315]}
            ctrl={[380, 250]}
            to={[360, 110]}
            w={26}
            phase={phase + 1.5}
            grad={tailFill}
          />
          <Tail
            from={[268, 325]}
            ctrl={[420, 400]}
            to={[480, 290]}
            w={24}
            phase={phase + 3}
            grad={tailFill}
          />
          <path
            d="M168 228 C140 270 134 330 150 348 L264 350 C292 320 294 262 262 212 C248 190 222 180 200 184 Z"
            fill={body}
            stroke={GLOW_LINE}
            strokeWidth={3.5}
          />
          <path
            d="M168 228 C160 252 162 280 176 300 C190 280 196 250 200 230 Z"
            fill="#DBEAFE"
            opacity={0.85}
          />
          <path
            d="M168 300 L164 350 M200 300 L198 352"
            stroke={GLOW_LINE}
            strokeWidth={3}
          />
          <ellipse
            cx={160}
            cy={352}
            rx={17}
            ry={8}
            fill="#BFDBFE"
            stroke={GLOW_LINE}
            strokeWidth={3}
          />
          <ellipse
            cx={196}
            cy={354}
            rx={17}
            ry={8}
            fill="#BFDBFE"
            stroke={GLOW_LINE}
            strokeWidth={3}
          />
          <g transform="translate(196 146) scale(1.18)">
            <Head eyes={eyes} body={body} id={id} />
          </g>
        </g>
      )}

      {pose === "leap" && (
        <g filter={`url(#glow${id})`}>
          <ellipse
            cx={260}
            cy={190}
            rx={320}
            ry={170}
            fill={`url(#aura${id})`}
          />
          <Tail
            from={[320, 182]}
            ctrl={[440, 120]}
            to={[580, 150]}
            w={30}
            phase={phase}
            grad={tailFill}
          />
          <Tail
            from={[318, 192]}
            ctrl={[450, 220]}
            to={[600, 220]}
            w={26}
            phase={phase + 1.5}
            grad={tailFill}
          />
          <Tail
            from={[316, 200]}
            ctrl={[420, 290]}
            to={[560, 300]}
            w={22}
            phase={phase + 3}
            grad={tailFill}
          />
          {/* Back legs, extended */}
          <path
            d="M296 214 L380 262 L372 274 L286 230 Z"
            fill="#2563EB"
            stroke={GLOW_LINE}
            strokeWidth={3}
            strokeLinejoin="round"
          />
          <path
            d="M150 168 C200 136 280 138 324 170 C340 186 330 216 306 224 C256 238 196 234 156 222 C136 210 136 182 150 168 Z"
            fill={body}
            stroke={GLOW_LINE}
            strokeWidth={3.5}
          />
          <path
            d="M156 222 C190 234 230 236 262 232 C232 220 196 206 168 186 Z"
            fill="#DBEAFE"
            opacity={0.7}
          />
          {/* Front legs reaching */}
          <path
            d="M168 206 L80 238 L84 252 L176 224 Z"
            fill="#60A5FA"
            stroke={GLOW_LINE}
            strokeWidth={3}
            strokeLinejoin="round"
          />
          <path
            d="M184 214 L104 264 L112 276 L196 228 Z"
            fill="#3B82F6"
            stroke={GLOW_LINE}
            strokeWidth={3}
            strokeLinejoin="round"
          />
          <ellipse
            cx={80}
            cy={246}
            rx={14}
            ry={9}
            fill="#BFDBFE"
            stroke={GLOW_LINE}
            strokeWidth={3}
          />
          <ellipse
            cx={106}
            cy={272}
            rx={14}
            ry={9}
            fill="#BFDBFE"
            stroke={GLOW_LINE}
            strokeWidth={3}
          />
          <g transform="translate(146 146) rotate(-12) scale(1.12)">
            <Head eyes={eyes} body={body} id={id} />
          </g>
        </g>
      )}

      {pose === "curl" && (
        <g filter={`url(#glow${id})`}>
          <ellipse
            cx={230}
            cy={260}
            rx={230}
            ry={150}
            fill={`url(#aura${id})`}
          />
          <ellipse
            cx={240}
            cy={270}
            rx={110}
            ry={74}
            fill={body}
            stroke={GLOW_LINE}
            strokeWidth={3.5}
          />
          <Tail
            from={[330, 290]}
            ctrl={[300, 380]}
            to={[150, 340]}
            w={30}
            phase={phase}
            grad={tailFill}
          />
          <Tail
            from={[338, 270]}
            ctrl={[380, 360]}
            to={[230, 360]}
            w={24}
            phase={phase + 2}
            grad={tailFill}
          />
          <g transform="translate(176 262) rotate(8) scale(0.92)">
            <Head eyes="closed" body={body} id={id} />
          </g>
          <text
            x={300}
            y={170}
            fontFamily="Inter"
            fontWeight={800}
            fontSize={34}
            fill="#BAE6FD"
          >
            z z
          </text>
        </g>
      )}
    </g>
  );
};

/** A thought: a golden firefly. `life` 1 = bright, 0 = grey ember turning to ash. */
export const Firefly: React.FC<{
  x: number;
  y: number;
  r: number;
  life?: number;
}> = ({ x, y, r, life = 1 }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const core = life > 0.5 ? "#FFF7D6" : "#E2E8F0";
  const mid = life > 0.5 ? "#FBBF24" : "#94A3B8";
  return (
    <g transform={`translate(${x} ${y})`}>
      <defs>
        <radialGradient id={`ff${id}`}>
          <stop offset="0%" stopColor={core} />
          <stop offset="35%" stopColor={mid} stopOpacity={0.95} />
          <stop offset="100%" stopColor={mid} stopOpacity={0} />
        </radialGradient>
      </defs>
      <circle
        r={r * (1.5 + 2.5 * life)}
        fill={`url(#ff${id})`}
        opacity={0.35 + 0.65 * life}
      />
      <ellipse
        cx={-r * 0.9}
        cy={-r * 0.6}
        rx={r * 0.9}
        ry={r * 0.45}
        fill="#FFFFFF"
        opacity={0.35 * life + 0.1}
        transform={`rotate(-30 ${-r * 0.9} ${-r * 0.6})`}
      />
      <ellipse
        cx={r * 0.9}
        cy={-r * 0.6}
        rx={r * 0.9}
        ry={r * 0.45}
        fill="#FFFFFF"
        opacity={0.35 * life + 0.1}
        transform={`rotate(30 ${r * 0.9} ${-r * 0.6})`}
      />
      <circle r={r * 0.55} fill={core} />
      {life < 0.5 &&
        Array.from({ length: 10 }, (_, i) => (
          <rect
            key={i}
            x={-r + ((i * 37) % 23) * (r / 11)}
            y={r * 0.6 + ((i * 53) % 17) * (r / 5)}
            width={r * 0.22}
            height={r * 0.22}
            fill={i % 3 ? "#64748B" : "#F97316"}
            opacity={0.9 - i * 0.07}
          />
        ))}
    </g>
  );
};

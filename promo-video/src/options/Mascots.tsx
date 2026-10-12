// Five alternative heroes, each in a 600 x 560 local box, catching a thought.
import { Eye, Glowfly, quad, ribbon, useGlow, type Pt } from "./shapes";

const LINE = "#F5F3FF";
const GOLD = "#FBBF24";

/* ----------------------------- GAJA: baby elephant ----------------------------- */
export const Gaja: React.FC = () => {
  const g = useGlow("#A5B4FC");
  const trunk = ribbon(
    quad([168, 300], [70, 330], [96, 168], 30),
    (t) => 30 - 16 * t,
  );
  return (
    <g>
      <defs>
        {g.defs}
        <linearGradient id={`gb${g.id}`} x1="0" y1="0" x2="0.4" y2="1">
          <stop offset="0%" stopColor="#E0E7FF" />
          <stop offset="55%" stopColor="#A5B4FC" />
          <stop offset="100%" stopColor="#6366F1" />
        </linearGradient>
      </defs>
      <ellipse cx={300} cy={330} rx={290} ry={230} fill={g.aura} />
      <g filter={g.filter}>
        {/* Back legs, tail */}
        <path
          d="M440 380 C470 380 488 360 492 330"
          stroke="#818CF8"
          strokeWidth={6}
          fill="none"
          strokeLinecap="round"
        />
        <circle cx={494} cy={326} r={8} fill={GOLD} />
        <rect
          x={370}
          y={400}
          width={52}
          height={96}
          rx={24}
          fill="#818CF8"
          stroke={LINE}
          strokeWidth={3}
        />
        <rect
          x={420}
          y={392}
          width={52}
          height={100}
          rx={24}
          fill="#6366F1"
          stroke={LINE}
          strokeWidth={3}
        />
        {/* Body */}
        <path
          d="M200 300 C200 220 300 196 384 224 C468 252 492 336 470 400 C458 440 424 462 380 462 L256 462 C214 462 196 420 196 380 Z"
          fill={`url(#gb${g.id})`}
          stroke={LINE}
          strokeWidth={3.5}
        />
        {/* Saddle cloth: crimson with a gold border and tassels */}
        <path
          d="M300 214 C360 204 430 230 460 290 L430 360 C380 330 330 324 290 336 Z"
          fill="#BE123C"
          stroke={GOLD}
          strokeWidth={6}
          strokeLinejoin="round"
        />
        {[300, 330, 360, 395, 428].map((x, i) => (
          <circle
            key={x}
            cx={x}
            cy={[336, 330, 330, 338, 356][i]}
            r={6}
            fill={GOLD}
          />
        ))}
        <path
          d="M330 250 L360 290 L330 310 L300 290 Z"
          fill={GOLD}
          opacity={0.9}
        />
        {/* Front legs */}
        <rect
          x={214}
          y={400}
          width={54}
          height={100}
          rx={24}
          fill="#A5B4FC"
          stroke={LINE}
          strokeWidth={3}
        />
        <rect
          x={270}
          y={404}
          width={54}
          height={96}
          rx={24}
          fill="#818CF8"
          stroke={LINE}
          strokeWidth={3}
        />
        {[230, 246, 286, 302].map((x) => (
          <ellipse key={x} cx={x} cy={492} rx={7} ry={5} fill="#EEF2FF" />
        ))}
        {/* Far ear */}
        <path
          d="M150 170 C96 170 70 236 106 296 L150 270 Z"
          fill="#818CF8"
          stroke={LINE}
          strokeWidth={3}
        />
        {/* Head */}
        <circle
          cx={214}
          cy={250}
          r={104}
          fill={`url(#gb${g.id})`}
          stroke={LINE}
          strokeWidth={3.5}
        />
        {/* Near ear: a big fan with sound-wave rings */}
        <path
          d="M262 160 C356 128 400 214 380 300 C366 362 300 376 262 334 C282 280 284 210 262 160 Z"
          fill="#C7D2FE"
          stroke={LINE}
          strokeWidth={3.5}
        />
        <path
          d="M280 186 C340 176 362 236 350 286 C340 326 306 336 286 318 C296 276 298 226 280 186 Z"
          fill="#F9A8D4"
          opacity={0.75}
        />
        {[0, 1, 2].map((i) => (
          <path
            key={i}
            d={`M${296 + i * 14} ${210 + i * 8} Q${330 + i * 6} ${252} ${300 + i * 12} ${300 - i * 6}`}
            stroke="#FFFFFF"
            strokeWidth={3}
            fill="none"
            opacity={0.75}
          />
        ))}
        {/* Trunk curling up, holding the thought */}
        <polygon
          points={trunk.outline}
          fill="#A5B4FC"
          stroke={LINE}
          strokeWidth={3}
          strokeLinejoin="round"
        />
        {trunk.spine
          .filter((_, i) => i % 4 === 2 && i < 26)
          .map((p, i) => (
            <line
              key={i}
              x1={p.x + p.nx * 18}
              y1={p.y + p.ny * 18}
              x2={p.x - p.nx * 18}
              y2={p.y - p.ny * 18}
              stroke="#6366F1"
              strokeWidth={2.5}
              opacity={0.6}
            />
          ))}
        {/* Forehead ornament (nettipattam) */}
        <path
          d="M214 150 L236 182 L214 214 L192 182 Z"
          fill={GOLD}
          stroke="#B45309"
          strokeWidth={2}
        />
        <circle cx={214} cy={182} r={9} fill="#E11D48" />
        {[176, 196, 232, 252].map((x) => (
          <circle key={x} cx={x} cy={x < 214 ? 196 : 196} r={4} fill={GOLD} />
        ))}
        {/* Face */}
        <Eye
          cx={200}
          cy={250}
          rx={15}
          ry={19}
          iris="#1E1B4B"
          pupil="#0B0A1F"
          lash="#1E1B4B"
        />
        <ellipse
          cx={248}
          cy={286}
          rx={18}
          ry={9}
          fill="#F472B6"
          opacity={0.45}
        />
        <path
          d="M206 296 Q222 308 238 298"
          stroke="#312E81"
          strokeWidth={3.5}
          fill="none"
          strokeLinecap="round"
        />
      </g>
      <Glowfly x={92} y={150} r={13} />
    </g>
  );
};

/* ----------------------------- KOYAL: koel bird ----------------------------- */
export const Koyal: React.FC = () => {
  const g = useGlow("#67E8F9");
  const feather = (
    ox: number,
    oy: number,
    ang: number,
    len: number,
    w: number,
    fill: string,
    k: number,
  ) => (
    <g key={k} transform={`translate(${ox} ${oy}) rotate(${ang})`}>
      <rect
        x={0}
        y={-w / 2}
        width={len}
        height={w}
        rx={w / 2}
        fill={fill}
        stroke="#CFFAFE"
        strokeWidth={2.5}
      />
      <circle cx={len - w / 2} cy={0} r={w * 0.28} fill={GOLD} />
    </g>
  );
  return (
    <g>
      <defs>
        {g.defs}
        <linearGradient id={`kb${g.id}`} x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0%" stopColor="#4338CA" />
          <stop offset="100%" stopColor="#1E1B4B" />
        </linearGradient>
        <linearGradient id={`kf${g.id}`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#2563EB" />
          <stop offset="100%" stopColor="#22D3EE" />
        </linearGradient>
      </defs>
      <ellipse cx={300} cy={300} rx={300} ry={230} fill={g.aura} />
      <g filter={g.filter}>
        {/* Far wing: equalizer feathers */}
        {[120, 170, 200, 170].map((len, i) =>
          feather(300, 250, -122 + i * 13, len * 0.8, 22, "#1D4ED8", i),
        )}
        {/* Tail: a waveform of feathers */}
        {[110, 170, 200, 160, 100].map((len, i) =>
          feather(420, 320, -10 + i * 7, len, 20, `url(#kf${g.id})`, 10 + i),
        )}
        {/* Body */}
        <path
          d="M196 300 C196 244 262 216 326 236 C392 256 432 296 446 320 C386 346 300 354 240 344 C212 338 196 324 196 300 Z"
          fill={`url(#kb${g.id})`}
          stroke="#A5F3FC"
          strokeWidth={3.5}
        />
        <path
          d="M240 344 C300 352 380 344 430 326 C380 334 300 336 250 320 Z"
          fill="#3730A3"
          opacity={0.7}
        />
        {/* Head */}
        <circle
          cx={214}
          cy={268}
          r={56}
          fill={`url(#kb${g.id})`}
          stroke="#A5F3FC"
          strokeWidth={3.5}
        />
        <path
          d="M184 226 Q214 210 244 226"
          stroke="#818CF8"
          strokeWidth={5}
          fill="none"
          opacity={0.8}
          strokeLinecap="round"
        />
        {/* Beak */}
        <path
          d="M164 262 L112 280 L164 292 Z"
          fill="#FDE68A"
          stroke="#B45309"
          strokeWidth={2.5}
          strokeLinejoin="round"
        />
        {/* The koel's ruby eye */}
        <Eye cx={206} cy={262} rx={13} ry={15} iris="#EF4444" pupil="#1F0A0A" />
        {/* Near wing: raised, mid-beat */}
        {[110, 160, 200, 190, 150].map((len, i) =>
          feather(300, 262, -128 + i * 14, len, 26, `url(#kf${g.id})`, 20 + i),
        )}
        <circle
          cx={300}
          cy={262}
          r={22}
          fill="#312E81"
          stroke="#A5F3FC"
          strokeWidth={3}
        />
        {/* Feet tucked */}
        <path
          d="M280 346 L272 372 M300 348 L296 374"
          stroke={GOLD}
          strokeWidth={5}
          strokeLinecap="round"
        />
      </g>
      <Glowfly x={106} y={282} r={12} />
      {/* Song: notes as little wave arcs */}
      {[0, 1, 2].map((i) => (
        <path
          key={i}
          d={`M${90 - i * 26} ${210 - i * 10} q-14 -30 0 -60`}
          stroke="#A5F3FC"
          strokeWidth={4}
          fill="none"
          opacity={0.7 - i * 0.2}
          strokeLinecap="round"
        />
      ))}
    </g>
  );
};

/* ----------------------------- NĀDA: little sound dragon ----------------------------- */
export const Nada: React.FC = () => {
  const g = useGlow("#5EEAD4");
  const pts: Pt[] = Array.from({ length: 70 }, (_, i) => {
    const t = i / 69;
    return [
      170 + t * 380,
      300 + Math.sin(t * Math.PI * 2.3) * 85 * (0.6 + 0.4 * t),
    ];
  });
  const body = ribbon(pts, (t) => 40 * (1 - 0.82 * t) + 4);
  const spikes = body.spine.filter((_, i) => i % 6 === 3 && i < 62);
  return (
    <g>
      <defs>
        {g.defs}
        <linearGradient id={`nb${g.id}`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#2DD4BF" />
          <stop offset="55%" stopColor="#6366F1" />
          <stop offset="100%" stopColor="#A855F7" />
        </linearGradient>
      </defs>
      <ellipse cx={330} cy={300} rx={300} ry={220} fill={g.aura} />
      <g filter={g.filter}>
        {/* Golden mane along the spine */}
        {spikes.map((p, i) => (
          <polygon
            key={i}
            points={`${p.x + p.nx * 30},${p.y + p.ny * 30} ${p.x - 10},${p.y - 4} ${p.x + 10},${p.y + 4}`}
            fill={GOLD}
            opacity={0.95}
            transform={`translate(${-p.nx * 50 * (1 - p.t * 0.7)} ${-p.ny * 50 * (1 - p.t * 0.7)})`}
          />
        ))}
        <polygon
          points={body.outline}
          fill={`url(#nb${g.id})`}
          stroke={LINE}
          strokeWidth={3.5}
          strokeLinejoin="round"
        />
        {/* Belly stripe: a waveform */}
        <path
          d={body.spine
            .map(
              (p, i) =>
                `${i ? "L" : "M"}${p.x + p.nx * 14 * (1 - p.t * 0.8) + Math.sin(i * 0.9) * 3},${p.y + p.ny * 14 * (1 - p.t * 0.8)}`,
            )
            .join(" ")}
          stroke="#CCFBF1"
          strokeWidth={5}
          fill="none"
          opacity={0.8}
        />
        <circle cx={pts[69][0]} cy={pts[69][1]} r={10} fill={GOLD} />
        {/* Small front claw holding the thought */}
        <path
          d="M200 330 L150 352 L140 342"
          stroke="#2DD4BF"
          strokeWidth={14}
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* Head, facing left */}
        <g transform="translate(150 250)">
          {/* Tuning-fork horns */}
          {[
            [10, -40, -18],
            [42, -34, 8],
          ].map(([hx, hy, rot], i) => (
            <g key={i} transform={`translate(${hx} ${hy}) rotate(${rot})`}>
              <path
                d="M0 0 L0 -40 M-12 -40 L-12 -74 M12 -40 L12 -74 M-12 -40 L12 -40"
                stroke={GOLD}
                strokeWidth={8}
                strokeLinecap="round"
                fill="none"
              />
            </g>
          ))}
          <path
            d="M-70 10 C-70 -26 -20 -48 30 -42 C76 -36 92 6 76 40 C60 70 14 76 -20 64 L-74 44 Q-90 30 -70 10 Z"
            fill={`url(#nb${g.id})`}
            stroke={LINE}
            strokeWidth={3.5}
          />
          <path
            d="M-74 44 Q-40 52 -10 40 L-20 64 Z"
            fill="#CCFBF1"
            opacity={0.8}
          />
          <ellipse cx={-64} cy={20} rx={4} ry={3} fill="#134E4A" />
          {/* Whiskers */}
          <path
            d="M-60 34 C-110 40 -130 80 -170 70"
            stroke="#99F6E4"
            strokeWidth={3}
            fill="none"
            strokeLinecap="round"
          />
          <path
            d="M-56 26 C-100 10 -130 30 -160 10"
            stroke="#99F6E4"
            strokeWidth={3}
            fill="none"
            strokeLinecap="round"
          />
          <Eye cx={0} cy={0} rx={15} ry={18} iris={GOLD} pupil="#1E1B4B" slit />
          <path
            d="M-16 -20 L18 -24"
            stroke="#134E4A"
            strokeWidth={4}
            strokeLinecap="round"
          />
        </g>
      </g>
      <Glowfly x={126} y={360} r={12} />
    </g>
  );
};

/* ----------------------------- DOT: recorder robot ----------------------------- */
export const Dot: React.FC = () => {
  const g = useGlow("#93C5FD");
  return (
    <g>
      <defs>
        {g.defs}
        <radialGradient id={`db${g.id}`} cx="38%" cy="30%" r="80%">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="60%" stopColor="#E2E8F0" />
          <stop offset="100%" stopColor="#94A3B8" />
        </radialGradient>
        <linearGradient id={`dj${g.id}`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#E0F2FE" stopOpacity={0.5} />
          <stop offset="50%" stopColor="#FFFFFF" stopOpacity={0.15} />
          <stop offset="100%" stopColor="#E0F2FE" stopOpacity={0.45} />
        </linearGradient>
      </defs>
      <ellipse cx={290} cy={300} rx={280} ry={230} fill={g.aura} />
      {/* Hover glow */}
      <ellipse
        cx={270}
        cy={500}
        rx={110}
        ry={20}
        fill="#38BDF8"
        opacity={0.35}
      />
      <path
        d="M240 470 L230 500 M270 474 L270 510 M300 470 L310 500"
        stroke="#7DD3FC"
        strokeWidth={6}
        strokeLinecap="round"
        opacity={0.7}
      />
      {/* Antenna with a little spark of its own */}
      <line
        x1={270}
        y1={150}
        x2={270}
        y2={96}
        stroke="#94A3B8"
        strokeWidth={8}
        strokeLinecap="round"
      />
      <circle cx={270} cy={86} r={14} fill={GOLD} />
      <circle cx={270} cy={86} r={30} fill={GOLD} opacity={0.25} />
      {/* Headphone ear discs */}
      <circle
        cx={122}
        cy={300}
        r={34}
        fill="#2563EB"
        stroke="#1E3A8A"
        strokeWidth={4}
      />
      <circle
        cx={418}
        cy={300}
        r={34}
        fill="#2563EB"
        stroke="#1E3A8A"
        strokeWidth={4}
      />
      {/* Body */}
      <circle
        cx={270}
        cy={310}
        r={160}
        fill={`url(#db${g.id})`}
        stroke="#CBD5E1"
        strokeWidth={3}
      />
      <path
        d="M150 230 Q200 170 270 166"
        stroke="#FFFFFF"
        strokeWidth={14}
        fill="none"
        strokeLinecap="round"
        opacity={0.9}
      />
      {/* Visor face */}
      <rect x={168} y={230} width={204} height={110} rx={55} fill="#0F172A" />
      <path
        d="M206 290 Q228 262 250 290"
        stroke="#22D3EE"
        strokeWidth={9}
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M290 290 Q312 262 334 290"
        stroke="#22D3EE"
        strokeWidth={9}
        fill="none"
        strokeLinecap="round"
      />
      <ellipse cx={200} cy={318} rx={12} ry={6} fill="#F472B6" opacity={0.8} />
      <ellipse cx={340} cy={318} rx={12} ry={6} fill="#F472B6" opacity={0.8} />
      {/* The record button, its heart */}
      <circle
        cx={270}
        cy={400}
        r={30}
        fill="#FFFFFF"
        stroke="#CBD5E1"
        strokeWidth={3}
      />
      <circle cx={270} cy={400} r={20} fill="#EF4444" />
      {/* Floating hands, one holding a jar with the caught thought */}
      <ellipse
        cx={130}
        cy={410}
        rx={26}
        ry={20}
        fill={`url(#db${g.id})`}
        stroke="#CBD5E1"
        strokeWidth={3}
      />
      <g transform="translate(410 300)">
        <rect
          x={0}
          y={20}
          width={110}
          height={140}
          rx={30}
          fill={`url(#dj${g.id})`}
          stroke="#E0F2FE"
          strokeWidth={4}
        />
        <rect
          x={10}
          y={4}
          width={90}
          height={24}
          rx={8}
          fill="#2563EB"
          stroke="#1E3A8A"
          strokeWidth={3}
        />
        <Glowfly x={55} y={92} r={13} />
        <path
          d="M18 50 L18 130"
          stroke="#FFFFFF"
          strokeWidth={6}
          strokeLinecap="round"
          opacity={0.6}
        />
      </g>
      <ellipse
        cx={414}
        cy={430}
        rx={26}
        ry={20}
        fill={`url(#db${g.id})`}
        stroke="#CBD5E1"
        strokeWidth={3}
      />
    </g>
  );
};

/* ----------------------------- ECHO: cat in headphones ----------------------------- */
export const Echo: React.FC = () => {
  const g = useGlow("#C4B5FD");
  const tailPts: Pt[] = Array.from({ length: 50 }, (_, i) => {
    const t = i / 49;
    return [370 + t * 160, 440 - t * 300 + Math.sin(t * Math.PI * 3) * 34];
  });
  const tail = ribbon(tailPts, (t) => 17 - 9 * t);
  return (
    <g>
      <defs>
        {g.defs}
        <linearGradient id={`eb${g.id}`} x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0%" stopColor="#475569" />
          <stop offset="100%" stopColor="#1E293B" />
        </linearGradient>
      </defs>
      <ellipse cx={300} cy={320} rx={290} ry={220} fill={g.aura} />
      <g filter={g.filter}>
        <polygon
          points={tail.outline}
          fill={`url(#eb${g.id})`}
          stroke="#DDD6FE"
          strokeWidth={3}
          strokeLinejoin="round"
        />
        <circle cx={tailPts[49][0]} cy={tailPts[49][1]} r={10} fill={GOLD} />
        {/* Body */}
        <path
          d="M232 270 C196 320 190 430 214 476 L376 476 C404 430 396 330 348 276 Z"
          fill={`url(#eb${g.id})`}
          stroke="#DDD6FE"
          strokeWidth={3.5}
        />
        <path
          d="M242 300 C232 340 240 380 262 400 C282 370 284 330 276 300 Z"
          fill="#F1F5F9"
          opacity={0.9}
        />
        <ellipse
          cx={244}
          cy={478}
          rx={26}
          ry={12}
          fill="#F1F5F9"
          stroke="#DDD6FE"
          strokeWidth={3}
        />
        {/* Raised paw swatting at the thought */}
        <path
          d="M240 300 C214 276 196 250 186 220"
          stroke="#334155"
          strokeWidth={38}
          strokeLinecap="round"
          fill="none"
        />
        <circle
          cx={184}
          cy={212}
          r={24}
          fill="#F1F5F9"
          stroke="#DDD6FE"
          strokeWidth={3}
        />
        {/* Head */}
        <path
          d="M206 150 L218 70 L262 128 Z"
          fill="#334155"
          stroke="#DDD6FE"
          strokeWidth={3}
          strokeLinejoin="round"
        />
        <path
          d="M300 128 L344 70 L350 152 Z"
          fill="#334155"
          stroke="#DDD6FE"
          strokeWidth={3}
          strokeLinejoin="round"
        />
        <path d="M216 144 L222 92 L250 130 Z" fill="#F9A8D4" opacity={0.7} />
        <ellipse
          cx={280}
          cy={200}
          rx={92}
          ry={80}
          fill={`url(#eb${g.id})`}
          stroke="#DDD6FE"
          strokeWidth={3.5}
        />
        {/* Over-ear headphones in Srutam cobalt */}
        <path
          d="M188 190 C188 96 372 96 372 190"
          stroke="#1E3A8A"
          strokeWidth={14}
          fill="none"
          strokeLinecap="round"
        />
        <rect
          x={168}
          y={170}
          width={42}
          height={76}
          rx={20}
          fill="#2563EB"
          stroke={GOLD}
          strokeWidth={4}
        />
        <rect
          x={352}
          y={170}
          width={42}
          height={76}
          rx={20}
          fill="#2563EB"
          stroke={GOLD}
          strokeWidth={4}
        />
        {/* Face */}
        <Eye
          cx={252}
          cy={196}
          rx={16}
          ry={20}
          iris={GOLD}
          pupil="#0F172A"
          slit
        />
        <Eye
          cx={312}
          cy={196}
          rx={16}
          ry={20}
          iris={GOLD}
          pupil="#0F172A"
          slit
        />
        <path d="M276 226 L288 226 L282 234 Z" fill="#F472B6" />
        <path
          d="M268 240 Q276 248 282 240 Q288 248 296 240"
          stroke="#E2E8F0"
          strokeWidth={3}
          fill="none"
          strokeLinecap="round"
        />
        {[-1, 1].map((d) => (
          <g
            key={d}
            stroke="#E2E8F0"
            strokeWidth={2.5}
            strokeLinecap="round"
            opacity={0.85}
          >
            <path d={`M${282 + d * 30} 232 L${282 + d * 90} 222`} />
            <path d={`M${282 + d * 30} 240 L${282 + d * 92} 246`} />
          </g>
        ))}
      </g>
      <Glowfly x={150} y={160} r={12} />
    </g>
  );
};

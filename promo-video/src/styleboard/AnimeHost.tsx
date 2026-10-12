import { useId } from "react";

const LINE = "#3B2A3F";
const SKIN = "#FDE7D8";
const SKIN_SHADE = "#F4C7B3";
const HAIR = "#1E1B4B";
const HAIR_SHINE = "#6366F1";

type Lighting = "sunset" | "phone";

/**
 * The Host as an anime commuter (bust, front view, 600x800 local units):
 * cel-shaded skin and hair, glossy eyes, headphones round the neck.
 * `lighting` adds a warm window rim light or a cool phone glow from below.
 */
export const AnimeHost: React.FC<{
  x: number;
  y: number;
  s: number;
  lighting?: Lighting;
  mouth?: "smile" | "open";
}> = ({ x, y, s, lighting = "sunset", mouth = "smile" }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const face =
    "M195 270 C195 190 245 160 300 160 C355 160 405 190 405 270 L400 330 C392 380 345 425 300 432 C255 425 208 380 200 330 Z";

  const eye = (cx: number, flip: boolean) => (
    <g>
      <ellipse cx={cx} cy={312} rx={28} ry={25} fill="#FFFFFF" />
      <ellipse
        cx={cx + (flip ? -2 : 2)}
        cy={313}
        rx={20}
        ry={24}
        fill={`url(#iris${id})`}
      />
      <ellipse
        cx={cx + (flip ? -2 : 2)}
        cy={311}
        rx={8}
        ry={12}
        fill="#0B1026"
      />
      <circle cx={cx - 8} cy={300} r={7.5} fill="#FFFFFF" />
      <circle cx={cx + 8} cy={324} r={3.5} fill="#FFFFFF" />
      <path
        d={`M${cx - 33} 300 Q${cx} 272 ${cx + 33} 296 L${cx + 38} 289 L${cx + 33} 303 Q${cx} 284 ${cx - 31} 306 Z`}
        fill={LINE}
        transform={flip ? `translate(${2 * cx} 0) scale(-1 1)` : undefined}
      />
      <path
        d={`M${cx - 16} 336 Q${cx} 340 ${cx + 16} 336`}
        stroke={LINE}
        strokeWidth={2.5}
        fill="none"
        strokeLinecap="round"
      />
    </g>
  );

  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <defs>
        <linearGradient id={`iris${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#1E3A8A" />
          <stop offset="65%" stopColor="#2563EB" />
          <stop offset="100%" stopColor="#7DD3FC" />
        </linearGradient>
        <clipPath id={`face${id}`}>
          <path d={face} />
        </clipPath>
        <linearGradient id={`phone${id}`} x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stopColor="#60A5FA" stopOpacity={0.75} />
          <stop offset="70%" stopColor="#60A5FA" stopOpacity={0} />
        </linearGradient>
      </defs>

      {/* Back hair */}
      <path
        d="M150 300 C130 150 220 92 300 92 C380 92 470 150 450 300 L462 440 C440 410 430 385 425 360 L175 360 C170 385 160 410 138 440 Z"
        fill={HAIR}
        stroke={LINE}
        strokeWidth={4}
      />
      {/* Body: jacket with a cel-shaded fold */}
      <path
        d="M110 800 C120 560 200 482 300 472 C400 482 480 560 490 800 Z"
        fill="#334155"
        stroke={LINE}
        strokeWidth={5}
      />
      <path
        d="M300 472 C400 482 480 560 490 800 L380 800 C390 640 360 540 300 500 Z"
        fill="#1E293B"
      />
      <path
        d="M248 476 L300 560 L352 476"
        fill="none"
        stroke={LINE}
        strokeWidth={5}
      />
      {/* Neck */}
      <path
        d="M268 400 L332 400 L336 486 Q300 510 264 486 Z"
        fill={SKIN_SHADE}
        stroke={LINE}
        strokeWidth={4}
      />
      {/* Headphones round the neck */}
      <path
        d="M215 470 Q300 560 385 470"
        stroke="#0F172A"
        strokeWidth={20}
        fill="none"
        strokeLinecap="round"
      />
      <ellipse
        cx={212}
        cy={478}
        rx={26}
        ry={34}
        fill="#2563EB"
        stroke={LINE}
        strokeWidth={4}
      />
      <ellipse
        cx={388}
        cy={478}
        rx={26}
        ry={34}
        fill="#2563EB"
        stroke={LINE}
        strokeWidth={4}
      />
      {/* Ears */}
      <ellipse
        cx={197}
        cy={308}
        rx={16}
        ry={26}
        fill={SKIN_SHADE}
        stroke={LINE}
        strokeWidth={4}
      />
      <ellipse
        cx={403}
        cy={308}
        rx={16}
        ry={26}
        fill={SKIN_SHADE}
        stroke={LINE}
        strokeWidth={4}
      />
      {/* Face */}
      <path d={face} fill={SKIN} stroke={LINE} strokeWidth={4} />
      <g clipPath={`url(#face${id})`}>
        {/* Hard cel shadow cast by the bangs */}
        <path
          d="M180 150 L420 150 L420 262 C380 250 360 280 330 262 C300 284 270 252 240 270 C215 256 200 280 180 270 Z"
          fill={SKIN_SHADE}
        />
        {lighting === "phone" && (
          <rect
            x={150}
            y={150}
            width={300}
            height={300}
            fill={`url(#phone${id})`}
          />
        )}
        {lighting === "sunset" && (
          <path
            d="M380 160 L420 160 L420 440 L360 440 C392 380 400 300 380 160 Z"
            fill="#FDBA74"
            opacity={0.55}
          />
        )}
      </g>
      {/* Brows, eyes, nose, mouth, blush */}
      <path
        d="M222 262 Q250 250 280 260"
        stroke={LINE}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M320 260 Q350 250 378 262"
        stroke={LINE}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
      />
      {eye(250, false)}
      {eye(350, true)}
      <path
        d="M301 348 L297 364 L304 365"
        stroke={LINE}
        strokeWidth={2.5}
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {mouth === "smile" ? (
        <path
          d="M285 390 Q300 400 315 390"
          stroke={LINE}
          strokeWidth={3.5}
          fill="none"
          strokeLinecap="round"
        />
      ) : (
        <path
          d="M290 388 Q300 384 310 388 Q308 404 300 405 Q292 404 290 388 Z"
          fill="#9F1239"
          stroke={LINE}
          strokeWidth={3}
        />
      )}
      {[228, 372].map((bx) => (
        <g key={bx}>
          <ellipse
            cx={bx}
            cy={358}
            rx={22}
            ry={9}
            fill="#FB7185"
            opacity={0.35}
          />
          {[-9, 0, 9].map((h) => (
            <path
              key={h}
              d={`M${bx + h - 3} 364 L${bx + h + 3} 352`}
              stroke="#E11D48"
              strokeWidth={2}
              strokeLinecap="round"
              opacity={0.6}
            />
          ))}
        </g>
      ))}
      {/* Front bangs and side locks */}
      <path
        d="M182 285 C176 168 238 118 300 118 C366 118 428 168 420 288 L402 232 L388 276 L362 206 L346 262 L318 200 L300 256 L282 198 L256 262 L238 208 L220 274 L204 230 Z"
        fill={HAIR}
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      <path
        d="M188 258 C172 330 180 390 200 430 L216 336 Z"
        fill={HAIR}
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      <path
        d="M412 258 C428 330 420 390 400 430 L384 336 Z"
        fill={HAIR}
        stroke={LINE}
        strokeWidth={4}
        strokeLinejoin="round"
      />
      {/* Hair "angel ring" shine */}
      <path
        d="M222 168 Q300 140 378 168 L372 180 Q300 156 228 180 Z"
        fill={HAIR_SHINE}
        opacity={0.85}
      />
      {lighting === "sunset" && (
        <path
          d="M410 200 C430 260 432 330 418 400"
          stroke="#FDBA74"
          strokeWidth={6}
          fill="none"
          strokeLinecap="round"
        />
      )}
    </g>
  );
};

/** Extreme close-up of the Host's eye with the gold spark reflected in the iris. */
export const EyeCloseUp: React.FC<{ w: number; h: number }> = ({ w, h }) => {
  const cx = w / 2;
  const cy = h * 0.52;
  return (
    <g>
      <defs>
        <linearGradient id="ecuIris" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#0B1F5C" />
          <stop offset="55%" stopColor="#2563EB" />
          <stop offset="100%" stopColor="#7DD3FC" />
        </linearGradient>
        <radialGradient id="ecuSpark">
          <stop offset="0%" stopColor="#FFFBEB" />
          <stop offset="40%" stopColor="#FBBF24" />
          <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
        </radialGradient>
        <clipPath id="ecuEye">
          <path
            d={`M${cx - 210} ${cy + 10} Q${cx} ${cy - 210} ${cx + 210} ${cy - 10} Q${cx} ${cy + 190} ${cx - 210} ${cy + 10} Z`}
          />
        </clipPath>
      </defs>
      <rect width={w} height={h} fill={SKIN} />
      <path
        d={`M0 0 L${w} 0 L${w} ${h * 0.3} C${w * 0.7} ${h * 0.24} ${w * 0.4} ${h * 0.34} 0 ${h * 0.28} Z`}
        fill={SKIN_SHADE}
      />
      {/* Bangs strands */}
      {[0, 1, 2, 3, 4].map((i) => (
        <path
          key={i}
          d={`M${i * 110 - 20} -10 L${i * 110 + 60} ${h * 0.2 + (i % 2) * 40} L${i * 110 + 120} -10 Z`}
          fill={HAIR}
          stroke={LINE}
          strokeWidth={4}
        />
      ))}
      <path
        d={`M${cx - 150} ${cy - 170} Q${cx} ${cy - 230} ${cx + 160} ${cy - 160}`}
        stroke={LINE}
        strokeWidth={12}
        fill="none"
        strokeLinecap="round"
      />
      <g clipPath="url(#ecuEye)">
        <rect x={0} y={0} width={w} height={h} fill="#FFFFFF" />
        <ellipse
          cx={cx + 6}
          cy={cy + 4}
          rx={128}
          ry={150}
          fill="url(#ecuIris)"
        />
        <ellipse cx={cx + 6} cy={cy} rx={52} ry={70} fill="#06102E" />
        {/* The spark, reflected */}
        <circle cx={cx + 46} cy={cy + 40} r={60} fill="url(#ecuSpark)" />
        <circle cx={cx + 46} cy={cy + 40} r={14} fill="#FFFBEB" />
        <circle cx={cx - 44} cy={cy - 56} r={30} fill="#FFFFFF" />
        <circle cx={cx + 70} cy={cy + 82} r={11} fill="#FFFFFF" />
        <path
          d={`M${cx - 210} ${cy - 40} Q${cx} ${cy - 170} ${cx + 210} ${cy - 50} L${cx + 210} ${cy - 120} L${cx - 210} ${cy - 120} Z`}
          fill="#94A3B8"
          opacity={0.25}
        />
      </g>
      {/* Upper lash line, heavy, with flicks */}
      <path
        d={`M${cx - 230} ${cy + 20} Q${cx} ${cy - 236} ${cx + 226} ${cy - 18} L${cx + 262} ${cy - 54} L${cx + 236} ${cy + 4} Q${cx} ${cy - 196} ${cx - 214} ${cy + 26} Z`}
        fill={LINE}
      />
      <path
        d={`M${cx - 150} ${cy + 120} Q${cx} ${cy + 200} ${cx + 160} ${cy + 96}`}
        stroke={LINE}
        strokeWidth={5}
        fill="none"
        strokeLinecap="round"
      />
    </g>
  );
};

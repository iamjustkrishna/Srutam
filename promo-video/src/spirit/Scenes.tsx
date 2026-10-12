// Painted environments and story frames for "Shru", all at 1080 x 1920 so
// every storyboard frame doubles as a frame of the eventual video.
import { useId } from "react";
import { rand } from "../anim";
import { Cloud, Sparkle, SpeedLines } from "../styleboard/FX";
import { LOGO } from "./logoData";
import { MeeraHD as Meera } from "./MeeraHD";
import { Firefly, Head, Shru } from "./Shru";

export const W = 1080;
export const H = 1920;

/** Film grain + vignette, laid over every frame. */
export const Finish: React.FC<{ vignette?: number }> = ({
  vignette = 0.55,
}) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <g pointerEvents="none">
      <defs>
        <filter id={`grain${id}`}>
          <feTurbulence
            type="fractalNoise"
            baseFrequency={0.85}
            numOctaves={2}
            seed={4}
          />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <radialGradient id={`vig${id}`} cx="50%" cy="48%" r="75%">
          <stop offset="55%" stopColor="#0B0718" stopOpacity={0} />
          <stop offset="100%" stopColor="#0B0718" stopOpacity={vignette} />
        </radialGradient>
      </defs>
      <rect
        width={W}
        height={H}
        filter={`url(#grain${id})`}
        opacity={0.07}
        style={{ mixBlendMode: "overlay" }}
      />
      <rect width={W} height={H} fill={`url(#vig${id})`} />
    </g>
  );
};

/** Inside a Mumbai local at golden hour: open door, sea link, light shafts. */
export const TrainDusk: React.FC<{ dim?: number }> = ({ dim = 0 }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <g>
      <defs>
        <linearGradient id={`wall${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#33245C" />
          <stop offset="100%" stopColor="#170F2E" />
        </linearGradient>
        <linearGradient id={`sky${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#5B3FA8" />
          <stop offset="38%" stopColor="#E26AA8" />
          <stop offset="62%" stopColor="#FB923C" />
          <stop offset="74%" stopColor="#FDE68A" />
        </linearGradient>
        <linearGradient id={`sea${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#F59E0B" />
          <stop offset="35%" stopColor="#9A3412" />
          <stop offset="100%" stopColor="#2E1458" />
        </linearGradient>
        <radialGradient id={`sun${id}`}>
          <stop offset="0%" stopColor="#FFF7D6" />
          <stop offset="18%" stopColor="#FDE68A" stopOpacity={0.95} />
          <stop offset="100%" stopColor="#FB923C" stopOpacity={0} />
        </radialGradient>
        <linearGradient id={`ray${id}`} x1="1" y1="0" x2="0" y2="0.3">
          <stop offset="0%" stopColor="#FFE7B3" stopOpacity={0.28} />
          <stop offset="100%" stopColor="#FFE7B3" stopOpacity={0} />
        </linearGradient>
        <linearGradient id={`pole${id}`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#FFD9A0" />
          <stop offset="35%" stopColor="#94A3B8" />
          <stop offset="100%" stopColor="#1E1B3A" />
        </linearGradient>
        <clipPath id={`door${id}`}>
          <rect x={620} y={300} width={460} height={1280} />
        </clipPath>
        <clipPath id={`win${id}`}>
          <rect x={70} y={430} width={400} height={520} rx={40} />
        </clipPath>
      </defs>
      <rect width={W} height={H} fill={`url(#wall${id})`} />

      {/* The open door: the city sliding by at sunset */}
      <g clipPath={`url(#door${id})`}>
        <rect
          x={620}
          y={300}
          width={460}
          height={1280}
          fill={`url(#sky${id})`}
        />
        <circle cx={880} cy={1110} r={420} fill={`url(#sun${id})`} />
        <circle cx={880} cy={1110} r={62} fill="#FFF7D6" />
        <Cloud x={690} y={560} s={1.1} lit="#FBCFE8" shade="#A78BFA" />
        <Cloud x={930} y={800} s={0.7} lit="#FED7AA" shade="#E879F9" />
        {Array.from({ length: 12 }, (_, i) => (
          <rect
            key={i}
            x={620 + i * 40}
            y={1092 - rand(i + 3) * 90}
            width={34}
            height={120}
            fill="#7E3F8F"
            opacity={0.85}
          />
        ))}
        {/* Bandra-Worli sea link */}
        <g stroke="#4A1D5E" strokeWidth={3}>
          {Array.from({ length: 9 }, (_, i) => (
            <line key={`l${i}`} x1={790} y1={960} x2={660 + i * 18} y2={1122} />
          ))}
          {Array.from({ length: 9 }, (_, i) => (
            <line key={`r${i}`} x1={790} y1={960} x2={810 + i * 18} y2={1122} />
          ))}
        </g>
        <path d="M778 1126 L786 950 L794 950 L802 1126 Z" fill="#4A1D5E" />
        <rect x={620} y={1120} width={460} height={10} fill="#4A1D5E" />
        <rect
          x={620}
          y={1130}
          width={460}
          height={450}
          fill={`url(#sea${id})`}
        />
        {Array.from({ length: 16 }, (_, i) => (
          <rect
            key={i}
            x={880 - 30 - rand(i) * 40}
            y={1140 + i * 22}
            width={60 + rand(i * 2) * 80}
            height={5}
            rx={2.5}
            fill="#FDE68A"
            opacity={0.75 - i * 0.04}
          />
        ))}
      </g>

      {/* Side window: motion-blurred city */}
      <g clipPath={`url(#win${id})`}>
        <rect
          x={70}
          y={430}
          width={400}
          height={520}
          fill={`url(#sky${id})`}
          opacity={0.75}
        />
        {Array.from({ length: 22 }, (_, i) => (
          <rect
            key={i}
            x={20 + rand(i + 40) * 300}
            y={520 + rand(i * 3 + 40) * 420}
            width={140 + rand(i) * 220}
            height={6 + rand(i * 7) * 14}
            rx={6}
            fill={i % 3 ? "#FBCFE8" : "#7E3F8F"}
            opacity={0.5}
          />
        ))}
      </g>
      <rect
        x={70}
        y={430}
        width={400}
        height={520}
        rx={40}
        fill="none"
        stroke="#120C26"
        strokeWidth={26}
      />

      {/* Door frame and the central pole */}
      <rect x={596} y={260} width={42} height={1400} fill="#120C26" />
      <rect x={596} y={260} width={500} height={56} fill="#120C26" />
      <rect
        x={760}
        y={300}
        width={24}
        height={1300}
        rx={12}
        fill={`url(#pole${id})`}
      />

      {/* Light shafts through the door */}
      {[
        "620,320 620,620 0,1150 0,760",
        "620,700 620,980 0,1620 0,1300",
        "620,1040 620,1200 0,1900 0,1720",
      ].map((p) => (
        <polygon key={p} points={p} fill={`url(#ray${id})`} />
      ))}
      {Array.from({ length: 70 }, (_, i) => {
        const x = rand(i + 7) * 640;
        const y = 600 + rand(i * 5 + 7) * 1100;
        return (
          <circle
            key={i}
            cx={x}
            cy={y}
            r={1.5 + rand(i * 9) * 3}
            fill="#FFE7B3"
            opacity={0.25 + rand(i * 11) * 0.6}
          />
        );
      })}

      {/* Overhead rail and swinging grab handles */}
      <rect x={0} y={150} width={W} height={18} rx={9} fill="#120C26" />
      {[110, 290, 470, 650, 830, 1010].map((x, i) => (
        <g key={x} transform={`rotate(${(i % 2 ? 1 : -1) * (4 + i)} ${x} 160)`}>
          <rect
            x={x - 8}
            y={160}
            width={16}
            height={120}
            rx={6}
            fill="#120C26"
          />
          <ellipse
            cx={x}
            cy={322}
            rx={40}
            ry={46}
            fill="none"
            stroke="#120C26"
            strokeWidth={16}
          />
          <path
            d={`M${x + 34} 296 A40 46 0 0 1 ${x + 30} 356`}
            stroke="#FFB27A"
            strokeWidth={4}
            fill="none"
            opacity={0.8}
          />
        </g>
      ))}
      {dim > 0 && <rect width={W} height={H} fill="#0B0718" opacity={dim} />}
    </g>
  );
};

/** A phone held low (hand below frame), screen glowing, the floating dock on its edge. */
export const PhoneInHand: React.FC<{
  x: number;
  y: number;
  rot: number;
  s?: number;
  dockGlow?: number;
}> = ({ x, y, rot, s = 1, dockGlow = 1 }) => (
  <g transform={`translate(${x} ${y}) rotate(${rot}) scale(${s})`}>
    <ellipse
      cx={170}
      cy={340}
      rx={380}
      ry={420}
      fill="#38BDF8"
      opacity={0.12}
    />
    <rect
      x={0}
      y={0}
      width={340}
      height={700}
      rx={48}
      fill="#0B0F1A"
      stroke="#2A3142"
      strokeWidth={6}
    />
    <rect x={14} y={14} width={312} height={672} rx={38} fill="#0C1225" />
    {[90, 250, 410].map((y0) => (
      <g key={y0}>
        <rect x={40} y={y0} width={260} height={120} rx={20} fill="#1B2540" />
        <rect
          x={60}
          y={y0 + 24}
          width={150}
          height={14}
          rx={7}
          fill="#2D3A5C"
        />
        <rect
          x={60}
          y={y0 + 54}
          width={210}
          height={12}
          rx={6}
          fill="#24304E"
        />
      </g>
    ))}
    {/* Floating dock tab on the left edge, glowing: Shru's home */}
    <circle cx={30} cy={330} r={110} fill="#67E8F9" opacity={0.3 * dockGlow} />
    <path
      d="M14 296 L58 296 Q82 296 82 320 L82 340 Q82 364 58 364 L14 364 Z"
      fill="#0F172A"
      stroke="#E0F2FE"
      strokeWidth={3}
    />
    <image href={LOGO} x={22} y={302} width={56} height={56} />
  </g>
);

/** Text that flows along a waveform: the caught thought becoming words. */
export const WordRibbon: React.FC<{
  d: string;
  text: string;
  size?: number;
  color?: string;
}> = ({ d, text, size = 40, color = "#FFF7D6" }) => {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  return (
    <g>
      <path id={`rib${id}`} d={d} fill="none" />
      <path
        d={d}
        stroke="#FDE68A"
        strokeWidth={size * 1.6}
        strokeLinecap="round"
        fill="none"
        opacity={0.12}
      />
      <text
        fontFamily="Inter"
        fontWeight={700}
        fontSize={size}
        fill={color}
        style={{ filter: "drop-shadow(0 0 8px rgba(251,191,36,0.9))" }}
      >
        <textPath href={`#rib${id}`}>{text}</textPath>
      </text>
    </g>
  );
};

/** Burned-in subtitle as it will appear in the video. */
export const Sub: React.FC<{ text: string; y?: number }> = ({
  text,
  y = 1700,
}) => (
  <text
    x={W / 2}
    y={y}
    textAnchor="middle"
    fontFamily="Inter"
    fontWeight={800}
    fontSize={62}
    fill="#FFFFFF"
    stroke="#0B0718"
    strokeWidth={10}
    paintOrder="stroke"
  >
    {text}
  </text>
);

/* ---------------------------- The key visual ---------------------------- */

export const KeyVisual: React.FC = () => (
  <g>
    <defs>
      <linearGradient id="kvTop" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#120C26" stopOpacity={0.95} />
        <stop offset="70%" stopColor="#120C26" stopOpacity={0.6} />
        <stop offset="100%" stopColor="#120C26" stopOpacity={0} />
      </linearGradient>
      <linearGradient id="kvBottom" x1="0" y1="1" x2="0" y2="0">
        <stop offset="0%" stopColor="#0B0718" stopOpacity={0.9} />
        <stop offset="100%" stopColor="#0B0718" stopOpacity={0} />
      </linearGradient>
    </defs>
    <TrainDusk />
    <Meera x={330} y={720} s={1.32} expr="awe" look={-1} />
    <PhoneInHand x={70} y={1460} rot={-12} />
    {/* The light trail from the dock up into Shru's tails */}
    <path
      d="M150 1790 C120 1620 360 1560 470 1390"
      stroke="#67E8F9"
      strokeWidth={40}
      fill="none"
      opacity={0.2}
      strokeLinecap="round"
    />
    <path
      d="M150 1790 C120 1620 360 1560 470 1390"
      stroke="#E0F2FE"
      strokeWidth={5}
      fill="none"
      opacity={0.85}
      strokeLinecap="round"
      strokeDasharray="2 14"
    />
    <g transform="translate(400 1110) rotate(60)">
      <Shru x={-315} y={-210} s={1.05} pose="leap" eyes="fierce" phase={1} />
    </g>
    <Firefly x={290} y={690} r={30} life={0.45} />
    {[
      [210, 600, 20],
      [400, 760, 14],
      [150, 820, 10],
    ].map(([x, y, s]) => (
      <Sparkle key={`${x}`} x={x} y={y} s={s} color="#FDE68A" />
    ))}
    <rect width={W} height={480} fill="url(#kvTop)" />
    <rect y={1700} width={W} height={220} fill="url(#kvBottom)" />
    {/* Title block */}
    <text
      x={W / 2}
      y={230}
      textAnchor="middle"
      fontFamily="Playfair Display"
      fontSize={150}
      letterSpacing={22}
      fill="#FFFFFF"
      style={{ filter: "drop-shadow(0 6px 24px rgba(11,7,24,0.8))" }}
    >
      SRUTAM
    </text>
    <text
      x={W / 2}
      y={300}
      textAnchor="middle"
      fontFamily="Playfair Display"
      fontStyle="italic"
      fontSize={46}
      fill="#FFE7B3"
    >
      Every thought deserves to be heard.
    </text>
    <text
      x={W / 2}
      y={1860}
      textAnchor="middle"
      fontFamily="Inter"
      fontWeight={600}
      fontSize={26}
      letterSpacing={8}
      fill="#E9D5FF"
      opacity={0.9}
    >
      CATCH IT BEFORE IT'S GONE
    </text>
    <Finish />
  </g>
);

/* ---------------------------- Story frames ---------------------------- */

export const FrameEye: React.FC = () => (
  <g>
    <defs>
      <linearGradient id="feIris" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#1E0E06" />
        <stop offset="55%" stopColor="#7A3E14" />
        <stop offset="100%" stopColor="#F0B062" />
      </linearGradient>
      <radialGradient id="feFF">
        <stop offset="0%" stopColor="#FFF7D6" />
        <stop offset="40%" stopColor="#FBBF24" />
        <stop offset="100%" stopColor="#FBBF24" stopOpacity={0} />
      </radialGradient>
      <clipPath id="feEye">
        <path d="M80 1000 Q480 560 1010 880 Q560 1260 80 1000 Z" />
      </clipPath>
      <linearGradient id="feSkin" x1="1" y1="0" x2="0" y2="0">
        <stop offset="0%" stopColor="#FFC79A" />
        <stop offset="60%" stopColor="#F3C9A8" />
        <stop offset="100%" stopColor="#C98F7A" />
      </linearGradient>
    </defs>
    <rect width={W} height={H} fill="url(#feSkin)" />
    {Array.from({ length: 9 }, (_, i) => (
      <path
        key={i}
        d={`M${i * 140 - 60} -20 C${i * 140 - 20} 260 ${i * 140 + 10} 420 ${i * 140 + 50 + (i % 2) * 30} ${560 + (i % 3) * 70} L${i * 140 + 150} -20 Z`}
        fill="#2A2050"
        stroke="#2A1A22"
        strokeWidth={4}
      />
    ))}
    <path
      d="M140 690 Q480 520 940 650"
      stroke="#2A1A22"
      strokeWidth={20}
      fill="none"
      strokeLinecap="round"
    />
    <g clipPath="url(#feEye)">
      <rect width={W} height={H} fill="#FBF6F2" />
      <ellipse cx={520} cy={930} rx={250} ry={300} fill="url(#feIris)" />
      <ellipse
        cx={520}
        cy={930}
        rx={250}
        ry={300}
        fill="none"
        stroke="#1B0C05"
        strokeWidth={8}
      />
      <ellipse cx={520} cy={920} rx={100} ry={140} fill="#120703" />
      <circle cx={600} cy={1010} r={130} fill="url(#feFF)" />
      <circle cx={600} cy={1010} r={26} fill="#FFF7D6" />
      <circle cx={430} cy={800} r={58} fill="#FFFFFF" />
      <circle cx={640} cy={1100} r={20} fill="#FFFFFF" />
      <path
        d="M80 1000 Q480 560 1010 880 L1010 760 L80 760 Z"
        fill="#8A6F7E"
        opacity={0.35}
      />
    </g>
    <path
      d="M60 1010 Q480 520 1000 860 L1060 780 L1020 900 Q480 610 90 1030 Z"
      fill="#2A1A22"
    />
    <path
      d="M300 1130 Q620 1200 900 1020"
      stroke="#2A1A22"
      strokeWidth={8}
      fill="none"
      strokeLinecap="round"
    />
    <Sparkle x={760} y={760} s={34} color="#FFF7D6" />
    <Sub text="Every idea gets a few seconds." />
    <Finish />
  </g>
);

export const FrameFade: React.FC = () => (
  <g>
    <TrainDusk dim={0.55} />
    {Array.from({ length: 14 }, (_, i) => (
      <circle
        key={i}
        cx={rand(i + 60) * W}
        cy={rand(i * 4 + 60) * H}
        r={30 + rand(i * 6) * 80}
        fill={i % 2 ? "#FB923C" : "#E26AA8"}
        opacity={0.12}
      />
    ))}
    <circle
      cx={540}
      cy={860}
      r={260}
      fill="none"
      stroke="#FDE68A"
      strokeWidth={6}
      strokeDasharray="420 2000"
      strokeLinecap="round"
      transform="rotate(-90 540 860)"
      opacity={0.7}
    />
    <Firefly x={540} y={860} r={70} life={0.2} />
    {Array.from({ length: 26 }, (_, i) => (
      <rect
        key={i}
        x={470 + rand(i) * 150}
        y={960 + rand(i * 3) * 420}
        width={12}
        height={12}
        fill={i % 4 ? "#64748B" : "#F97316"}
        opacity={0.9 - rand(i * 3) * 0.8}
        transform={`rotate(${rand(i) * 80} ${470 + rand(i) * 150} ${960 + rand(i * 3) * 420})`}
      />
    ))}
    <Sub text="...and most of them fade." />
    <Finish />
  </g>
);

export const FrameWake: React.FC = () => (
  <g>
    <rect width={W} height={H} fill="#0C1225" />
    {[260, 620, 980, 1340].map((y0) => (
      <g key={y0}>
        <rect x={120} y={y0} width={860} height={300} rx={50} fill="#1B2540" />
        <rect
          x={180}
          y={y0 + 70}
          width={420}
          height={36}
          rx={18}
          fill="#2D3A5C"
        />
        <rect
          x={180}
          y={y0 + 150}
          width={600}
          height={30}
          rx={15}
          fill="#24304E"
        />
      </g>
    ))}
    <rect width={W} height={H} fill="#0C1225" opacity={0.55} />
    <circle cx={120} cy={900} r={420} fill="#67E8F9" opacity={0.22} />
    {/* Shru peeks out from behind the dock */}
    <g transform="translate(330 860) scale(2.6)">
      <Head eyes="open" body="#3B82F6" id="wake" />
    </g>
    <defs>
      <radialGradient id="eyewake" cx="50%" cy="65%">
        <stop offset="0%" stopColor="#FEF3C7" />
        <stop offset="60%" stopColor="#FBBF24" />
        <stop offset="100%" stopColor="#B45309" />
      </radialGradient>
    </defs>
    <path
      d="M0 760 L170 760 Q240 760 240 830 L240 970 Q240 1040 170 1040 L0 1040 Z"
      fill="#0F172A"
      stroke="#E0F2FE"
      strokeWidth={6}
    />
    <image href={LOGO} x={30} y={790} width={190} height={190} />
    <Sparkle x={560} y={560} s={40} color="#BAE6FD" />
    <Sparkle x={760} y={760} s={24} color="#FDE68A" />
    <Sparkle x={640} y={1180} s={30} color="#BAE6FD" />
    <Sub text="But something is listening." />
    <Finish />
  </g>
);

export const FrameLeap: React.FC = () => (
  <g>
    <rect width={W} height={H} fill="#140D2E" />
    <SpeedLines
      cx={700}
      cy={620}
      inner={220}
      color="#BAE6FD"
      count={130}
      seed={3}
    />
    <Firefly x={760} y={520} r={44} life={0.5} />
    <g transform="translate(470 1060) rotate(-38)">
      <Shru x={-420} y={-260} s={1.4} pose="leap" eyes="fierce" phase={2} />
    </g>
    <path
      d="M60 1840 C120 1600 260 1460 340 1300"
      stroke="#67E8F9"
      strokeWidth={60}
      fill="none"
      opacity={0.15}
      strokeLinecap="round"
    />
    <path
      d="M0 1500 L150 1500 Q210 1500 210 1560 L210 1680 Q210 1740 150 1740 L0 1740 Z"
      fill="#0F172A"
      stroke="#E0F2FE"
      strokeWidth={6}
    />
    <image href={LOGO} x={20} y={1530} width={170} height={170} />
    <circle
      cx={110}
      cy={1620}
      r={150}
      fill="none"
      stroke="#FFFFFF"
      strokeWidth={8}
      opacity={0.6}
    />
    <circle
      cx={110}
      cy={1620}
      r={220}
      fill="none"
      stroke="#FFFFFF"
      strokeWidth={4}
      opacity={0.3}
    />
    <Sub text="Tap, tap." y={1880} />
    <Finish />
  </g>
);

export const FrameCaught: React.FC = () => (
  <g>
    <defs>
      <radialGradient id="fcBg" cx="50%" cy="45%" r="75%">
        <stop offset="0%" stopColor="#3B2A7A" />
        <stop offset="100%" stopColor="#0E0A24" />
      </radialGradient>
    </defs>
    <rect width={W} height={H} fill="url(#fcBg)" />
    {Array.from({ length: 40 }, (_, i) => (
      <circle
        key={i}
        cx={rand(i + 90) * W}
        cy={rand(i * 2 + 90) * H}
        r={2 + rand(i) * 4}
        fill="#FDE68A"
        opacity={0.3 + rand(i * 5) * 0.6}
      />
    ))}
    <Shru x={170} y={760} s={1.45} pose="sit" eyes="open" phase={0.5} />
    <Firefly x={800} y={760} r={46} life={1} />
    <WordRibbon
      d="M150 560 C260 380 560 360 690 470 C770 540 760 640 790 720"
      text="What if the launch moves to Friday..."
      size={44}
    />
    <WordRibbon
      d="M120 1560 C300 1460 560 1640 960 1500"
      text="...and beta users invite one friend each?"
      size={40}
    />
    <Sub text="Caught. Transcribed on your phone." y={1760} />
    <Finish />
  </g>
);

export const FrameNight: React.FC = () => {
  const stars = [
    [150, 360],
    [330, 260],
    [520, 400],
    [700, 280],
    [900, 380],
    [260, 560],
    [610, 590],
    [860, 560],
  ];
  const links = [
    [0, 1],
    [1, 2],
    [2, 3],
    [3, 4],
    [1, 5],
    [5, 6],
    [2, 6],
    [6, 7],
    [4, 7],
  ];
  return (
    <g>
      <defs>
        <linearGradient id="fnSky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#050714" />
          <stop offset="70%" stopColor="#1E1B4B" />
          <stop offset="100%" stopColor="#4C1D95" />
        </linearGradient>
      </defs>
      <rect width={W} height={H} fill="url(#fnSky)" />
      {Array.from({ length: 80 }, (_, i) => (
        <circle
          key={i}
          cx={rand(i + 5) * W}
          cy={rand(i * 3 + 5) * 900}
          r={1 + rand(i) * 2.5}
          fill="#FFFFFF"
          opacity={0.3 + rand(i * 7) * 0.7}
        />
      ))}
      {links.map(([a, b], i) => (
        <line
          key={i}
          x1={stars[a][0]}
          y1={stars[a][1]}
          x2={stars[b][0]}
          y2={stars[b][1]}
          stroke="#A78BFA"
          strokeWidth={3}
          opacity={0.8}
        />
      ))}
      {stars.map(([x, y], i) => (
        <Firefly key={i} x={x} y={y} r={14} life={1} />
      ))}
      {Array.from({ length: 16 }, (_, i) => (
        <rect
          key={i}
          x={i * 70 - 20}
          y={860 - rand(i + 30) * 220}
          width={64}
          height={500}
          fill="#0C1225"
        />
      ))}
      {Array.from({ length: 50 }, (_, i) => (
        <rect
          key={i}
          x={rand(i + 77) * W}
          y={700 + rand(i * 2 + 77) * 360}
          width={8}
          height={10}
          fill="#FDE68A"
          opacity={0.6}
        />
      ))}
      <rect
        x={0}
        y={0}
        width={W}
        height={H}
        fill="none"
        stroke="#120C26"
        strokeWidth={60}
      />
      <rect x={0} y={1080} width={W} height={60} fill="#120C26" />
      <Meera x={300} y={980} s={1.0} expr="smile" light="night" />
      <rect x={0} y={1640} width={W} height={280} fill="#1A1433" />
      <rect x={60} y={1600} width={420} height={70} rx={14} fill="#0B0F1A" />
      <Shru x={70} y={1420} s={0.62} pose="curl" />
      <Sub text="Every thought, kept." y={1840} />
      <Finish />
    </g>
  );
};

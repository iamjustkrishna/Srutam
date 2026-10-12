// Idea 1, "Knock Knock": the promises we make out loud, and the app's
// knock-knock reminder sound as the hook and the payoff.
import { MeeraSketch } from "../sketch/MeeraSketch";
import {
  Bubble,
  Burst,
  Caption,
  Door,
  EndCard,
  Frame,
  GripHand,
  H,
  InsightsScreen,
  LINE,
  LockNotifs,
  Phone,
  Sfx,
  W,
} from "./Kit";
import { Person } from "./People";

export const MA = { hair: "nani", outfit: "saree", hairColor: "#5A4A45", skin: "#D9A07C", cloth: "#0F766E", accent: "#FDE68A", bindi: true, aged: true } as const;

export const Sofa: React.FC<{ y: number }> = ({ y }) => (
  <g>
    <rect x={-40} y={y} width={W + 80} height={420} rx={60} fill="#7C3AED" stroke={LINE} strokeWidth={6} opacity={0.85} />
    <rect x={-40} y={y + 160} width={W + 80} height={H} fill="#6D28D9" stroke={LINE} strokeWidth={6} />
  </g>
);

export const Street: React.FC = () => (
  <g>
    {[0, 1, 2, 3, 4].map((i) => (
      <rect key={i} x={i * 230 - 20} y={520 + (i % 2) * 120} width={210} height={900} fill={i % 2 ? "#3B2F5C" : "#4C3B6E"} />
    ))}
    {Array.from({ length: 18 }, (_, i) => (
      <rect key={i} x={30 + (i % 6) * 180} y={600 + Math.floor(i / 6) * 160} width={40} height={60} fill="#FCD34D" opacity={0.7} />
    ))}
    {/* auto-rickshaw */}
    <g transform="translate(700 1180)">
      <path d="M0 120 C0 20 60 -20 160 -20 L260 -20 L280 120 Z" fill="#FACC15" stroke={LINE} strokeWidth={6} />
      <path d="M0 120 L300 120 L300 170 L0 170 Z" fill="#15803D" stroke={LINE} strokeWidth={6} />
      <circle cx={60} cy={180} r={36} fill="#1F2937" />
      <circle cx={250} cy={180} r={36} fill="#1F2937" />
    </g>
  </g>
);

export const K1: React.FC = () => (
  <Frame bg={["#120A08", "#2B1A12"]} night>
    <Door x={150} y={240} w={780} h={1700} />
    <Burst x={540} y={900} r={240} color="#FBBF24" n={12} />
    <Sfx x={540} y={860} text="knock" size={190} rot={-10} />
    <Sfx x={560} y={1060} text="knock!" size={190} rot={6} />
  </Frame>
);

export const K2: React.FC = () => (
  <Frame bg={["#F59E0B", "#7C3AED"]}>
    <Street />
    <MeeraSketch x={110} y={720} s={1.45} expr="laugh" turn={0.2} />
    <g transform="translate(720 980) rotate(16)">
      <rect x={0} y={0} width={120} height={250} rx={22} fill="#0B0F1A" stroke="#3A4154" strokeWidth={4} />
    </g>
    <GripHand x={760} y={1260} s={0.9} rot={-14} />
    <Bubble x={90} y={260} w={900} lines={["Haan Ma, I'll call you", "on Sunday. Promise!"]} tail={[560, 760]} size={64} />
  </Frame>
);

export const K3: React.FC = () => (
  <Frame bg={["#1E1B4B", "#0F172A"]} night>
    {/* Top: Ma waiting by a silent phone */}
    <defs>
      <clipPath id="k3top">
        <path d={`M0 0 L${W} 0 L${W} 860 L0 1000 Z`} />
      </clipPath>
    </defs>
    <g clipPath="url(#k3top)">
      <rect width={W} height={1000} fill="#FDE7C7" />
      <Person {...MA} x={330} y={180} s={1.05} expr="worried" glasses gaze={[-0.6, 0.6]} />
      <rect x={40} y={800} width={300} height={40} fill="#A9744D" stroke={LINE} strokeWidth={5} />
      <g transform="translate(110 690) rotate(-8)">
        <rect width={110} height={110} rx={18} fill="#0B0F1A" stroke="#3A4154" strokeWidth={4} />
        <rect x={12} y={12} width={86} height={86} rx={10} fill="#1F2937" />
      </g>
      <Bubble x={40} y={150} w={360} lines={["Sunday…", "no call?"]} tail={[330, 420]} size={48} think />
    </g>
    <path d={`M0 1000 L${W} 860`} stroke="#FFFDF8" strokeWidth={14} />
    {/* Bottom: Meera, TV glow, popcorn, phone face down */}
    <rect x={0} y={1000} width={W} height={920} fill="#60A5FA" opacity={0.15} />
    <Sofa y={1500} />
    <MeeraSketch x={250} y={1030} s={1.0} expr="laugh" turn={-0.3} />
    <g transform="translate(800 1720)">
      <path d="M-120 0 C-110 90 110 90 120 0 Z" fill="#DC2626" stroke={LINE} strokeWidth={5} />
      {Array.from({ length: 9 }, (_, i) => (
        <circle key={i} cx={-90 + i * 22} cy={-10 - (i % 3) * 16} r={20} fill="#FEF3C7" stroke={LINE} strokeWidth={2} />
      ))}
    </g>
    <Caption text="Sunday, 9:40 pm" y={1040} />
  </Frame>
);

export const K4: React.FC = () => (
  <Frame bg={["#FDE7C7", "#F3D2A8"]}>
    <MeeraSketch x={-170} y={880} s={1.3} expr="smug" turn={0.3} gaze={[0.6, 0.3]} />
    <Phone x={540} y={640} w={500} rot={7}>
      <InsightsScreen
        w={500}
        note="Call Ma"
        meta="Today, 8:12 pm · 0:04"
        items={[{ kind: "REMINDER", text: "Call Ma", meta: "Sunday, 7:00 pm" }]}
      />
    </Phone>
    <GripHand x={790} y={1820} s={1.2} rot={-4} />
    <Bubble x={40} y={200} w={600} lines={["Call Ma,", "Sunday at 7."]} tail={[230, 1000]} size={66} />
  </Frame>
);

export const K5: React.FC = () => (
  <Frame bg={["#1E1B4B", "#312E81"]} night>
    <Sofa y={1500} />
    <MeeraSketch x={-60} y={1000} s={1.15} expr="surprised" turn={0.35} gaze={[0.9, 0.2]} />
    <Phone x={470} y={560} w={560} rot={-4} glow="#FBBF24">
      <LockNotifs
        w={560}
        date="Sunday, 10 March"
        time="6:45"
        notifs={[{ title: "Call Ma", text: "Starts in 15 minutes.", reminder: true }]}
      />
    </Phone>
    <Burst x={750} y={1180} r={700} color="#FBBF24" n={14} />
    <Sfx x={540} y={380} text="knock knock!" size={150} rot={-6} />
  </Frame>
);

export const K6: React.FC = () => (
  <Frame bg={["#FDE7C7", "#0B0F26"]}>
    <rect x={0} y={1230} width={W} height={690} fill="#050714" />
    <MeeraSketch x={-30} y={420} s={1.0} expr="laugh" turn={0.3} />
    <Phone x={600} y={200} w={380} rot={5}>
      <div style={{ position: "absolute", inset: 0, background: "#FDE7C7" }}>
        <svg viewBox="0 0 600 800" width="100%" style={{ position: "absolute", bottom: 0 }}>
          <Person {...MA} x={-90} y={-80} s={1.3} expr="laugh" glasses />
        </svg>
      </div>
    </Phone>
    <Bubble x={540} y={60} w={500} lines={["You remembered!"]} tail={[760, 260]} size={56} />
    <g transform="translate(0 520)">
      <EndCard tagline={["Say it once.", "Srutam knocks."]} />
    </g>
  </Frame>
);

export const KNOCK = {
  num: 1,
  title: "Knock Knock",
  logline:
    "We make promises out loud all day and forget them by dinner. Say it once to Srutam, and it knocks at exactly the right moment. The app's own knock-knock becomes the brand sound.",
  audience: "Everyone with a family group chat",
  features: ["Reminders from speech", "15-min heads-up", "Mark Done / Snooze", "knock-knock sound"],
  length: "30 s",
  beats: [
    { t: "0–2 s", title: "Hook", vo: "Knock knock.", sfx: "The app's knock-knock over black", Panel: K1 },
    { t: "2–7 s", title: "The promise", vo: "“Haan Ma, I'll call you on Sunday. Promise!”", sfx: "Street, auto horn", Panel: K2 },
    { t: "7–12 s", title: "Life happens", vo: "We promise out loud… then life happens.", sfx: "TV laughter; Ma's silent phone", Panel: K3 },
    { t: "12–18 s", title: "Say it once", vo: "She taps Start on Srutam's notification: “Call Ma, Sunday at 7.”", sfx: "Reminder card pops: Sunday, 7:00 pm", Panel: K4 },
    { t: "18–25 s", title: "Knock knock", vo: "Srutam remembers for you.", sfx: "knock-knock · “Starts in 15 minutes.”", Panel: K5 },
    { t: "25–30 s", title: "Payoff + end card", vo: "“You remembered!” · Say it once. Srutam knocks.", sfx: "Warm chord, logo", Panel: K6 },
  ],
} as const;

// Idea 4, "2 a.m. Before the Exam": the one line from the lecture that
// mattered, found in seconds with Ask Srutam AI.
import { MeeraSketch } from "../sketch/MeeraSketch";
import {
  AskScreen,
  Board,
  Books,
  Bubble,
  Caption,
  Clock,
  EndBand,
  Frame,
  GripHand,
  InsightsScreen,
  Lamp,
  LINE,
  LockNotifs,
  Phone,
  Sfx,
  Table,
  W,
} from "./Kit";
import { Person } from "./People";

const PROF = { hair: "curly", outfit: "kurta", hairColor: "#9CA3AF", skin: "#C98B62", cloth: "#E7E5E4", accent: "#92400E", glasses: true, aged: true } as const;
const RIYA = { hair: "ponytail", outfit: "hoodie", hairColor: "#1F1512", skin: "#D9A07C", cloth: "#EC4899", accent: "#FDF2F8" } as const;
const KABIR = { hair: "curly", outfit: "hoodie", hairColor: "#1F1512", skin: "#B87A55", cloth: "#16A34A", accent: "#ECFDF5" } as const;

export const E1: React.FC = () => (
  <Frame bg={["#0B1033", "#1E1B4B"]} night>
    <Lamp x={800} y={820} />
    <MeeraSketch x={60} y={760} s={1.25} expr="worried" turn={0.1} gaze={[0.2, -0.6]} />
    <Table y={1640} color="#3F2A1D" top="#5B3A22" />
    <Books x={830} y={1660} n={6} />
    {[0, 1, 2].map((i) => (
      <rect key={i} x={60 + i * 150} y={1700 + (i % 2) * 30} width={140} height={110} fill="#FFFDF8" stroke={LINE} strokeWidth={3} transform={`rotate(${i * 9 - 9} ${130 + i * 150} ${1750})`} />
    ))}
    <Clock x={300} y={330} text="2:07 AM" size={80} />
    <Bubble x={420} y={520} w={620} lines={["Which chapters", "did Prof say?!"]} tail={[520, 1050]} size={60} />
    <Caption text="Exam in 7 hours." y={60} />
  </Frame>
);

export const E2: React.FC = () => (
  <Frame bg={["#E9D8B4", "#C9AE82"]}>
    <Board x={60} y={250} w={960} h={520} chalk lines={["Exam:", "Ch. 4, Ch. 7", "+ metro case study"]} />
    <Person {...PROF} x={300} y={640} s={0.95} expr="talk" />
    <Table y={1380} color="#7C5134" top="#A9744D" />
    <Person {...KABIR} x={-120} y={1180} s={0.7} expr="sleepy" />
    <Phone x={600} y={1250} w={360} rot={-8}>
      <LockNotifs w={360} date="Monday" time="9:31" notifs={[{ title: "Recording", text: "31:42", recording: true }]} />
    </Phone>
    <rect x={0} y={0} width={W} height={1920} fill="#B45309" opacity={0.1} />
    <Caption text="Monday, 9 am. Half the class asleep." y={40} />
  </Frame>
);

export const E3: React.FC = () => (
  <Frame bg={["#0B1033", "#050714"]} night>
    <Phone x={150} y={250} w={700} glow="#3B82F6">
      <AskScreen
        w={700}
        q="What did Prof say will be on the exam?"
        a="Chapters 4 and 7, plus the metro case study. He said the case study alone is worth 20 marks."
        refs={["Econ lecture · 52 min"]}
      />
    </Phone>
    <GripHand x={500} y={1820} s={1.4} rot={-2} />
  </Frame>
);

export const E4: React.FC = () => (
  <Frame bg={["#1E1B4B", "#0B1033"]} night>
    {/* Open door: lit corridor, the door leaf swung to the right */}
    <rect x={120} y={260} width={700} height={1660} fill="#FDE68A" stroke={LINE} strokeWidth={8} />
    <path d="M820 260 L1010 200 L1010 1920 L820 1920 Z" fill="#7C5134" stroke={LINE} strokeWidth={8} />
    <circle cx={850} cy={1100} r={14} fill="#FBBF24" stroke={LINE} strokeWidth={4} />
    <Person {...RIYA} x={80} y={640} s={0.72} expr="surprised" />
    <Person {...KABIR} x={400} y={680} s={0.72} expr="talk" flip />
    <Sfx x={560} y={210} text="knock knock" size={110} rot={-4} />
    <MeeraSketch x={-230} y={1180} s={1.05} expr="smug" turn={0.4} gaze={[0.8, -0.4]} />
    <Bubble x={460} y={330} w={560} lines={["Did you", "record it?!"]} tail={[560, 760]} size={56} />
    <Caption text="2:15 am" y={60} />
  </Frame>
);

export const E5: React.FC = () => (
  <Frame bg={["#1E1B4B", "#0B1033"]} night>
    <Phone x={210} y={120} w={620} glow="#FBBF24">
      <InsightsScreen
        w={620}
        note="Econ lecture"
        meta="Mon, 9:00 am · 52:10"
        items={[
          { kind: "KEY POINT", text: "Ch. 4: why metro fares rose 12%" },
          { kind: "KEY POINT", text: "Ch. 7: public goods" },
          { kind: "NEXT STEP", text: "Revise the metro case study", meta: "20 marks" },
        ]}
      />
    </Phone>
    <Person {...RIYA} x={-140} y={1300} s={0.78} expr="laugh" />
    <MeeraSketch x={250} y={1240} s={0.85} expr="laugh" />
    <Person {...KABIR} x={680} y={1300} s={0.78} expr="laugh" flip />
    <Caption text="The whole floor, one recording." y={1760} />
  </Frame>
);

export const E6: React.FC = () => (
  <Frame bg={["#FEF9C3", "#FDE68A"]}>
    <MeeraSketch x={140} y={330} s={1.3} expr="smug" turn={0.1} gaze={[0, 0.8]} />
    <g transform="translate(200 1250) rotate(-3)">
      <rect width={680} height={420} fill="#FFFDF8" stroke={LINE} strokeWidth={5} />
      <text x={40} y={90} fontFamily="Kalam" fontWeight={700} fontSize={44} fill="#1F2937">
        <tspan x={40}>Q1. Explain Chapter 4…</tspan>
        <tspan x={40} dy={70}>Q2. Chapter 7: public…</tspan>
        <tspan x={40} dy={70}>Q3. Metro case study</tspan>
        <tspan x={40} dy={70}>      (20 marks)</tspan>
      </text>
    </g>
    <EndBand tagline="Listen once. Ask anytime." y={1620} />
  </Frame>
);

export const EXAM = {
  num: 4,
  title: "2 a.m. Before the Exam",
  logline:
    "It's 2 a.m., the exam is at 9, and nobody remembers what Prof said would come. Meera recorded the lecture. One question to Srutam, and the whole hostel floor knows.",
  audience: "College students · exam season",
  features: ["Recording notification", "Long lectures", "Ask Srutam AI", "Key Points", "Next Steps"],
  length: "30 s",
  beats: [
    { t: "0–3 s", title: "Hook", vo: "Exam in 7 hours. “Which chapters did Prof say?!”", sfx: "Clock tick, panicked page flips", Panel: E1 },
    { t: "3–8 s", title: "Flashback", vo: "Monday, 9 am. Half the class asleep. Srutam wasn't.", sfx: "Chalk, distant yawn, rewind whir", Panel: E2 },
    { t: "8–14 s", title: "Ask", vo: "“What did Prof say will be on the exam?”", sfx: "Answer types in · Referenced Notes pill", Panel: E3 },
    { t: "14–20 s", title: "Knock knock", vo: "Riya & Kabir: “Did you record it?!”", sfx: "Real door knock (echoes the app sound)", Panel: E4 },
    { t: "20–26 s", title: "Whole floor", vo: "The whole floor, one recording.", sfx: "Laughter, pages, chai glasses", Panel: E5 },
    { t: "26–30 s", title: "Payoff + end card", vo: "Q3: Metro case study. (smug) · Listen once. Ask anytime.", sfx: "Bell, logo", Panel: E6 },
  ],
} as const;

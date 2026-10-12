// Idea 3, "Who Said That?": a meeting where everyone agreed and nobody wrote
// it down, except Meera's floating dock.
import { MeeraSketch } from "../sketch/MeeraSketch";
import {
  AskScreen,
  Board,
  Bubble,
  Caption,
  DockOverApp,
  EndBand,
  Frame,
  GripHand,
  InsightsScreen,
  LINE,
  Phone,
  Sweat,
  Table,
  W,
} from "./Kit";
import { Person } from "./People";

const RAO = { hair: "bald", outfit: "blazer", hairColor: "#3B2F2F", skin: "#C98B62", cloth: "#BFDBFE", glasses: true, beard: true } as const;
const ARJUN = { hair: "short", outfit: "shirt", hairColor: "#1F1512", skin: "#B87A55", cloth: "#2563EB" } as const;
const NEHA = { hair: "ponytail", outfit: "shirt", hairColor: "#3A2418", skin: "#E3A982", cloth: "#D97706" } as const;

const Office: React.FC = () => (
  <g>
    <rect x={0} y={0} width={W} height={1920} fill="#DCE3EC" />
    <Board x={120} y={170} w={840} h={520} lines={["Q3 launch?", "pricing → ??", "14th or 21st"]} />
    {[0, 1, 2].map((i) => (
      <rect key={i} x={740 + (i % 2) * 90} y={260 + i * 110} width={80} height={80} fill={["#FDE68A", "#FBCFE8", "#BBF7D0"][i]} transform={`rotate(${i * 7 - 6} ${780} ${300 + i * 110})`} />
    ))}
  </g>
);

export const M1: React.FC = () => (
  <Frame bg={["#DCE3EC", "#C7D2E0"]}>
    <Office />
    <Person {...ARJUN} x={-110} y={900} s={0.95} expr="smile" />
    <Person {...NEHA} x={590} y={900} s={0.95} expr="smile" flip />
    <Person {...RAO} x={225} y={760} s={1.05} expr="laugh" />
    <Table y={1560} color="#6B4F3A" top="#8B6B52" />
    <Bubble x={260} y={710} w={620} lines={["Great. So we're", "all aligned?"]} tail={[540, 1010]} size={56} />
    <Caption text="Tuesday, 11 am" y={1650} />
  </Frame>
);

export const M2: React.FC = () => {
  const cell = (cx: number, cy: number, child: React.ReactNode, key: number) => (
    <g key={key}>
      <clipPath id={`m2c${key}`}>
        <rect x={cx} y={cy} width={540} height={760} />
      </clipPath>
      <rect x={cx} y={cy} width={540} height={760} fill={["#E0E7FF", "#FCE7F3", "#DCFCE7", "#FEF3C7"][key]} />
      <g clipPath={`url(#m2c${key})`}>{child}</g>
    </g>
  );
  return (
    <Frame bg={["#0F172A", "#0F172A"]}>
      {cell(0, 200, <Person {...ARJUN} x={-20} y={300} s={0.95} expr="sideeye" />, 0)}
      {cell(540, 200, <Person {...NEHA} x={540} y={300} s={0.95} expr="sideeye" flip />, 1)}
      {cell(0, 960, <Person {...RAO} x={-20} y={1060} s={0.95} expr="sideeye" />, 2)}
      {cell(540, 960, <MeeraSketch x={520} y={1040} s={0.95} expr="smug" turn={0.3} gaze={[0.7, 0]} />, 3)}
      <path d={`M540 200 L540 1720 M0 960 L${W} 960`} stroke="#0F172A" strokeWidth={16} />
      <Caption text="…so who's writing this down?" y={40} />
      <Caption text="(Meera knows.)" y={1740} />
    </Frame>
  );
};

export const M3: React.FC = () => (
  <Frame bg={["#C7D2E0", "#8B6B52"]}>
    <Table y={980} color="#6B4F3A" top="#8B6B52" />
    <Phone x={230} y={300} w={600} rot={-6}>
      <DockOverApp w={600} recording label="Team chat" />
    </Phone>
    {/* Index finger tapping the dock */}
    <g transform="translate(250 930) rotate(28)">
      <rect x={-60} y={300} width={170} height={500} rx={30} fill="#EFE6D8" stroke={LINE} strokeWidth={5} />
      <rect x={-40} y={0} width={86} height={420} rx={43} fill="#F2C3A2" stroke={LINE} strokeWidth={5} />
      <rect x={-26} y={12} width={58} height={64} rx={24} fill="#FBE1CC" stroke={LINE} strokeWidth={3} />
      <path d="M-40 160 q43 14 86 0 M-40 250 q43 14 86 0" stroke={LINE} strokeWidth={3} fill="none" opacity={0.6} />
    </g>
    <circle cx={250} cy={900} r={70} fill="none" stroke="#FFFFFF" strokeWidth={8} opacity={0.8} />
    <Caption text="Meera tapped the dock at minute one." y={60} />
    <Caption text="It even works over other apps." y={1700} />
  </Frame>
);

export const M4: React.FC = () => (
  <Frame bg={["#0F172A", "#1E293B"]} night>
    <Phone x={150} y={200} w={700} glow="#3B82F6">
      <InsightsScreen
        w={700}
        note="Tuesday sync"
        meta="Today, 11:02 am · 32:14"
        items={[
          { kind: "DECISION", text: "Launch moves to the 14th" },
          { kind: "NEXT STEP", text: "Arjun sends the pricing deck", meta: "By Thursday" },
          { kind: "REMINDER", text: "Pricing deck to Mr. Rao", meta: "Thursday, 10:00 am" },
        ]}
      />
    </Phone>
  </Frame>
);

export const M5: React.FC = () => (
  <Frame bg={["#FEE2E2", "#FCA5A5"]}>
    {[...Array(12)].map((_, i) => (
      <path key={i} d={`M540 760 L${540 + Math.cos((i / 12) * 6.28) * 1400} ${760 + Math.sin((i / 12) * 6.28) * 1400}`} stroke="#F87171" strokeWidth={60} opacity={0.35} />
    ))}
    <Person {...RAO} x={140} y={360} s={1.3} expr="angry" />
    {/* comic anger mark */}
    <g transform="translate(760 660)" stroke="#DC2626" strokeWidth={14} strokeLinecap="round" fill="none">
      <path d="M-40 -10 Q-10 -10 -10 -40 M40 -10 Q10 -10 10 -40 M-40 10 Q-10 10 -10 40 M40 10 Q10 10 10 40" />
    </g>
    <Bubble x={60} y={80} w={960} lines={["Who agreed to the 14th?!"]} tail={[540, 420]} size={70} />
    <Person {...ARJUN} x={-150} y={1330} s={0.75} expr="worried" gaze={[1, 0]} />
    <Person {...NEHA} x={780} y={1330} s={0.75} expr="worried" gaze={[1, 0]} flip />
    <Sweat x={150} y={1500} />
    <Sweat x={880} y={1500} />
    <Bubble x={40} y={1180} w={260} lines={["Her!"]} tail={[300, 1400]} size={56} />
    <Bubble x={780} y={1180} w={260} lines={["Him!"]} tail={[780, 1400]} size={56} />
    <Caption text="One week later" y={1760} />
  </Frame>
);

export const M6: React.FC = () => (
  <Frame bg={["#DCE3EC", "#C7D2E0"]}>
    <Person {...RAO} x={590} y={660} s={0.8} expr="worried" gaze={[-0.8, 0]} />
    <Sweat x={980} y={880} />
    <MeeraSketch x={-170} y={560} s={1.0} expr="smug" turn={0.3} gaze={[0.6, 0.2]} />
    <Phone x={250} y={150} w={440} rot={-5}>
      <AskScreen
        w={440}
        q="Who decided on the 14th?"
        a="In Tuesday's sync, Mr. Rao said: “Fine, let's go with the 14th.”"
        refs={["Tuesday sync"]}
      />
    </Phone>
    <GripHand x={480} y={1170} s={1} rot={-5} />
    <EndBand tagline="Every meeting, minuted." />
  </Frame>
);

export const MEETING = {
  num: 3,
  title: "Who Said That?",
  logline:
    "Everyone agreed in the meeting. Nobody wrote it down. A week later, nobody remembers who agreed to what, except Meera, whose Srutam dock was listening.",
  audience: "Office teams · LinkedIn and Instagram work humour",
  features: ["Floating dock over any app", "Decisions", "Next Steps", "Reminders", "Ask with Referenced Notes"],
  length: "30 s",
  beats: [
    { t: "0–4 s", title: "Hook", vo: "Mr. Rao: “Great. So we're all aligned?” Everyone nods.", sfx: "Office hum, marker squeak", Panel: M1 },
    { t: "4–8 s", title: "The side-eye", vo: "…so who's writing this down?", sfx: "Comic side-eye whoosh ×3", Panel: M2 },
    { t: "8–12 s", title: "The dock", vo: "Meera tapped the dock at minute one.", sfx: "tap · soft record blip", Panel: M3 },
    { t: "12–18 s", title: "Sorted", vo: "Decision, next step, reminder. Before lunch.", sfx: "Cards pop in one by one", Panel: M4 },
    { t: "18–25 s", title: "One week later", vo: "“Who agreed to the 14th?!” “Her!” “Him!”", sfx: "Record scratch, dramatic sting", Panel: M5 },
    { t: "25–30 s", title: "Payoff + end card", vo: "Meera: “Let's ask.” · Every meeting, minuted.", sfx: "Answer pops · office laughter · logo", Panel: M6 },
  ],
} as const;

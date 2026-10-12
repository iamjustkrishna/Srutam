// Idea 2, "Nani's Laddoos": the recipe that was never written down, kept in
// Nani's own voice and found again with Ask Srutam AI.
import { MeeraSketch } from "../sketch/MeeraSketch";
import {
  AskScreen,
  Bubble,
  Caption,
  Diya,
  EndBand,
  Frame,
  GripHand,
  H,
  Jar,
  Kadhai,
  Laddoos,
  LINE,
  Phone,
  PlaybackScreen,
  NotificationShade,
  Table,
  W,
  Window,
} from "./Kit";
import { Person } from "./People";

export const NANI = {
  hair: "nani",
  outfit: "saree",
  hairColor: "#CFC8C2",
  skin: "#D9A07C",
  cloth: "#9D174D",
  accent: "#FDE68A",
  bindi: true,
  aged: true,
  glasses: true,
} as const;

export const Shelf: React.FC = () => (
  <g>
    <rect x={0} y={640} width={W} height={26} fill="#8B5E3C" stroke={LINE} strokeWidth={5} />
    <Jar x={150} y={640} label="Gud" fill="#B7791F" />
    <Jar x={340} y={640} label="Besan" fill="#F6D58E" s={0.9} />
    <Jar x={890} y={640} label="Ghee" fill="#FDE68A" s={0.85} />
  </g>
);

export const N1: React.FC = () => (
  <Frame bg={["#FCD9A8", "#E9A86B"]}>
    <Window x={600} y={200} w={360} h={380} sky={["#FEF9C3", "#FDE68A"]} />
    <Shelf />
    <Person {...NANI} x={150} y={700} s={1.3} expr="laugh" />
    <Table y={1650} color="#7C5134" top="#A9744D" />
    <Kadhai x={560} y={1690} s={1.3} />
    <Caption text="Nani never measured anything." y={60} />
    <Bubble x={430} y={720} w={620} lines={["Measure? Beta,", "you go by smell!"]} tail={[600, 1250]} size={56} />
  </Frame>
);

export const N2: React.FC = () => (
  <Frame bg={["#FCD9A8", "#E9A86B"]}>
    <g opacity={0.45}>
      <Person {...NANI} x={560} y={560} s={0.9} expr="talk" />
      <Kadhai x={760} y={1260} s={0.8} />
    </g>
    <Phone x={170} y={420} w={620} rot={-4}>
      <NotificationShade w={620} recording secs={4} />
    </Phone>
    <GripHand x={470} y={1780} s={1.4} rot={2} />
    <Caption text="So one Sunday, I let Srutam listen." y={60} />
  </Frame>
);

export const N3: React.FC = () => (
  <Frame bg={["#1E1B4B", "#312E81"]} night>
    <Window x={120} y={220} w={840} h={620} sky={["#0B1033", "#1E3A8A"]} moon />
    {Array.from({ length: 20 }, (_, i) => (
      <circle key={i} cx={160 + (i % 10) * 82} cy={380 + Math.floor(i / 10) * 260 + (i % 3) * 30} r={9} fill="#FCD34D" opacity={0.8} />
    ))}
    <rect x={80} y={840} width={920} height={30} fill="#A9744D" stroke={LINE} strokeWidth={5} />
    <Diya x={240} y={840} s={0.9} />
    <Diya x={540} y={840} s={0.9} />
    <Diya x={840} y={840} s={0.9} />
    <MeeraSketch x={90} y={980} s={1.25} expr="worried" turn={0.15} gaze={[0.3, 0.8]} />
    <Table y={1720} color="#5B3A22" top="#7C5134" />
    <Kadhai x={760} y={1760} s={1.1} steam={false} />
    <Bubble x={470} y={930} w={540} lines={["How much", "gud?!"]} tail={[330, 1160]} size={62} think />
    <Caption text="Diwali. 1,400 km from Nani's kitchen." y={60} />
  </Frame>
);

export const N4: React.FC = () => (
  <Frame bg={["#1E1B4B", "#0B0F26"]} night>
    <Phone x={150} y={250} w={700} rot={-2} glow="#3B82F6">
      <AskScreen
        w={700}
        q="How much jaggery did Nani put in the laddoos?"
        a="About one fistful of gud for every cup of besan, roasted “until it smells like ghee and home.”"
        refs={["Nani's kitchen · 14 min"]}
      />
    </Phone>
    <GripHand x={500} y={1820} s={1.4} rot={-2} />
  </Frame>
);

export const N5: React.FC = () => (
  <Frame bg={["#3B2A5E", "#1E1B4B"]} night>
    <rect x={0} y={0} width={W} height={H} fill="#FBBF24" opacity={0.12} />
    <MeeraSketch x={-30} y={760} s={1.25} expr="laugh" turn={0.25} />
    {/* a happy tear */}
    <path d="M300 1260 C312 1282 312 1298 300 1304 C288 1298 288 1282 300 1260 Z" fill="#BFDBFE" stroke={LINE} strokeWidth={3} />
    <Table y={1640} color="#5B3A22" top="#7C5134" />
    <Kadhai x={340} y={1690} s={1.0} />
    <Phone x={680} y={980} w={330} rot={-6}>
      <PlaybackScreen w={330} title="Nani's kitchen" meta="3 Mar · 14:02" quote="…aur thoda sa pyaar. Bas." />
    </Phone>
    <Bubble x={400} y={170} w={640} lines={["“…and a little love.", "That's it.”"]} tail={[840, 980]} size={54} />
  </Frame>
);

export const N6: React.FC = () => (
  <Frame bg={["#FDE7C7", "#F3D2A8"]}>
    <Phone x={470} y={170} w={540} rot={4}>
      <div style={{ position: "absolute", inset: 0, background: "#FCD9A8" }}>
        <svg viewBox="0 0 600 800" width="100%" style={{ position: "absolute", bottom: 0 }}>
          <Person {...NANI} x={-90} y={-60} s={1.3} expr="laugh" />
        </svg>
      </div>
    </Phone>
    <MeeraSketch x={-110} y={620} s={1.05} expr="laugh" turn={0.3} />
    <Laddoos x={330} y={1380} s={0.9} />
    <Bubble x={40} y={130} w={520} lines={["Bilkul mere", "jaise!"]} tail={[520, 520]} size={58} />
    <EndBand tagline="Keep the recipe. Keep the voice." />
  </Frame>
);

export const NANI_IDEA = {
  num: 2,
  title: "Nani's Laddoos",
  logline:
    "Nani never measured anything, so the recipe only ever lived in her voice. One Sunday Meera let Srutam listen in the kitchen. Diwali, 1,400 km away, she asks for it back and hears Nani say it.",
  audience: "Diwali / festive season · anyone who moved away from home",
  features: ["Recording notification", "Ask Srutam AI", "Referenced Notes", "Playback of the original voice"],
  length: "40 s",
  beats: [
    { t: "0–4 s", title: "Hook", vo: "“Measure? Beta, you go by smell!”", sfx: "Sizzling ghee, steel spoon on kadhai", Panel: N1 },
    { t: "4–9 s", title: "Let it listen", vo: "So one Sunday, I let Srutam listen.", sfx: "Pull down the shade · tap Start on Srutam's notification", Panel: N2 },
    { t: "9–15 s", title: "Far from home", vo: "Diwali. 1,400 km from Nani's kitchen. How much gud?!", sfx: "Distant crackers, quiet flat", Panel: N3 },
    { t: "15–22 s", title: "Ask", vo: "“How much jaggery did Nani put in the laddoos?”", sfx: "Answer types in · Referenced Notes pill", Panel: N4 },
    { t: "22–30 s", title: "Her voice", vo: "Nani (recording): “…aur thoda sa pyaar. Bas.”", sfx: "Nani's real voice from the note · music swells", Panel: N5 },
    { t: "30–40 s", title: "Payoff + end card", vo: "Nani on video call: “Bilkul mere jaise!” · Keep the recipe. Keep the voice.", sfx: "Warm sitar-and-strings resolve, logo", Panel: N6 },
  ],
} as const;

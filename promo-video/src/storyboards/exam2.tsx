// Storyboard v2, "2 a.m. Before the Exam": the one line from the lecture that
// mattered, found with Ask Srutam AI, staged with the rigged cast.
import { Kulhad, Zzz } from "../bedtime/Props";
import { Actor } from "../cast/Rig";
import { KABIR, PROF, RIYA } from "../cast/cast";
import {
  AskScreen,
  Board,
  Books,
  Bubble,
  Caption,
  Clock,
  EndBand,
  Frame,
  H,
  InsightsScreen,
  Lamp,
  LINE,
  LockNotifs,
  Phone,
  Sfx,
  Table,
  W,
} from "./Kit";
import { Act, NotationDefs, Pan, Push, Shot } from "./Notation";
import type { ProIdea } from "./ProBoard";

const Hostel: React.FC = () => (
  <g>
    <rect width={W} height={H} fill="#1E1B4B" />
    <rect x={80} y={160} width={420} height={520} fill="#0B1033" stroke={LINE} strokeWidth={8} />
    <circle cx={400} cy={270} r={44} fill="#FEF3C7" />
    {[0, 1, 2, 3, 4].map((i) => (
      <rect key={i} x={640 + (i % 3) * 120} y={220 + Math.floor(i / 3) * 160} width={90} height={120} rx={8} fill={["#F472B6", "#60A5FA", "#FBBF24", "#34D399", "#A78BFA"][i]} opacity={0.5} transform={`rotate(${i * 5 - 8} ${680 + (i % 3) * 120} ${280})`} />
    ))}
  </g>
);

/** A sepia ripple: the flashback dissolve. */
const Ripple: React.FC = () => (
  <g fill="none" stroke="#FDE68A" strokeWidth={6} opacity={0.5}>
    {[0, 1, 2, 3].map((i) => (
      <ellipse key={i} cx={540} cy={960} rx={300 + i * 140} ry={180 + i * 90} />
    ))}
  </g>
);

export const X1: React.FC = () => (
  <Frame bg={["#0B1033", "#1E1B4B"]} night>
    <NotationDefs />
    <Hostel />
    <Lamp x={780} y={760} />
    <Actor x={-40} y={760} s={1.15} who="meera" expr="worried" gaze={[0.2, -0.7]} tilt={-3} arms={{ l: { to: [215, 345], pose: "open", rot: 20 } }} />
    <Table y={1640} color="#3F2A1D" top="#5B3A22" />
    <Books x={830} y={1660} n={7} />
    <Clock x={540} y={330} text="2:07 AM" size={110} />
    <Push out inset={330} cy={290} label="PULL OUT" />
    <Caption text="Exam at 9." y={1700} />
    <Shot text="ECU→WS" />
  </Frame>
);

export const X2: React.FC = () => (
  <Frame bg={["#1E1B4B", "#312E81"]} night>
    <NotationDefs />
    {[0, 1, 2, 3, 4].map((i) => (
      <g key={i} transform={`translate(${120 + i * 190} ${300 + (i % 2) * 140}) rotate(${i * 17 - 30})`}>
        <rect x={-80} y={-100} width={160} height={200} fill="#FFFDF8" stroke={LINE} strokeWidth={4} />
        <path d="M-56 -60 h112 M-56 -20 h90 M-56 20 h112" stroke="#94A3B8" strokeWidth={6} />
      </g>
    ))}
    <Actor x={-20} y={760} s={1.25} who="meera" expr="surprised" viseme="D" arms={{ l: { to: [60, 520], pose: "open", rot: -30 }, r: { to: [560, 520], pose: "open", rot: 30 } }} />
    <Act d="M120 980 q-60 -150 40 -260" label="flip!" lx={10} ly={700} />
    <Act d="M960 980 q60 -150 -40 -260" />
    <Bubble x={200} y={80} w={680} lines={["Which chapters", "did he say?!"]} tail={[520, 820]} size={60} />
    <Shot text="CU" />
  </Frame>
);

export const X3: React.FC = () => (
  <Frame bg={["#E9D8B4", "#C9AE82"]}>
    <NotationDefs />
    <Board x={60} y={200} w={960} h={460} chalk lines={["Exam:", "Ch. 4, Ch. 7", "+ metro case (20)"]} />
    <Actor x={310} y={600} s={0.75} who={PROF} expr="talk" viseme="C" arms={{ r: { to: [560, 440], pose: "open", rot: -40 } }} />
    <Table y={1260} color="#7C5134" top="#A9744D" />
    <Actor x={-140} y={1150} s={0.7} who={KABIR} expr="sleepy" tilt={-10} />
    <Actor x={600} y={1150} s={0.7} who={RIYA} expr="sleepy" tilt={8} />
    <Zzz x={120} y={1150} t={20} />
    <Zzz x={880} y={1150} t={5} />
    <Ripple />
    <rect width={W} height={H} fill="#B45309" opacity={0.12} />
    <Caption text="Monday. Half the class asleep." y={1720} />
    <Shot text="WS · FLASHBACK" />
  </Frame>
);

export const X4: React.FC = () => (
  <Frame bg={["#E9D8B4", "#C9AE82"]}>
    <NotationDefs />
    <Board x={-60} y={160} w={1200} h={520} chalk lines={["\u00a0", "\u00a0", "metro case = 20 marks"]} />
    <Actor x={-30} y={560} s={1.25} who={PROF} expr="talk" viseme="E" turn={0.2} arms={{ r: { to: [600, 500], pose: "open", rot: -50 } }} />
    <Bubble x={40} y={170} w={1000} lines={["Chapters 4 and 7… and the metro", "case study. Twenty marks."]} tail={[440, 760]} size={52} />
    <Act d="M960 1080 q60 -60 20 -160" label="gesture" lx={700} ly={1180} />
    <rect width={W} height={H} fill="#B45309" opacity={0.12} />
    <Shot text="MCU" />
  </Frame>
);

export const X5: React.FC = () => (
  <Frame bg={["#7C5134", "#5B3A22"]}>
    <NotationDefs />
    <rect x={0} y={0} width={W} height={H} fill="#A9744D" opacity={0.4} />
    {[0, 1, 2].map((i) => (
      <path key={i} d={`M${100 + i * 30} ${1500 + i * 60} h${700 - i * 80}`} stroke="#5B3A22" strokeWidth={6} opacity={0.4} />
    ))}
    <Phone x={190} y={300} w={640} rot={-5}>
      <LockNotifs
        w={640}
        date="Monday, 4 March"
        time="9:31"
        notifs={[{ title: "Recording", text: "31:42", recording: true, ongoing: true, actions: ["Save", "Pause"] }]}
      />
    </Phone>
    <Caption text="Srutam wasn't." y={1700} />
    <Push inset={200} cy={760} label="SLOW PUSH" />
    <Shot text="INSERT" />
  </Frame>
);

export const X6: React.FC = () => (
  <Frame bg={["#0B1033", "#050714"]} night>
    <NotationDefs />
    <Phone x={170} y={200} w={680} rot={-3} glow="#3B82F6">
      <AskScreen
        w={680}
        q="What did Prof say will be on the exam?"
        a="In Monday's lecture: chapters 4 and 7, plus the metro case study, “twenty marks.”"
        refs={["Econ lecture · 41:05"]}
      />
    </Phone>
    <g transform="translate(-170 1080) scale(1.5)">
      <Actor x={0} y={0} s={1} who="meera" expr="blank" turn={0.4} gaze={[0.9, -0.9]} />
    </g>
    <Caption text="▶ Econ lecture, 41:05: “…twenty marks.”" y={1740} />
    <Shot text="OTS" />
  </Frame>
);

export const X7: React.FC = () => (
  <Frame bg={["#1E1B4B", "#0B1033"]} night>
    <NotationDefs />
    <rect x={120} y={260} width={700} height={1660} fill="#FDE68A" stroke={LINE} strokeWidth={8} />
    <path d="M820 260 L1010 200 L1010 1920 L820 1920 Z" fill="#7C5134" stroke={LINE} strokeWidth={8} />
    <Actor x={30} y={620} s={0.8} who={RIYA} expr="surprised" viseme="D" arms={{ r: { to: [520, 560], pose: "open", rot: -20 } }} />
    <Actor x={420} y={700} s={0.78} who={KABIR} expr="talk" viseme="C" turn={-0.3} arms={{ l: { to: [60, 520], pose: "point" } }} />
    <Sfx x={540} y={200} text="knock knock" size={110} rot={-4} />
    <Bubble x={420} y={300} w={600} lines={["Did you", "record it?!"]} tail={[560, 720]} size={58} />
    <Act d="M1000 1100 q60 -200 -40 -420" label="door bursts" lx={560} ly={1560} />
    <Pan from={900} to={160} y={1820} whip />
    <Shot text="MS" />
  </Frame>
);

export const X8: React.FC = () => (
  <Frame bg={["#312E81", "#1E1B4B"]} night>
    <NotationDefs />
    <Phone x={270} y={140} w={540} glow="#FBBF24">
      <InsightsScreen
        w={540}
        note="Econ lecture"
        meta="Mon, 9:00 am · 52:10"
        items={[
          { kind: "KEY POINT", text: "Ch. 4: why metro fares rose 12%" },
          { kind: "KEY POINT", text: "Ch. 7: public goods" },
          { kind: "NEXT STEP", text: "Revise the metro case study" },
        ]}
      />
    </Phone>
    <Actor x={-170} y={1230} s={0.8} who={RIYA} expr="laugh" />
    <Actor x={230} y={1180} s={0.85} who="meera" expr="laugh" />
    <Actor x={650} y={1230} s={0.8} who={KABIR} expr="laugh" arms={{ r: { to: [520, 470], pose: "thumb", upright: true, rot: 90 } }} />
    <Kulhad x={140} y={1880} s={1.4} t={10} />
    <Kulhad x={940} y={1880} s={1.4} t={30} />
    <Clock x={180} y={150} text="3:40" size={60} />
    <text x={420} y={150} fill="#DC2626" fontFamily="Kalam" fontWeight={700} fontSize={56} stroke="#FFFFFF" strokeWidth={10} paintOrder="stroke">
      TIME-LAPSE 2:15→3:40
    </text>
    <Shot text="MS" />
  </Frame>
);

export const X9: React.FC = () => (
  <Frame bg={["#FEF9C3", "#FDE68A"]}>
    <NotationDefs />
    <Actor x={130} y={300} s={1.3} who="meera" expr="smug" gaze={[0, 0.8]} arms={{ r: { to: [560, 560], pose: "fist", rot: -20 } }} />
    <g transform="translate(200 1260) rotate(-3)">
      <rect width={680} height={420} fill="#FFFDF8" stroke={LINE} strokeWidth={5} />
      <text x={40} y={90} fontFamily="Kalam" fontWeight={700} fontSize={44} fill="#1F2937">
        <tspan x={40}>Q1. Explain Chapter 4…</tspan>
        <tspan x={40} dy={70}>Q2. Chapter 7: public…</tspan>
        <tspan x={40} dy={70}>Q3. Metro case study</tspan>
        <tspan x={40} dy={70}>      (20 marks)</tspan>
      </text>
      <ellipse cx={330} cy={230} rx={300} ry={50} fill="none" stroke="#DC2626" strokeWidth={6} />
    </g>
    <Act d="M960 860 a60 60 0 1 1 -10 -10" label="pen spin" lx={720} ly={760} />
    <Push inset={260} cy={700} />
    <Shot text="MCU" />
  </Frame>
);

export const X10: React.FC = () => (
  <Frame bg={["#FDE7C7", "#F3D2A8"]}>
    <NotationDefs />
    <Actor x={-170} y={620} s={0.85} who={RIYA} expr="laugh" />
    <Actor x={650} y={620} s={0.85} who={KABIR} expr="laugh" />
    <Actor x={230} y={560} s={1.0} who="meera" expr="laugh" arms={{ r: { to: [520, 470], pose: "phone", upright: true } }} />
    <EndBand tagline="Listen once. Ask anytime." />
    <Shot text="END" />
  </Frame>
);

export const EXAM2: ProIdea = {
  num: 4,
  title: "2 a.m. Before the Exam",
  logline:
    "It's 2 a.m., the exam is at 9, and nobody remembers what Prof said would come. Meera recorded the lecture. One question to Srutam, and the whole hostel floor knows.",
  runtime: "34 s",
  cast: ["Meera", "Prof. Iyer", "Riya", "Kabir"],
  features: ["Recording notification", "Long lectures", "Ask Srutam AI", "Referenced Notes", "Key Points", "Next Steps"],
  beats: [
    { t: "0–3 s", shot: "ECU→WS", cut: "CUT", action: "Clock reads 2:07 AM; pull out to Meera, hand on head, buried in books.", lines: [["Meera (VO)", "Exam at nine. And I can't remember what Prof said would come."]], sfx: "Clock tick, fan hum", anim: "Pull-out 30f ease-in-out; hand slides to forehead", Panel: X1 },
    { t: "3–6 s", shot: "CU", cut: "RIPPLE", action: "Panic: pages fly as she flips through every notebook.", lines: [["Meera", "Which chapters did he say?!"]], sfx: "Frantic page flips", anim: "Smear frames on the hands; papers on 2s", Panel: X2 },
    { t: "6–10 s", shot: "WS · FLASHBACK", cut: "CUT", action: "Monday, sepia: Prof Iyer at the board; Riya and Kabir asleep.", lines: [["Meera (VO)", "Monday. Half the class asleep."]], sfx: "Rewind whir into a chalk scratch, yawns", anim: "Ripple dissolve 12f; Zzz drift", Panel: X3 },
    { t: "10–14 s", shot: "MCU", cut: "CUT", action: "Prof makes his point with an open palm.", lines: [["Prof. Iyer", "Chapters 4 and 7… and the metro case study. Twenty marks."]], sfx: "Chalk tap on 'twenty'", anim: "Palm beat on stressed words", Panel: X4 },
    { t: "14–17 s", shot: "INSERT", cut: "RIPPLE", action: "Meera's phone on the desk: Srutam's notification says Recording, 31:42.", lines: [["Meera (VO)", "Srutam wasn't."]], sfx: "Soft record blip", anim: "Timer ticks; slow push", Panel: X5 },
    { t: "17–21 s", shot: "OTS", cut: "WHIP", action: "Back to 2 a.m.: she asks Srutam AI and gets the answer, with the lecture referenced.", lines: [["Meera", "What did Prof say will be on the exam?"], ["Econ lecture (recording)", "…and the metro case study. Twenty marks."]], sfx: "Typing, answer pop, Prof's voice from the note", anim: "Question types in; answer cascades", Panel: X6 },
    { t: "21–24 s", shot: "MS", cut: "CUT", action: "Knock knock: Riya and Kabir burst in.", lines: [["Riya", "Did you record it?!"]], sfx: "Real door knock, door bang", anim: "Door swing with overshoot; whip pan", Panel: X7 },
    { t: "24–28 s", shot: "MS", cut: "MATCH CUT", action: "Time-lapse: the three revise around the phone with chai; Key Points glow.", lines: [["Meera (VO)", "The whole floor, one recording."]], sfx: "Laughter, chai glasses clink, upbeat music", anim: "Clock spins 2:15→3:40; chai steam loops", Panel: X8 },
    { t: "28–31 s", shot: "MCU", cut: "CUT", action: "Exam hall, morning: Q3 is the metro case study. Smug pen spin.", sfx: "Bell, pen click", anim: "Pen spin on 1s; push in", Panel: X9 },
    { t: "31–34 s", shot: "END", action: "The three laugh; Meera holds up Srutam. End card.", lines: [["Meera (VO)", "Listen once. Ask anytime."]], sfx: "Warm resolve, logo", anim: "End band rises 18f", Panel: X10 },
  ],
};

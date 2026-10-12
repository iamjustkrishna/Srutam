// Storyboard v2, "Who Said That?": the meeting nobody wrote down, staged with
// the rigged cast (arms, mouth shapes) and full camera / action notation.
import { Actor, Hand } from "../cast/Rig";
import { ARJUN, NEHA, RAO } from "../cast/cast";
import {
  AskScreen,
  Board,
  Bubble,
  Burst,
  Caption,
  DockOverApp,
  EndBand,
  Frame,
  H,
  InsightsScreen,
  LINE,
  Phone,
  Sfx,
  Sweat,
  W,
} from "./Kit";
import { Act, NotationDefs, Push, Shake, Shot } from "./Notation";
import type { ProIdea } from "./ProBoard";

export const OfficeWall: React.FC<{ board?: boolean }> = ({ board = true }) => (
  <g>
    <rect width={W} height={H} fill="#DCE3EC" />
    <rect x={660} y={150} width={360} height={520} fill="#BFDBFE" stroke={LINE} strokeWidth={8} />
    {[0, 1, 2, 3].map((i) => (
      <rect key={i} x={690 + i * 82} y={420 - i * 40} width={60} height={250 + i * 40} fill="#94A3B8" />
    ))}
    <path d="M840 150 L840 670 M660 410 L1020 410" stroke={LINE} strokeWidth={6} />
    {board && <Board x={60} y={170} w={560} h={430} lines={["LAUNCH:", "14th?? 21st??", "pricing → ?"]} />}
    <g transform="translate(980 980)">
      <path d="M-40 0 L40 0 L30 120 L-30 120 Z" fill="#B45309" stroke={LINE} strokeWidth={5} />
      {[-50, -20, 10, 40].map((x, i) => (
        <ellipse key={i} cx={x} cy={-40 - (i % 2) * 40} rx={34} ry={70} fill="#15803D" stroke={LINE} strokeWidth={4} transform={`rotate(${x / 2} ${x} -20)`} />
      ))}
    </g>
  </g>
);

export const MeetingTable: React.FC<{ y: number }> = ({ y }) => (
  <g>
    <path d={`M-40 ${y} L${W + 40} ${y} L${W + 40} ${H} L-40 ${H} Z`} fill="#6B4F3A" />
    <rect x={-40} y={y} width={W + 80} height={36} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
    <g transform={`translate(160 ${y - 30})`}>
      <path d="M0 30 L140 30 L120 -50 L20 -50 Z" fill="#CBD5E1" stroke={LINE} strokeWidth={4} />
    </g>
    <g transform={`translate(820 ${y - 4})`}>
      <path d="M0 0 L60 0 L54 -70 L6 -70 Z" fill="#FFFFFF" stroke={LINE} strokeWidth={4} />
      <path d="M58 -55 q30 10 0 34" stroke={LINE} strokeWidth={5} fill="none" />
    </g>
  </g>
);

export const W1: React.FC = () => (
  <Frame bg={["#DCE3EC", "#C7D2E0"]}>
    <NotationDefs />
    <OfficeWall />
    <Actor x={520} y={540} s={0.82} who={RAO} expr="laugh" viseme="C" turn={-0.3} arms={{ l: { to: [-140, 520], pose: "point" } }} />
    <Actor x={-150} y={1130} s={0.7} who={ARJUN} expr="smile" turn={0.3} />
    <Actor x={290} y={1170} s={0.66} who="meera" expr="blank" turn={0.1} />
    <Actor x={680} y={1130} s={0.7} who={NEHA} expr="smile" turn={-0.3} />
    <MeetingTable y={1660} />
    <Bubble x={60} y={660} w={620} lines={["Launch on the 14th.", "All aligned?"]} tail={[700, 860]} size={52} />
    <Act d="M70 1160 q40 50 0 100" label="nod" lx={20} ly={1130} />
    <Act d="M1010 1160 q-40 50 0 100" />
    <Push inset={150} cy={1000} />
    <Shot text="WS" />
  </Frame>
);

export const W2: React.FC = () => (
  <Frame bg={["#DCE3EC", "#C7D2E0"]}>
    <NotationDefs />
    <OfficeWall board={false} />
    <Actor x={20} y={520} s={1.45} who={RAO} expr="laugh" viseme="D" arms={{ r: { to: [560, 470], pose: "thumb", upright: true, rot: 90 } }} />
    <Bubble x={60} y={170} w={760} lines={["Great. Someone", "note it down!"]} tail={[440, 760]} size={62} />
    <Act d="M960 900 q30 -90 -10 -170" label="pop!" lx={830} ly={690} />
    <Shot text="CU" />
  </Frame>
);

export const W3: React.FC = () => {
  const band = (y: number, fill: string, child: React.ReactNode, k: number) => (
    <g key={k}>
      <clipPath id={`w3b${k}`}>
        <rect x={0} y={y} width={W} height={640} />
      </clipPath>
      <rect x={0} y={y} width={W} height={640} fill={fill} />
      <g clipPath={`url(#w3b${k})`}>{child}</g>
    </g>
  );
  return (
    <Frame bg={["#0F172A", "#0F172A"]}>
      <NotationDefs />
      {band(0, "#E0E7FF", <Actor x={130} y={-60} s={0.95} who={ARJUN} expr="sideeye" gaze={[1, 0]} />, 0)}
      {band(640, "#FEF3C7", <Actor x={130} y={580} s={0.95} who={NEHA} expr="sideeye" gaze={[1, 0]} />, 1)}
      {band(1280, "#FDE7C7", <Actor x={130} y={1220} s={0.95} who="meera" expr="smug" turn={0.3} gaze={[0.8, 0]} />, 2)}
      <path d={`M0 640 L${W} 640 M0 1280 L${W} 1280`} stroke="#0F172A" strokeWidth={14} />
      <line x1={1000} y1={120} x2={1000} y2={1780} stroke="#DC2626" strokeWidth={12} markerEnd="url(#camHead)" />
      <text x={985} y={960} fill="#DC2626" fontFamily="Kalam" fontWeight={700} fontSize={56} textAnchor="end" stroke="#FFFFFF" strokeWidth={10} paintOrder="stroke">
        WHIP ↓
      </text>
      <Caption text="…someone?" y={540} />
      <Shot text="CU ×3" />
    </Frame>
  );
};

export const W4: React.FC = () => (
  <Frame bg={["#3F2E22", "#1F1712"]} night>
    <NotationDefs />
    <rect x={0} y={0} width={W} height={260} fill="#6B4F3A" />
    <rect x={0} y={240} width={W} height={36} fill="#8B6B52" stroke={LINE} strokeWidth={5} />
    <Phone x={200} y={470} w={560} rot={-6}>
      <DockOverApp w={560} recording label="Team chat" />
    </Phone>
    {/* Meera's sleeve and pointing hand reaching up to the dock */}
    <g transform="translate(148 1132) rotate(-50)">
      <rect x={-420} y={-40} width={430} height={80} rx={36} fill="#EFE6D8" stroke={LINE} strokeWidth={5} />
      <g transform="scale(1.6)">
        <Hand pose="point" skin="#F2C3A2" />
      </g>
    </g>
    <circle cx={232} cy={1000} r={64} fill="none" stroke="#FFFFFF" strokeWidth={8} />
    <Act d="M120 1260 q40 -110 80 -160" label="tap" lx={20} ly={1330} />
    <Caption text="Nobody wrote it down. Except Srutam." y={1700} />
    <Shot text="INSERT" />
  </Frame>
);

export const W5: React.FC = () => (
  <Frame bg={["#0F172A", "#1E293B"]} night>
    <NotationDefs />
    <Phone x={150} y={180} w={700} glow="#3B82F6">
      <InsightsScreen
        w={700}
        note="Tuesday sync"
        meta="Tue, 11:02 am · 32:14"
        items={[
          { kind: "DECISION", text: "Launch moves to the 14th" },
          { kind: "NEXT STEP", text: "Arjun sends the pricing deck", meta: "By Thursday" },
          { kind: "REMINDER", text: "Pricing deck to Mr. Rao", meta: "Thursday, 10:00 am" },
        ]}
      />
    </Phone>
    {[0, 1, 2].map((i) => (
      <g key={i}>
        <circle cx={70} cy={830 + i * 250} r={46} fill="#F97316" />
        <text x={70} y={850 + i * 250} textAnchor="middle" fill="#FFF" fontFamily="Inter" fontWeight={800} fontSize={52}>
          {i + 1}
        </text>
      </g>
    ))}
    <Push inset={120} cy={1000} label="SLOW PUSH" />
    <Shot text="INSERT" />
  </Frame>
);

export const W6: React.FC = () => (
  <Frame bg={["#111827", "#1F2937"]} night>
    <NotationDefs />
    <g transform="translate(540 900) rotate(-4)">
      <rect x={-330} y={-380} width={660} height={760} rx={30} fill="#FFFDF8" stroke={LINE} strokeWidth={8} />
      <rect x={-330} y={-380} width={660} height={160} rx={30} fill="#DC2626" />
      <text x={0} y={-270} textAnchor="middle" fill="#FFF" fontFamily="Inter" fontWeight={800} fontSize={70}>
        TUESDAY
      </text>
      <text x={0} y={180} textAnchor="middle" fill="#111827" fontFamily="Inter" fontWeight={800} fontSize={360}>
        19
      </text>
      <path d="M-330 -220 Q0 -120 330 -260" stroke="#E5E7EB" strokeWidth={10} fill="none" />
    </g>
    <Act d="M820 560 q140 -120 60 -300" label="page flips" lx={560} ly={230} />
    <text x={540} y={1560} textAnchor="middle" fill="#FDE68A" fontFamily="Playfair Display" fontStyle="italic" fontWeight={700} fontSize={96}>
      One week later
    </text>
    <Shot text="TITLE" />
  </Frame>
);

export const W7: React.FC = () => (
  <Frame bg={["#FEE2E2", "#FCA5A5"]}>
    <NotationDefs />
    {Array.from({ length: 14 }, (_, i) => (
      <path key={i} d={`M540 860 L${540 + Math.cos((i / 14) * 6.28) * 1500} ${860 + Math.sin((i / 14) * 6.28) * 1500}`} stroke="#F87171" strokeWidth={70} opacity={0.3} />
    ))}
    <Actor x={20} y={420} s={1.4} who={RAO} expr="angry" viseme="D" tilt={4} arms={{ r: { to: [520, 950], pose: "fist", rot: 70 } }} />
    <rect x={-20} y={1690} width={W + 40} height={260} fill="#6B4F3A" stroke={LINE} strokeWidth={6} />
    <Burst x={830} y={1690} r={90} color="#2B1E1B" n={10} />
    <Sfx x={820} y={1600} text="BAM!" size={120} rot={-8} />
    <g transform="translate(860 560)" stroke="#DC2626" strokeWidth={16} strokeLinecap="round" fill="none">
      <path d="M-40 -10 Q-10 -10 -10 -40 M40 -10 Q10 -10 10 -40 M-40 10 Q-10 10 -10 40 M40 10 Q10 10 10 40" />
    </g>
    <Bubble x={60} y={90} w={960} lines={["WHO agreed to the 14th?!"]} tail={[520, 420]} size={70} />
    <Shake />
    <Push inset={260} cy={1050} label="CRASH ZOOM" />
    <Shot text="CU" />
  </Frame>
);

export const W8: React.FC = () => (
  <Frame bg={["#FFE4E6", "#FECDD3"]}>
    <NotationDefs />
    <Actor x={-220} y={700} s={1.0} who={ARJUN} expr="worried" viseme="D" turn={0.3} gaze={[1, 0]} arms={{ r: { to: [700, 600], pose: "point" } }} />
    <Actor x={480} y={700} s={1.0} who={NEHA} expr="angry" viseme="C" turn={-0.3} gaze={[-1, 0]} arms={{ l: { to: [-100, 600], pose: "point" } }} />
    <Sweat x={150} y={760} s={1.3} />
    <Sweat x={900} y={760} s={1.3} />
    <Bubble x={40} y={260} w={330} lines={["Her!"]} tail={[180, 640]} size={80} />
    <Bubble x={700} y={260} w={330} lines={["Him!"]} tail={[880, 640]} size={80} />
    <Act d="M380 1180 L620 1180" label="points" lx={390} ly={1150} />
    <Act d="M700 1260 L460 1260" />
    <Shot text="2-SHOT" />
  </Frame>
);

export const W9: React.FC = () => (
  <Frame bg={["#DCE3EC", "#C7D2E0"]}>
    <NotationDefs />
    <OfficeWall board={false} />
    <Actor x={-250} y={760} s={1.15} who="meera" expr="smug" turn={0.3} gaze={[0.8, -0.2]} arms={{ r: { to: [520, 440], pose: "phone", upright: true } }} />
    <Phone x={560} y={140} w={440} rot={3} glow="#3B82F6">
      <AskScreen w={440} q="When did we agree on the 14th?" a="Tuesday's sync, 0:42: “Fine. The 14th, final.”" refs={["Tuesday sync · 0:42"]} />
    </Phone>
    <Actor x={600} y={1260} s={0.62} who={RAO} expr="worried" gaze={[-0.8, -0.4]} />
    <Sweat x={980} y={1450} />
    <Act d="M380 1500 q40 -200 140 -360" label="phone up" lx={20} ly={1640} />
    <Caption text="▶ Rao (recording): “Fine, let's go with the 14th.”" y={1760} />
    <Shot text="MS" />
  </Frame>
);

export const W10: React.FC = () => (
  <Frame bg={["#DCE3EC", "#C7D2E0"]}>
    <NotationDefs />
    <OfficeWall board={false} />
    <Actor x={-160} y={560} s={0.82} who={ARJUN} expr="laugh" />
    <Actor x={540} y={560} s={0.82} who={NEHA} expr="laugh" />
    <Actor x={190} y={760} s={0.95} who={RAO} expr="laugh" arms={{ l: { to: [190, 360], pose: "open", rot: 20 } }} />
    <EndBand tagline="Every meeting, minuted." />
    <Act d="M250 700 q-60 -40 -30 -120" label="sheepish" lx={20} ly={560} />
    <Shot text="END" />
  </Frame>
);

export const MEETING2: ProIdea = {
  num: 3,
  title: "Who Said That?",
  logline:
    "Everyone agreed in the meeting; nobody wrote it down. A week later the boss explodes and everyone points at everyone. Meera lets Srutam answer, in the boss's own recorded words.",
  runtime: "34 s",
  cast: ["Meera", "Mr. Rao (boss)", "Arjun (sales)", "Neha (design)"],
  features: ["Floating dock over any app", "Decisions", "Next Steps", "Reminders", "Ask with Referenced Notes", "Note playback"],
  beats: [
    { t: "0–4 s", shot: "WS", cut: "CUT", action: "Tuesday sync. Rao points at the whiteboard; the team nods along.", lines: [["Rao", "Launch on the 14th. All aligned?"]], sfx: "Office hum, marker squeak, light ukulele", anim: "Nods on 2s, offset by 3 frames each", Panel: W1 },
    { t: "4–6 s", shot: "CU", cut: "WHIP", action: "Rao beams, thumbs up.", lines: [["Rao", "Great. Someone note it down!"]], sfx: "Music stops dead", anim: "Thumb: 4f anticipation, overshoot, settle", Panel: W2 },
    { t: "6–9 s", shot: "CU ×3", cut: "CUT", action: "Side-eye chain: Arjun looks at Neha, Neha looks at Meera. Meera is smug.", sfx: "Three whooshes, one cricket", anim: "Eyes snap on 1s; hold 8f on Meera", Panel: W3 },
    { t: "9–13 s", shot: "INSERT", cut: "CUT", action: "Under the table, Meera's thumb taps the Srutam dock over the team chat: recording.", lines: [["Meera (VO)", "Nobody wrote it down. Except Srutam."]], sfx: "tap · soft record blip", anim: "Finger squash on contact; ripple 10f", Panel: W4 },
    { t: "13–17 s", shot: "INSERT", cut: "MATCH CUT", action: "After the meeting: Decision, Next Step and Reminder drop in one by one.", lines: [["Meera (VO)", "Decision. Next step. Reminder. Before lunch."]], sfx: "Three pops, music returns", anim: "Cards cascade 6f apart, spring in", Panel: W5 },
    { t: "17–19 s", shot: "TITLE", cut: "CUT", action: "Calendar page flips: one week later.", sfx: "Paper flip, clock tick", anim: "Page curl 8f", Panel: W6 },
    { t: "19–23 s", shot: "CU", cut: "CUT", action: "Rao slams the table and roars.", lines: [["Rao", "WHO agreed to the 14th?!"]], sfx: "BAM, record scratch, dramatic sting", anim: "Crash zoom 6f, shake 10f, anger mark pulses", Panel: W7 },
    { t: "23–26 s", shot: "2-SHOT", cut: "CUT", action: "Arjun and Neha point at each other at the same time.", lines: [["Arjun", "Her!"], ["Neha", "Him!"]], sfx: "Two comic boings", anim: "Mirror the point; sweat drops on 2s", Panel: W8 },
    { t: "26–31 s", shot: "MS", cut: "CUT", action: "Meera calmly holds up her phone. Srutam AI finds the moment (it doesn't guess who spoke) and plays it: the room hears Rao himself.", lines: [["Meera", "Relax. Let's ask Srutam."], ["Tuesday sync (recording)", "Fine. The 14th, final."]], sfx: "Answer pop, then the recording plays", anim: "Phone raise with overshoot; Rao shrinks 3%", Panel: W9 },
    { t: "31–34 s", shot: "END", action: "Everyone laughs; Rao rubs his head. End card.", lines: [["Meera (VO)", "Every meeting, minuted."]], sfx: "Laughter, warm resolve, logo", anim: "End band rises 18f", Panel: W10 },
  ],
};

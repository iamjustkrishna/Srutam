// The 15 panels of the painted 30 s storyboard (1600 x 900 each).
import {
  AiChat,
  FloatingDock,
  InsightCard,
  InsightTabs,
  OnDeviceBadge,
  SavedNotification,
  SrutamHeader,
} from "../components/AppUI";
import { rand } from "../anim";
import { MeeraHD as Meera } from "../spirit/MeeraHD";
import { Firefly, Head, Shru } from "../spirit/Shru";
import { SpeedLines, Sparkle } from "../styleboard/FX";
import {
  Bedroom,
  Bokeh,
  Grain,
  MarineDrive,
  Monsoon,
  Painted,
  PH,
  PW,
  Sky,
  SoftCloud,
  Studio,
  TrainWindow,
} from "./Env";
import { Hand, MeeraBack, PhoneUI, Script } from "./Figures";

const INK = "#4A2E35";

/** A cloud of fireflies (thoughts) around a point. */
const Swarm: React.FC<{
  cx: number;
  cy: number;
  n: number;
  spread: number;
  life?: number;
  seed?: number;
}> = ({ cx, cy, n, spread, life = 1, seed = 1 }) => (
  <g>
    {Array.from({ length: n }, (_, i) => (
      <Firefly
        key={i}
        x={cx + (rand(i + seed) - 0.5) * spread * 2}
        y={cy + (rand(i * 3 + seed) - 0.5) * spread * 1.2}
        r={6 + rand(i * 5 + seed) * 9}
        life={life * (0.5 + rand(i * 7 + seed) * 0.5)}
      />
    ))}
  </g>
);

const AnyAppScreen: React.FC = () => (
  <div
    style={{
      position: "absolute",
      inset: 0,
      background: "#FFFFFF",
      padding: "16% 7% 0",
    }}
  >
    {[0, 1, 2].map((i) => (
      <div key={i} style={{ marginBottom: "8%" }}>
        <div style={{ display: "flex", gap: "4%", alignItems: "center" }}>
          <div
            style={{
              width: "12%",
              aspectRatio: "1",
              borderRadius: 99,
              background: "#CBD5E1",
            }}
          />
          <div
            style={{
              width: "45%",
              height: 10,
              borderRadius: 5,
              background: "#E2E8F0",
            }}
          />
        </div>
        <div
          style={{
            marginTop: "4%",
            height: 160,
            borderRadius: 18,
            background: ["#E2E8F0", "#CBD5E1", "#E5E7EB"][i],
          }}
        />
      </div>
    ))}
  </div>
);

export const P1: React.FC = () => (
  <g>
    <Painted>
      <TrainWindow mood="golden" />
      <Meera x={800} y={40} s={1.0} expr="focus" ink={INK} />
    </Painted>
    <Swarm cx={1020} cy={300} n={12} spread={230} seed={2} />
    <Script
      x={80}
      y={690}
      lines={["So many thoughts...", "ideas... things to remember..."]}
      size={60}
    />
    <Grain />
  </g>
);

export const P2: React.FC = () => (
  <g>
    <Painted>
      <TrainWindow mood="golden" tint={0.4} />
      <Meera x={600} y={-230} s={1.75} expr="awe" ink={INK} />
    </Painted>
    <Swarm cx={560} cy={460} n={9} spread={220} life={0.25} seed={5} />
    {Array.from({ length: 30 }, (_, i) => (
      <rect
        key={i}
        x={360 + rand(i) * 420}
        y={560 + rand(i * 2) * 300}
        width={7}
        height={7}
        fill={i % 4 ? "#94A3B8" : "#F97316"}
        opacity={0.8 - rand(i * 2) * 0.6}
      />
    ))}
    <Script
      x={80}
      y={150}
      lines={["By the next station,", "most of them are gone..."]}
      size={58}
    />
    <Grain />
  </g>
);

export const P3: React.FC = () => (
  <g>
    <g filter="blur(10px)">
      <TrainWindow mood="golden" tint={0.25} />
    </g>
    <Bokeh n={16} seed={8} />
    <PhoneUI x={560} y={140} w={380} rot={-6} dock>
      <AnyAppScreen />
    </PhoneUI>
    {/* Shru peeks out of the dock */}
    <g transform="translate(600 450) rotate(-6) scale(1.05)">
      <Head eyes="open" body="#3B82F6" id="p3" />
    </g>
    <defs>
      <radialGradient id="eyep3" cx="50%" cy="65%">
        <stop offset="0%" stopColor="#FEF3C7" />
        <stop offset="60%" stopColor="#FBBF24" />
        <stop offset="100%" stopColor="#B45309" />
      </radialGradient>
    </defs>
    <Sparkle x={700} y={320} s={22} color="#BAE6FD" />
    <Sparkle x={760} y={560} s={14} color="#FDE68A" />
    <Hand x={520} y={620} w={460} />
    <Script
      x={1060}
      y={330}
      lines={["But something", "is listening."]}
      size={66}
    />
    <Grain />
  </g>
);

export const P4: React.FC = () => (
  <g>
    <rect width={PW} height={PH} fill="#E9EDF3" />
    <rect x={0} y={80} width={PW} height={260} rx={30} fill="#D9E0EA" />
    <rect x={0} y={560} width={PW} height={300} rx={30} fill="#D9E0EA" />
    <foreignObject x={0} y={330} width={900} height={240}>
      <div style={{ paddingTop: 20 }}>
        <FloatingDock state="idle" dp={4.2} open={1} />
      </div>
    </foreignObject>
    {[0, 1, 2].map((i) => (
      <circle
        key={i}
        cx={95}
        cy={445}
        r={120 + i * 60}
        fill="none"
        stroke="#EF4444"
        strokeWidth={8 - i * 2}
        opacity={0.6 - i * 0.18}
      />
    ))}
    <g transform="translate(70 440) rotate(-30)">
      <rect
        x={-70}
        y={0}
        width={150}
        height={560}
        rx={75}
        fill="#E7B48F"
        stroke={INK}
        strokeWidth={5}
      />
      <rect
        x={-46}
        y={18}
        width={102}
        height={120}
        rx={48}
        fill="#F8D6BC"
        stroke={INK}
        strokeWidth={3}
      />
    </g>
    <Script
      x={1000}
      y={380}
      lines={["Tap, tap.", "From any app."]}
      size={76}
      color="#1A1433"
      rot={-4}
    />
    <Grain vignette={0.25} />
  </g>
);

export const P5: React.FC = () => (
  <g>
    <Painted>
      <TrainWindow mood="golden" tint={0.3} />
    </Painted>
    <SpeedLines
      cx={1150}
      cy={300}
      inner={170}
      color="#FFF1C9"
      count={120}
      seed={4}
    />
    <path
      d="M120 900 C300 760 520 640 700 560"
      stroke="#67E8F9"
      strokeWidth={60}
      fill="none"
      opacity={0.18}
      strokeLinecap="round"
    />
    <g transform="translate(860 460) rotate(-28)">
      <Shru x={-330} y={-210} s={1.1} pose="leap" eyes="fierce" phase={1.2} />
    </g>
    <Firefly x={1150} y={300} r={30} life={0.85} />
    <Sparkle x={1250} y={220} s={26} color="#FFFFFF" />
    <Grain />
  </g>
);

export const P6: React.FC = () => (
  <g>
    <Painted>
      <TrainWindow mood="golden" />
      <Meera x={820} y={40} s={1.08} expr="awe" ink={INK} />
    </Painted>
    {[0, 1, 2].map((i) => (
      <path
        key={i}
        d={`M${1210 - i * 30} ${520 - i * 14} q-26 30 0 60`}
        stroke="#FFFFFF"
        strokeWidth={6}
        fill="none"
        opacity={0.85 - i * 0.25}
        strokeLinecap="round"
      />
    ))}
    <Shru x={520} y={520} s={0.62} pose="sit" phase={2.4} />
    <g>
      <path
        d="M90 90 h700 a40 40 0 0 1 40 40 v230 a40 40 0 0 1 -40 40 h-120 l40 70 l-120 -70 h-500 a40 40 0 0 1 -40 -40 v-230 a40 40 0 0 1 40 -40 z"
        fill="#FFF9F2"
        opacity={0.95}
      />
      <Script
        x={130}
        y={170}
        lines={[
          "What if the launch moves to Friday...",
          "and beta users invite a friend?",
          "I'll draft the email by Thursday.",
        ]}
        size={42}
        rot={0}
        color="#3B2A4A"
      />
    </g>
    <Grain />
  </g>
);

export const P7: React.FC = () => (
  <g>
    <Sky mood="golden" sun={[1300, 640]} sunR={50} />
    <SoftCloud x={300} y={220} s={1.2} lit="#FFD3E6" shade="#A58BE0" seed={3} />
    <Bokeh n={14} seed={11} />
    <PhoneUI x={260} y={70} w={520} rot={-3}>
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: "#F4F5F8",
          padding: "9% 6% 0",
        }}
      >
        <SavedNotification width={520 * 0.88} finishing={false} seconds={5} />
        <div style={{ marginTop: 34 }}>
          <SrutamHeader
            word="Notes"
            size={52}
            sub={
              <div style={{ marginTop: 8 }}>
                <OnDeviceBadge size={22} />
              </div>
            }
          />
        </div>
        <div
          style={{
            marginTop: 22,
            padding: 22,
            borderRadius: 24,
            background: "#FFFFFF",
            border: "1.5px solid #E2E8F0",
            fontSize: 24,
            lineHeight: 1.45,
            color: "#0F172A",
          }}
        >
          <div style={{ fontWeight: 700, fontSize: 26 }}>Bus ride idea</div>
          <div style={{ color: "#64748B", fontSize: 18, marginBottom: 10 }}>
            Today, 8:42 am · 0:05
          </div>
          What if the launch moves to Friday, and beta users invite one friend
          each? I'll draft the email by Thursday.
        </div>
      </div>
    </PhoneUI>
    <Shru x={540} y={-20} s={0.42} pose="curl" />
    <Script
      x={940}
      y={330}
      lines={["Saved and transcribed,", "right on your phone."]}
      size={62}
    />
    <Grain />
  </g>
);

export const P8: React.FC = () => (
  <g>
    <Sky mood="dusk" sun={[1350, 700]} sunR={44} />
    <SoftCloud
      x={1100}
      y={230}
      s={1.0}
      lit="#FFD3E6"
      shade="#8E72D6"
      seed={6}
    />
    {[
      ["#2563EB", 160, 160],
      ["#D97706", 120, 380],
      ["#0D9488", 220, 640],
    ].map(([c, x, y], i) => (
      <g key={i}>
        <path
          d={`M${x} ${y} C${Number(x) + 200} ${Number(y) - 80} 420 ${360 + i * 40} 560 ${380 + i * 40}`}
          stroke={String(c)}
          strokeWidth={6}
          fill="none"
          opacity={0.6}
          strokeDasharray="4 12"
          strokeLinecap="round"
        />
        <circle
          cx={Number(x)}
          cy={Number(y)}
          r={34}
          fill={String(c)}
          opacity={0.3}
        />
        <circle cx={Number(x)} cy={Number(y)} r={14} fill={String(c)} />
      </g>
    ))}
    <PhoneUI x={560} y={60} w={520} rot={2}>
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: "#F4F5F8",
          padding: "10% 6% 0",
        }}
      >
        <SrutamHeader
          word="Insights"
          size={56}
          sub={
            <div style={{ marginTop: 8 }}>
              <OnDeviceBadge size={22} />
            </div>
          }
        />
        <div style={{ marginTop: 22 }}>
          <InsightTabs
            s={0.72}
            selected="next"
            counts={{ next: 1, idea: 1, decision: 1 }}
          />
        </div>
        <div style={{ marginTop: 20 }}>
          <InsightCard
            s={0.74}
            kind="next"
            text="Draft the launch email by Thursday"
          />
        </div>
      </div>
    </PhoneUI>
    <Script
      x={1140}
      y={560}
      lines={["Next steps, ideas,", "decisions. Sorted."]}
      size={60}
    />
    <Grain />
  </g>
);

const Slice: React.FC<{ x: number; children: React.ReactNode }> = ({
  x,
  children,
}) => (
  <svg
    x={x}
    y={0}
    width={PW / 3}
    height={PH}
    viewBox={`${x} 0 ${PW / 3} ${PH}`}
  >
    {children}
  </svg>
);

export const P9: React.FC = () => (
  <g>
    <Slice x={0}>
      <Painted>
        <Studio />
        <Meera x={-60} y={160} s={0.95} expr="focus" ink={INK} />
      </Painted>
      <Script
        x={40}
        y={110}
        lines={["In the studio,", "mid-sketch..."]}
        size={46}
      />
    </Slice>
    <Slice x={PW / 3}>
      <Painted>
        <Monsoon />
        <Meera x={480} y={170} s={0.95} expr="awe" ink={INK} />
      </Painted>
      <path
        d={`M${PW / 3 + 70} 250 Q${PW / 3 + 300} 40 ${PW / 3 + 520} 250 Z`}
        fill="#BE123C"
        stroke={INK}
        strokeWidth={4}
      />
      <line
        x1={PW / 3 + 295}
        y1={150}
        x2={PW / 3 + 300}
        y2={420}
        stroke={INK}
        strokeWidth={6}
      />
      <Script
        x={PW / 3 + 40}
        y={110}
        lines={["In the monsoon,", "stuck in traffic..."]}
        size={46}
      />
    </Slice>
    <Slice x={(PW * 2) / 3}>
      <Painted>
        <Bedroom />
        <Meera
          x={(PW * 2) / 3 - 40}
          y={180}
          s={0.95}
          expr="smile"
          light="night"
          ink={INK}
        />
      </Painted>
      <Shru x={(PW * 2) / 3 + 330} y={600} s={0.38} pose="curl" />
      <Script
        x={(PW * 2) / 3 + 40}
        y={110}
        lines={["At 2 a.m.,", "when it finally clicks..."]}
        size={46}
      />
    </Slice>
    <line
      x1={PW / 3}
      y1={0}
      x2={PW / 3}
      y2={PH}
      stroke="#05030F"
      strokeWidth={10}
    />
    <line
      x1={(PW * 2) / 3}
      y1={0}
      x2={(PW * 2) / 3}
      y2={PH}
      stroke="#05030F"
      strokeWidth={10}
    />
    <Grain />
  </g>
);

export const P10: React.FC = () => (
  <g>
    <Sky mood="morning" sun={[1250, 300]} sunR={56} />
    <SoftCloud
      x={360}
      y={260}
      s={1.2}
      lit="#FFFFFF"
      shade="#C7D2F0"
      seed={12}
    />
    <PhoneUI x={520} y={120} w={520} dark>
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: "linear-gradient(180deg,#1E2A55,#0B1030)",
          padding: "12% 6% 0",
          color: "#F8FAFC",
        }}
      >
        <div style={{ textAlign: "center", fontSize: 110, fontWeight: 300 }}>
          8:45
        </div>
        <div
          style={{
            textAlign: "center",
            fontSize: 24,
            opacity: 0.8,
            marginBottom: 30,
          }}
        >
          Friday, 17 October
        </div>
        <div
          style={{
            borderRadius: 30,
            background: "rgba(255,255,255,0.92)",
            color: "#0F172A",
            padding: "22px 24px",
          }}
        >
          <div style={{ fontSize: 18, color: "#64748B", fontWeight: 600 }}>
            Srutam Reminders · now
          </div>
          <div style={{ fontSize: 30, fontWeight: 800, marginTop: 6 }}>
            Send the launch email
          </div>
          <div style={{ fontSize: 22, color: "#475569" }}>
            Starts in 15 minutes.
          </div>
          <div style={{ display: "flex", gap: 14, marginTop: 16 }}>
            <div
              style={{
                flex: 1,
                textAlign: "center",
                padding: "12px 0",
                borderRadius: 999,
                background: "#10B981",
                color: "#FFF",
                fontWeight: 700,
                fontSize: 22,
              }}
            >
              Mark Done
            </div>
            <div
              style={{
                flex: 1,
                textAlign: "center",
                padding: "12px 0",
                borderRadius: 999,
                background: "#E2E8F0",
                fontWeight: 700,
                fontSize: 22,
              }}
            >
              Snooze 10m
            </div>
          </div>
        </div>
      </div>
    </PhoneUI>
    <Script
      x={1120}
      y={600}
      lines={["Right on time."]}
      size={64}
      color="#1A1433"
    />
    <Grain vignette={0.3} />
  </g>
);

export const P11: React.FC = () => {
  const stars: [number, number][] = [
    [220, 200],
    [420, 120],
    [640, 230],
    [860, 150],
    [1080, 260],
    [1300, 170],
    [520, 360],
    [980, 400],
    [1220, 380],
  ];
  const links = [
    [0, 1],
    [1, 2],
    [2, 3],
    [3, 4],
    [4, 5],
    [2, 6],
    [6, 7],
    [4, 7],
    [7, 8],
    [5, 8],
  ];
  return (
    <g>
      <Painted>
        <MarineDrive mood="night" />
      </Painted>
      {links.map(([a, b], i) => (
        <line
          key={i}
          x1={stars[a][0]}
          y1={stars[a][1]}
          x2={stars[b][0]}
          y2={stars[b][1]}
          stroke="#A78BFA"
          strokeWidth={3}
          opacity={0.85}
        />
      ))}
      {stars.map(([x, y], i) => (
        <Firefly key={i} x={x} y={y} r={11} life={1} />
      ))}
      <Script x={90} y={560} lines={["Every thought", "connects."]} size={70} />
      <Grain />
    </g>
  );
};

export const P12: React.FC = () => (
  <g>
    <Painted>
      <Bedroom />
      <Meera x={880} y={60} s={1.05} expr="smile" light="night" ink={INK} />
    </Painted>
    <PhoneUI x={300} y={70} w={500} dark rot={-4}>
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: "#050714",
          padding: "11% 6% 0",
        }}
      >
        <SrutamHeader
          word="AI"
          size={56}
          dark
          sub={
            <div style={{ color: "#94A3B8", fontSize: 22, marginTop: 4 }}>
              Ask across all your voice notes
            </div>
          }
        />
        <div style={{ marginTop: 28 }}>
          <AiChat s={0.7} frame={999} askAt={0} answerAt={10} />
        </div>
      </div>
    </PhoneUI>
    <Script x={1000} y={130} lines={["Just ask."]} size={70} />
    <Grain />
  </g>
);

export const P13: React.FC = () => {
  const steps = [
    { label: "Speak", sub: "from any app", color: "#EF4444", icon: "wave" },
    {
      label: "Transcribe",
      sub: "on your phone",
      color: "#10B981",
      icon: "lock",
    },
    {
      label: "Insights",
      sub: "steps · ideas · decisions",
      color: "#2563EB",
      icon: "dots",
    },
    { label: "Ask", sub: "across all notes", color: "#7C3AED", icon: "spark" },
  ];
  return (
    <g>
      <Sky mood="dusk" />
      <Bokeh n={12} seed={40} />
      {steps.map((st, i) => {
        const x = 150 + i * 360;
        return (
          <g key={st.label}>
            <rect
              x={x}
              y={250}
              width={220}
              height={220}
              rx={48}
              fill="#FFFFFF"
              opacity={0.95}
            />
            <g transform={`translate(${x + 110} 360)`}>
              {st.icon === "wave" &&
                [-40, -20, 0, 20, 40].map((bx, k) => (
                  <rect
                    key={k}
                    x={bx - 6}
                    y={-[20, 40, 56, 40, 20][k] / 2}
                    width={12}
                    height={[20, 40, 56, 40, 20][k]}
                    rx={6}
                    fill={st.color}
                  />
                ))}
              {st.icon === "lock" && (
                <g fill={st.color}>
                  <rect x={-36} y={-6} width={72} height={58} rx={10} />
                  <path
                    d="M-22 -6 V-24 A22 22 0 0 1 22 -24 V-6"
                    stroke={st.color}
                    strokeWidth={10}
                    fill="none"
                  />
                </g>
              )}
              {st.icon === "dots" &&
                ["#2563EB", "#D97706", "#0D9488"].map((c, k) => (
                  <circle key={k} cx={(k - 1) * 40} cy={0} r={16} fill={c} />
                ))}
              {st.icon === "spark" && (
                <path
                  d="M0 -50 L12 -12 L50 0 L12 12 L0 50 L-12 12 L-50 0 L-12 -12 Z"
                  fill={st.color}
                />
              )}
            </g>
            <text
              x={x + 110}
              y={540}
              textAnchor="middle"
              fontFamily="Kalam"
              fontWeight={700}
              fontSize={44}
              fill="#FFFFFF"
            >
              {st.label}
            </text>
            <text
              x={x + 110}
              y={585}
              textAnchor="middle"
              fontFamily="Inter"
              fontSize={22}
              fill="#FDE68A"
            >
              {st.sub}
            </text>
            {i < 3 && (
              <path
                d={`M${x + 240} 360 L${x + 330} 360 M${x + 312} 342 L${x + 332} 360 L${x + 312} 378`}
                stroke="#FFFFFF"
                strokeWidth={7}
                fill="none"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            )}
          </g>
        );
      })}
      <Shru x={1300} y={600} s={0.42} pose="sit" />
      <Grain />
    </g>
  );
};

export const P14: React.FC = () => (
  <g>
    <Painted>
      <MarineDrive mood="dusk" />
    </Painted>
    <Swarm cx={900} cy={260} n={16} spread={420} seed={21} />
    <MeeraBack x={360} y={300} s={0.95} />
    <Shru x={760} y={660} s={0.45} pose="sit" />
    <Script x={1000} y={130} lines={["Every thought,", "heard."]} size={72} />
    <Grain />
  </g>
);

export const P15: React.FC = () => (
  <g>
    <Sky mood="night" />
    {Array.from({ length: 160 }, (_, i) => (
      <circle
        key={i}
        cx={rand(i + 90) * PW}
        cy={rand(i * 3 + 90) * PH}
        r={0.8 + rand(i * 5) * 2}
        fill="#FFFFFF"
        opacity={0.3 + rand(i * 7) * 0.7}
      />
    ))}
    <path
      d="M1300 120 L1480 40"
      stroke="#FFFFFF"
      strokeWidth={4}
      strokeLinecap="round"
      opacity={0.85}
    />
    <circle cx={1480} cy={40} r={6} fill="#FFFFFF" />
    <text
      x={PW / 2}
      y={420}
      textAnchor="middle"
      fontFamily="Playfair Display"
      fontSize={170}
      fill="#FFFFFF"
    >
      Srutam
    </text>
    <text
      x={PW / 2}
      y={500}
      textAnchor="middle"
      fontFamily="Kalam"
      fontSize={52}
      fill="#E9D5FF"
    >
      Catch it before it's gone.
    </text>
    <rect
      x={PW / 2 - 200}
      y={570}
      width={400}
      height={90}
      rx={45}
      fill="#FFFFFF"
    />
    <text
      x={PW / 2}
      y={628}
      textAnchor="middle"
      fontFamily="Inter"
      fontWeight={800}
      fontSize={34}
      fill="#1A1433"
    >
      Available on Android
    </text>
    <Shru x={1180} y={600} s={0.42} pose="curl" />
    <Grain />
  </g>
);

export { PH, PW };
export const PANELS = [
  P1,
  P2,
  P3,
  P4,
  P5,
  P6,
  P7,
  P8,
  P9,
  P10,
  P11,
  P12,
  P13,
  P14,
  P15,
];

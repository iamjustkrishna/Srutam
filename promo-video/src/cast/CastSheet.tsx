// Cast sheet: the upgraded characters, their rig poses and the mouth chart.
import { AbsoluteFill } from "remotion";
import { FONT } from "../theme";
import { ARJUN, KABIR, NEHA, PROF, RAO, RIYA } from "./cast";
import { Mouth, type Viseme } from "./Mouth";
import { Actor, type ActorProps } from "./Rig";

const CELL_W = 520;
const CELL_H = 640;

const Cell: React.FC<{ name: string; note: string; actor: Omit<ActorProps, "x" | "y" | "s">; bg: string }> = ({ name, note, actor, bg }) => (
  <div style={{ width: CELL_W }}>
    <div style={{ width: CELL_W, height: CELL_H, borderRadius: 28, background: bg, overflow: "hidden", border: "4px solid #2B1E1B" }}>
      <svg viewBox="-150 100 900 1108" width={CELL_W} height={CELL_H}>
        <Actor x={0} y={160} s={1} {...actor} />
      </svg>
    </div>
    <div style={{ fontFamily: FONT.serif, fontSize: 40, marginTop: 12 }}>{name}</div>
    <div style={{ fontSize: 22, color: "#7A6A60", lineHeight: 1.35 }}>{note}</div>
  </div>
);

const MOUTHS: [Viseme, string][] = [
  ["A", "M · B · P"],
  ["B", "K · S · T · EE"],
  ["C", "EH · AE"],
  ["D", "AA"],
  ["E", "AO · ER"],
  ["F", "OO · W"],
  ["G", "F · V"],
  ["H", "L"],
  ["X", "rest"],
];

export const CastSheet: React.FC = () => (
  <AbsoluteFill style={{ background: "#F3EDE4", color: "#2B1E1B", fontFamily: FONT.sans, padding: "40px 48px" }}>
    <div style={{ display: "flex", alignItems: "baseline", gap: 24 }}>
      <div style={{ fontFamily: FONT.serif, fontSize: 70 }}>Srutam · the cast, v2</div>
      <div style={{ fontSize: 28, color: "#7A6A60" }}>
        IK arms with hand poses · Rhubarb mouth shapes · head turn and tilt · breathing · face shading
      </div>
    </div>
    <div style={{ display: "flex", gap: 24, marginTop: 24 }}>
      <Cell name="Meera" note="Shows Srutam to the room: phone up, smug." bg="#FDE7C7" actor={{ who: "meera", expr: "smug", turn: 0.3, gaze: [0.6, 0], arms: { r: { to: [500, 470], pose: "phone", upright: true } } }} />
      <Cell name="Meera" note="“Who's writing this down?” Hand to the forehead." bg="#E0E7FF" actor={{ who: "meera", expr: "worried", tilt: -4, viseme: "E", arms: { l: { to: [215, 360], pose: "open", rot: 20 } } }} />
      <Cell name="Mr. Rao" note="The boss. Points, shouts, then backs down." bg="#FEE2E2" actor={{ who: RAO, expr: "angry", viseme: "D", tilt: 3, arms: { r: { to: [715, 585], pose: "point" } } }} />
      <Cell name="Arjun" note="Sales. “Her!” Points left, sweating." bg="#DBEAFE" actor={{ who: ARJUN, expr: "worried", gaze: [-0.9, 0], turn: -0.3, viseme: "C", arms: { l: { to: [-115, 585], pose: "point" } } }} />
      <Cell name="Neha" note="Design lead. “Him!” Points right." bg="#FEF3C7" actor={{ who: NEHA, expr: "sideeye", turn: 0.3, viseme: "B", arms: { r: { to: [715, 585], pose: "point" } } }} />
    </div>
    <div style={{ display: "flex", gap: 24, marginTop: 28 }}>
      <Cell name="Prof. Iyer" note="Economics. Explains with an open palm." bg="#E7E5E4" actor={{ who: PROF, expr: "talk", viseme: "C", arms: { r: { to: [540, 520], pose: "open", rot: -40 } } }} />
      <Cell name="Riya" note="Hostel friend. “Did you record it?!”" bg="#FCE7F3" actor={{ who: RIYA, expr: "surprised", viseme: "D", arms: { l: { to: [175, 470], pose: "open", rot: 15 }, r: { to: [425, 470], pose: "open", rot: -15 } } }} />
      <Cell name="Kabir" note="Hostel friend. Thumbs up at 2 a.m." bg="#DCFCE7" actor={{ who: KABIR, expr: "laugh", turn: -0.2, arms: { r: { to: [520, 500], pose: "thumb", upright: true, rot: 90 } } }} />
      <div style={{ width: CELL_W * 2 + 24 }}>
        <div style={{ height: CELL_H, borderRadius: 28, background: "#FFFDF8", border: "4px solid #2B1E1B", padding: "26px 30px", boxSizing: "border-box" }}>
          <div style={{ fontFamily: FONT.serif, fontSize: 40 }}>Mouth chart</div>
          <div style={{ fontSize: 22, color: "#7A6A60", marginBottom: 14 }}>
            Rhubarb Lip Sync reads each voice line and picks one of these per frame.
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: 16 }}>
            {MOUTHS.map(([v, label]) => (
              <div key={v} style={{ textAlign: "center" }}>
                <svg viewBox="-60 -50 120 100" width={150} height={125}>
                  <ellipse rx={58} ry={48} fill="#F2C3A2" />
                  <Mouth shape={v === "X" ? "A" : v} x={0} y={0} s={1.3} />
                </svg>
                <div style={{ fontWeight: 800, fontSize: 26 }}>{v}</div>
                <div style={{ fontSize: 19, color: "#7A6A60" }}>{label}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  </AbsoluteFill>
);

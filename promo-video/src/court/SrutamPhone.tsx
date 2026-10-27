// The real Srutam app on a phone, rebuilt to match its screens (app_live_screen.png,
// screen_card_opened.png): Playfair "Srutam" with a blue italic second word, white rounded
// cards, a blue play button over a grey waveform, a "TRANSCRIPT EVIDENCE" block and the
// Notes / Insights / AI bar beside the red record button. Logo: the app's srutam_final_log.
import { Img, interpolate, staticFile } from "remotion";
import { clamp, easeOut } from "../anim";

const BLUE = "#2563EB";
const INK = "#0F172A";
const MUTED = "#64748B";
const LINE = "#E2E8F0";
const BG = "#F1F4F9";

const WAVE = Array.from({ length: 38 }, (_, i) => 0.3 + 0.7 * Math.abs(Math.sin(i * 1.9) * Math.cos(i * 0.55)));

/** The phone's screen content at story frame f; `ask`, `answer` and `play` are the beats. */
export const SrutamPhone: React.FC<{ f: number; w: number; ask: number; answer: number; play: number; playLen: number }> = ({ f, w, ask, answer, play, playLen }) => {
  const k = w / 880; // the real screens are 1080 px wide; drawn a little larger so they read on a phone-sized video
  const px = (n: number) => n * k;
  const Q = "What did Priya say the deadline was?";
  const typed = Q.slice(0, Math.max(0, Math.min(Q.length, Math.floor((f - ask) * 1.6))));
  const sent = f >= ask + Q.length / 1.6 + 4;
  const showA = interpolate(f, [answer, answer + 8], [0, 1], { ...clamp, easing: easeOut });
  const p = interpolate(f, [play, play + playLen], [0, 1], clamp);
  const pressed = f >= play - 3 && f < play + 3;
  return (
    <div style={{ position: "absolute", inset: 0, background: BG, fontFamily: "Inter", color: INK, overflow: "hidden" }}>
      {/* status bar */}
      <div style={{ display: "flex", justifyContent: "space-between", padding: `${px(40)}px ${px(70)}px 0`, fontSize: px(40), fontWeight: 600 }}>
        <span>21:06</span>
        <span style={{ letterSpacing: px(6) }}>▂▄▆ 82%</span>
      </div>
      {/* header */}
      <div style={{ display: "flex", alignItems: "center", gap: px(24), padding: `${px(60)}px ${px(56)}px 0` }}>
        <Img src={staticFile("court/srutam-logo.png")} style={{ width: px(120), height: px(120), margin: px(-14) }} />
        <div style={{ fontFamily: "Playfair Display", fontSize: px(96), lineHeight: 1 }}>
          Srutam <span style={{ color: BLUE, fontStyle: "italic", fontWeight: 700 }}>AI</span>
        </div>
      </div>
      <div style={{ padding: `${px(18)}px ${px(60)}px 0`, fontSize: px(36), color: "#0F766E", fontWeight: 600 }}>🔒 Transcribed on this device</div>

      {/* the question */}
      <div style={{ display: "flex", justifyContent: "flex-end", padding: `${px(70)}px ${px(56)}px 0` }}>
        <div style={{ maxWidth: px(800), background: sent ? BLUE : "#FFFFFF", color: sent ? "#fff" : INK, border: sent ? "none" : `${px(3)}px solid ${LINE}`, borderRadius: px(48), borderBottomRightRadius: px(14), padding: `${px(32)}px ${px(44)}px`, fontSize: px(46), fontWeight: 600, minHeight: px(60) }}>
          {typed}
          {!sent && <span style={{ opacity: Math.floor(f / 8) % 2 ? 1 : 0 }}>|</span>}
        </div>
      </div>

      {/* the answer, with transcript evidence */}
      <div style={{ opacity: showA, transform: `translateY(${(1 - showA) * px(60)}px)`, margin: `${px(44)}px ${px(40)}px 0`, background: "#FFFFFF", borderRadius: px(56), border: `${px(3)}px solid ${LINE}`, padding: px(44), boxShadow: `0 ${px(20)}px ${px(60)}px rgba(15,23,42,0.08)` }}>
        <div style={{ fontSize: px(50), fontWeight: 700, lineHeight: 1.3 }}>
          <span style={{ color: BLUE }}>✦</span> Tomorrow, 10 a.m. Priya said it on today's 3 p.m. call.
        </div>
        <div style={{ display: "inline-flex", alignItems: "center", gap: px(14), marginTop: px(34), background: "#EEF2F7", borderRadius: px(40), padding: `${px(14)}px ${px(30)}px`, fontSize: px(34), fontWeight: 700, letterSpacing: px(3), color: "#334155" }}>
          <span style={{ color: BLUE }}>▍▌▍</span> VOICE NOTE · TEAM CALL · 3:00 PM
        </div>
        <div style={{ marginTop: px(30), background: "#F8FAFC", border: `${px(3)}px solid ${LINE}`, borderRadius: px(36), padding: `${px(30)}px ${px(36)}px`, display: "flex", gap: px(28) }}>
          <div style={{ width: px(8), borderRadius: px(4), background: "#1E293B" }} />
          <div>
            <div style={{ fontSize: px(32), fontWeight: 700, letterSpacing: px(3), color: "#94A3B8" }}>TRANSCRIPT EVIDENCE · 14:22</div>
            <div style={{ fontSize: px(44), lineHeight: 1.4, color: "#334155", marginTop: px(12) }}>
              “…and Aarav, send me the final deck by <span style={{ background: "#BFDBFE", color: INK, fontWeight: 700, borderRadius: px(10), padding: `0 ${px(8)}px` }}>tomorrow, ten a.m.</span>”
            </div>
          </div>
        </div>
        {/* play row, as on the Notes cards */}
        <div style={{ display: "flex", alignItems: "center", gap: px(30), marginTop: px(36) }}>
          <div style={{ width: px(110), height: px(110), borderRadius: 999, background: BLUE, display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: px(46), transform: `scale(${pressed ? 0.88 : 1})`, flexShrink: 0 }}>{p > 0 && p < 1 ? "❚❚" : "▶"}</div>
          <div style={{ flex: 1, display: "flex", alignItems: "center", gap: px(7), height: px(90) }}>
            {WAVE.map((h, i) => (
              <div key={i} style={{ flex: 1, height: `${h * 100}%`, borderRadius: px(6), background: i / WAVE.length < p ? BLUE : "#CBD5E1" }} />
            ))}
          </div>
          <div style={{ fontSize: px(42), fontWeight: 700 }}>0:{String(Math.floor(4 * p) + 22).padStart(2, "0")}</div>
        </div>
      </div>

      {/* bottom bar */}
      <div style={{ position: "absolute", left: px(40), right: px(40), bottom: px(70), display: "flex", alignItems: "center", gap: px(30) }}>
        <div style={{ flex: 1, display: "flex", justifyContent: "space-around", alignItems: "center", background: "#EEF1F6", borderRadius: px(80), padding: px(14), border: `${px(3)}px solid ${LINE}`, fontSize: px(42), color: MUTED, fontWeight: 600 }}>
          <span>≡ Notes</span>
          <span>☑ Insights</span>
          <span style={{ background: "#fff", color: INK, borderRadius: px(60), padding: `${px(26)}px ${px(46)}px`, boxShadow: `0 ${px(6)}px ${px(16)}px rgba(15,23,42,0.1)` }}>✦ AI</span>
        </div>
        <div style={{ width: px(150), height: px(150), borderRadius: 999, background: "#FECACA", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <div style={{ width: px(110), height: px(110), borderRadius: 999, background: "#EF4444", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <div style={{ width: px(60), height: px(60), borderRadius: 999, background: "#991B1B" }} />
          </div>
        </div>
      </div>
    </div>
  );
};

/** A phone body around the screen. */
export const PhoneBody: React.FC<{ w: number; children: React.ReactNode }> = ({ w, children }) => {
  const h = w * (2400 / 1080);
  return (
    <div style={{ width: w, height: h, borderRadius: w * 0.11, background: "#0B0F1A", padding: w * 0.03, boxShadow: "0 40px 120px rgba(0,0,0,0.55)", border: "3px solid #3A4154" }}>
      <div style={{ position: "relative", width: "100%", height: "100%", borderRadius: w * 0.085, overflow: "hidden" }}>{children}</div>
    </div>
  );
};

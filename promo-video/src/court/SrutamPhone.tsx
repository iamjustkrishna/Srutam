// The real Srutam AI chat screen on a phone, rebuilt from the app's own code and screenshots
// (ui/screens/ChatScreen.kt, app_live_screen.png): "Srutam AI" top bar, cobalt user bubble and
// slate answer bubble (22dp corners, 8dp on the tail side), "Thinking..." with a spinner, a
// source card styled like the Notes cards (blue play button, grey waveform), and the
// "Ask anything about this recording" input bar. Sizes are in dp, like the app.
import { Img, interpolate, staticFile } from "remotion";
import { clamp, easeOut } from "../anim";
import { springAt } from "../stories/physics";

const COBALT = "#2563EB";
const INK = "#0F172A";
const MUTED = "#64748B";
const BORDER = "#E2E8F0";
const SLATE = "#F1F5F9";
const BG = "#F8FAFC";

const WAVE = Array.from({ length: 30 }, (_, i) => 0.28 + 0.72 * Math.abs(Math.sin(i * 1.9) * Math.cos(i * 0.55)));
const Q = "What did Priya say the deadline was?";

/** The phone's screen at story frame f. Beats: ask (typing starts), answer, play (recording). */
export const SrutamPhone: React.FC<{ f: number; w: number; ask: number; answer: number; play: number; playLen: number }> = ({ f, w, ask, answer, play, playLen }) => {
  // The real screen is ~392dp wide; draw it a little larger (340dp) so it reads in a video.
  const dp = (n: number) => (n * w) / 340;
  const n = Math.max(0, Math.min(Q.length, Math.floor((f - ask) * 2.4)));
  const sentAt = ask + Q.length / 2.4 + 3;
  const sent = f >= sentAt;
  const bubble = springAt(f, 0, [{ at: sentAt, to: 1 }], 15, 200);
  const thinking = sent && f < answer;
  const a = springAt(f, 0, [{ at: answer, to: 1 }], 15, 170);
  const p = interpolate(f, [play, play + playLen], [0, 1], clamp);
  const pressed = f >= play - 3 && f < play + 3;
  const words = "Tomorrow at 10 a.m. Priya said it on today's 3 p.m. call.".split(" ");
  const shown = Math.round(interpolate(f, [answer, answer + 10], [0, words.length], { ...clamp, easing: easeOut }));
  return (
    <div style={{ position: "absolute", inset: 0, background: BG, fontFamily: "Inter", color: INK, overflow: "hidden" }}>
      {/* status bar */}
      <div style={{ height: dp(30), display: "flex", alignItems: "center", justifyContent: "space-between", padding: `0 ${dp(22)}px`, fontSize: dp(12.5), fontWeight: 600 }}>
        <span>9:06</span>
        <span style={{ display: "flex", gap: dp(5), alignItems: "center" }}>
          <span style={{ display: "flex", gap: dp(1.5), alignItems: "flex-end" }}>
            {[4, 6, 8, 10].map((h) => <span key={h} style={{ width: dp(2.5), height: dp(h), background: INK, borderRadius: dp(1) }} />)}
          </span>
          <span style={{ width: dp(20), height: dp(10), border: `${dp(1.2)}px solid ${INK}`, borderRadius: dp(3), padding: dp(1) }}>
            <span style={{ display: "block", width: "78%", height: "100%", background: INK, borderRadius: dp(1) }} />
          </span>
        </span>
      </div>

      {/* top bar, as in ChatScreen: back arrow, title, recording name */}
      <div style={{ display: "flex", alignItems: "center", gap: dp(12), padding: `${dp(8)}px ${dp(16)}px ${dp(12)}px`, borderBottom: `${dp(0.5)}px solid ${BORDER}`, background: "#FFFFFF" }}>
        <span style={{ fontSize: dp(20), color: INK }}>←</span>
        <Img src={staticFile("court/srutam-logo.png")} style={{ width: dp(34), height: dp(34), margin: dp(-3) }} />
        <div>
          <div style={{ fontSize: dp(17), fontWeight: 700 }}>Srutam AI</div>
          <div style={{ fontSize: dp(11.5), color: MUTED, marginTop: dp(1) }}>Team call · Today, 3:00 pm</div>
        </div>
      </div>

      <div style={{ padding: `${dp(18)}px ${dp(14)}px 0`, display: "flex", flexDirection: "column", gap: dp(12) }}>
        {/* user message */}
        {sent && (
          <div style={{ display: "flex", justifyContent: "flex-end", paddingLeft: dp(48), transform: `translateY(${(1 - bubble) * dp(30)}px) scale(${0.9 + 0.1 * bubble})`, transformOrigin: "right bottom", opacity: Math.min(1, bubble * 1.5) }}>
            <div style={{ background: COBALT, color: "#fff", borderRadius: `${dp(22)}px ${dp(22)}px ${dp(8)}px ${dp(22)}px`, padding: `${dp(12)}px ${dp(14)}px`, fontSize: dp(14.5), lineHeight: 1.4 }}>{Q}</div>
          </div>
        )}

        {/* thinking */}
        {thinking && (
          <div style={{ display: "flex", paddingRight: dp(48) }}>
            <div style={{ display: "flex", alignItems: "center", gap: dp(10), background: "#E8EDF3", borderRadius: dp(22), padding: `${dp(14)}px ${dp(16)}px`, fontSize: dp(14), color: MUTED }}>
              <span style={{ width: dp(16), height: dp(16), borderRadius: 999, border: `${dp(2)}px solid #CBD5E1`, borderTopColor: COBALT, transform: `rotate(${f * 24}deg)` }} />
              Thinking...
            </div>
          </div>
        )}

        {/* answer */}
        {a > 0.01 && (
          <div style={{ display: "flex", paddingRight: dp(30), opacity: Math.min(1, a * 1.5), transform: `translateY(${(1 - a) * dp(24)}px)` }}>
            <div style={{ background: SLATE, border: `${dp(0.5)}px solid ${BORDER}`, borderRadius: `${dp(22)}px ${dp(22)}px ${dp(22)}px ${dp(8)}px`, padding: `${dp(12)}px ${dp(14)}px ${dp(14)}px`, width: "100%" }}>
              <div style={{ fontSize: dp(15), lineHeight: 1.45, fontWeight: 600 }}>{words.slice(0, shown).join(" ")}</div>
              {/* source: the recording, as a Notes card */}
              <div style={{ marginTop: dp(12), background: "#FFFFFF", border: `${dp(0.5)}px solid ${BORDER}`, borderRadius: dp(18), padding: `${dp(12)}px ${dp(12)}px ${dp(12)}px`, boxShadow: `0 ${dp(2)}px ${dp(8)}px rgba(15,23,42,0.05)` }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                  <span style={{ fontSize: dp(13), fontWeight: 700 }}>Team call</span>
                  <span style={{ fontSize: dp(10.5), fontWeight: 700, color: COBALT, background: "#DBEAFE", borderRadius: 999, padding: `${dp(3)}px ${dp(9)}px` }}>✦ Source · 14:22</span>
                </div>
                <div style={{ marginTop: dp(8), display: "flex", gap: dp(9) }}>
                  <div style={{ width: dp(3), borderRadius: dp(2), background: "#1E293B" }} />
                  <div style={{ fontSize: dp(13), lineHeight: 1.45, color: "#334155" }}>
                    “…and Aarav, send me the final deck by <span style={{ background: "#BFDBFE", color: INK, fontWeight: 700, borderRadius: dp(4), padding: `0 ${dp(3)}px` }}>tomorrow, ten a.m.</span>”
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: dp(10), marginTop: dp(12) }}>
                  <div style={{ width: dp(34), height: dp(34), borderRadius: 999, background: COBALT, display: "flex", alignItems: "center", justifyContent: "center", transform: `scale(${pressed ? 0.86 : 1})`, boxShadow: pressed ? `0 0 0 ${dp(6)}px rgba(37,99,235,0.18)` : "none", flexShrink: 0 }}>
                    {p > 0 && p < 1 ? (
                      <span style={{ display: "flex", gap: dp(3) }}>
                        <span style={{ width: dp(3), height: dp(11), background: "#fff", borderRadius: dp(1) }} />
                        <span style={{ width: dp(3), height: dp(11), background: "#fff", borderRadius: dp(1) }} />
                      </span>
                    ) : (
                      <span style={{ width: 0, height: 0, borderTop: `${dp(6)}px solid transparent`, borderBottom: `${dp(6)}px solid transparent`, borderLeft: `${dp(10)}px solid #fff`, marginLeft: dp(2) }} />
                    )}
                  </div>
                  <div style={{ flex: 1, display: "flex", alignItems: "center", gap: dp(2.2), height: dp(26) }}>
                    {WAVE.map((h, i) => (
                      <div key={i} style={{ flex: 1, height: `${h * 100}%`, borderRadius: dp(2), background: i / WAVE.length < p ? COBALT : "#CBD5E1" }} />
                    ))}
                  </div>
                  <span style={{ fontSize: dp(12.5), fontWeight: 600, color: "#334155" }}>0:{String(Math.floor(4 * p) + 22).padStart(2, "0")}</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* input bar, as ChatInputBar */}
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, background: "#FFFFFF", borderTop: `${dp(0.5)}px solid ${BORDER}`, padding: `${dp(12)}px ${dp(16)}px ${dp(22)}px`, boxShadow: `0 -${dp(2)}px ${dp(8)}px rgba(15,23,42,0.04)` }}>
        <div style={{ display: "flex", alignItems: "center", gap: dp(10) }}>
          <div style={{ flex: 1, background: SLATE, borderRadius: dp(24), padding: `${dp(14)}px ${dp(16)}px`, fontSize: dp(14.5), color: !sent && n > 0 ? INK : "#94A3B8", minHeight: dp(20) }}>
            {!sent && n > 0 ? (
              <>
                {Q.slice(0, n)}
                <span style={{ color: COBALT, opacity: Math.floor(f / 8) % 2 ? 1 : 0 }}>|</span>
              </>
            ) : (
              "Ask anything about this recording"
            )}
          </div>
          <div style={{ width: dp(44), height: dp(44), borderRadius: 999, background: !sent && n > 0 ? COBALT : "#CBD5E1", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: dp(18), transform: `scale(${f >= sentAt - 3 && f < sentAt + 2 ? 0.85 : 1})` }}>➤</div>
        </div>
      </div>
    </div>
  );
};

/** A modern phone body around the screen: thin bezel, punch-hole camera, side buttons. */
export const PhoneBody: React.FC<{ w: number; children: React.ReactNode }> = ({ w, children }) => {
  const h = w * (2400 / 1080);
  const b = w * 0.028;
  return (
    <div style={{ position: "relative", width: w + 2 * b, height: h + 2 * b }}>
      <div style={{ position: "absolute", right: -w * 0.012, top: h * 0.18, width: w * 0.014, height: h * 0.09, borderRadius: 4, background: "#1F2937" }} />
      <div style={{ position: "absolute", right: -w * 0.012, top: h * 0.3, width: w * 0.014, height: h * 0.05, borderRadius: 4, background: "#1F2937" }} />
      <div style={{ position: "absolute", inset: 0, borderRadius: w * 0.12, background: "linear-gradient(145deg, #3B4252, #0B0F1A 40%, #1F2937)", boxShadow: "0 50px 120px rgba(0,0,0,0.55), 0 0 0 2px #4B5563 inset" }} />
      <div style={{ position: "absolute", left: b, top: b, width: w, height: h, borderRadius: w * 0.1, overflow: "hidden", background: "#000" }}>
        {children}
        <div style={{ position: "absolute", left: "50%", top: w * 0.028, width: w * 0.035, height: w * 0.035, marginLeft: -w * 0.0175, borderRadius: 999, background: "#0B0F1A", boxShadow: "0 0 0 2px #1F2937" }} />
        <div style={{ position: "absolute", left: "35%", right: "35%", bottom: w * 0.012, height: w * 0.008, borderRadius: 99, background: "#0F172A", opacity: 0.6 }} />
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(115deg, rgba(255,255,255,0.10) 0%, rgba(255,255,255,0) 35%)", pointerEvents: "none" }} />
      </div>
    </div>
  );
};

// Style test for the redrawn cast: one hand-illustrated frame (1080 x 1920). 1:43 a.m.,
// Meera bolts upright in bed as "wait... where are slides 8-12??" lands. Drawn shape by
// shape in SVG, after the reference short: inked outlines with a slight hand-drawn
// wobble, a lived-in room, a muted night palette, light from the phone, full figure,
// real hands, a big readable expression.
const INK = "#1E1A24";
const SKIN = "#C98A6B";
const SKIN_SH = "#A9694F";
const HAIR = "#231B24";
const TEE = "#5F8C86";
const TEE_SH = "#4A706B";

/** A limb drawn as an outlined stroke: ink underneath, colour on top. */
const Limb: React.FC<{ d: string; w: number; fill: string }> = ({ d, w, fill }) => (
  <g fill="none" strokeLinecap="round" strokeLinejoin="round">
    <path d={d} stroke={INK} strokeWidth={w + 9} />
    <path d={d} stroke={fill} strokeWidth={w} />
  </g>
);

/** One finger lying across the back of the phone, with a knuckle crease. */
const Finger: React.FC<{ x: number; y: number; len: number; dir: 1 | -1; r?: number }> = ({ x, y, len, dir, r = 15 }) => {
  const x2 = x + dir * len;
  return (
    <g>
      <path d={`M${x} ${y} L${x2} ${y}`} stroke={INK} strokeWidth={r * 2 + 7} strokeLinecap="round" />
      <path d={`M${x} ${y} L${x2} ${y}`} stroke={SKIN} strokeWidth={r * 2} strokeLinecap="round" />
      <path d={`M${x2 - dir * 10} ${y - r + 4} q${dir * 6} ${r - 4} 0 ${2 * r - 8}`} stroke="#E8B9A2" strokeWidth={3} fill="none" opacity={0.8} />
      <path d={`M${x + dir * len * 0.45} ${y - r + 5} v${2 * r - 10}`} stroke={SKIN_SH} strokeWidth={3} opacity={0.7} />
    </g>
  );
};

/** A hand gripping the side of the phone: the heel of the palm on the outside edge,
    four fingers curled over the back (towards us), uneven like real fingers. */
const GripHandL: React.FC<{ x: number; y: number; dir: 1 | -1 }> = ({ x, y, dir }) => (
  <g>
    {/* palm and heel of the hand around the phone edge */}
    <g transform={`translate(${x} ${y}) scale(0.62) translate(${-x} ${-y})`}>
    <path
      d={`M${x - dir * 46} ${y + 150} C${x - dir * 70} ${y + 100} ${x - dir * 66} ${y + 10} ${x - dir * 40} ${y - 30} C${x - dir * 18} ${y - 52} ${x + dir * 10} ${y - 40} ${x + dir * 14} ${y - 10} L${x + dir * 18} ${y + 140} C${x + dir * 6} ${y + 172} ${x - dir * 26} ${y + 176} ${x - dir * 46} ${y + 150} Z`}
      fill={SKIN}
      stroke={INK}
      strokeWidth={5}
      strokeLinejoin="round"
    />
    <path d={`M${x - dir * 52} ${y + 40} q${-dir * 6} 40 ${dir * 4} 80`} stroke={SKIN_SH} strokeWidth={4} fill="none" />
    <Finger x={x} y={y - 12} len={50} dir={dir} r={14} />
    <Finger x={x} y={y + 24} len={60} dir={dir} r={15} />
    <Finger x={x} y={y + 62} len={56} dir={dir} r={15} />
    <Finger x={x} y={y + 98} len={42} dir={dir} r={13} />
    </g>
  </g>
);

export const TestFrame: React.FC = () => (
  <svg viewBox="0 0 1080 1920" width="100%" height="100%" style={{ display: "block" }}>
    <defs>
      <filter id="inked" x="-2%" y="-2%" width="104%" height="104%">
        <feTurbulence type="fractalNoise" baseFrequency={0.022} numOctaves={2} seed={7} />
        <feDisplacementMap in="SourceGraphic" scale={3.2} xChannelSelector="R" yChannelSelector="G" />
      </filter>
      <filter id="paper">
        <feTurbulence type="fractalNoise" baseFrequency={0.9} numOctaves={3} seed={3} />
        <feColorMatrix type="saturate" values="0" />
      </filter>
      <filter id="soft">
        <feGaussianBlur stdDeviation={18} />
      </filter>
      <linearGradient id="wall" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#3B4766" />
        <stop offset="100%" stopColor="#2E3852" />
      </linearGradient>
      <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#151B33" />
        <stop offset="100%" stopColor="#2C3560" />
      </linearGradient>
      <radialGradient id="phoneGlow" cx="50%" cy="60%" r="50%">
        <stop offset="0%" stopColor="#B9DCFF" stopOpacity={0.75} />
        <stop offset="60%" stopColor="#7FB2F0" stopOpacity={0.2} />
        <stop offset="100%" stopColor="#7FB2F0" stopOpacity={0} />
      </radialGradient>
      <radialGradient id="warm" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stopColor="#FFD08A" stopOpacity={0.9} />
        <stop offset="100%" stopColor="#FFD08A" stopOpacity={0} />
      </radialGradient>
      <radialGradient id="vig" cx="50%" cy="45%" r="75%">
        <stop offset="55%" stopColor="#000" stopOpacity={0} />
        <stop offset="100%" stopColor="#000" stopOpacity={0.55} />
      </radialGradient>
      <clipPath id="win">
        <rect x={70} y={250} width={360} height={470} />
      </clipPath>
    </defs>

    <g transform="translate(565 980) scale(1.2) translate(-565 -930)">
    <g filter="url(#inked)">
      {/* ---------- the room ---------- */}
      <rect width={1080} height={1920} fill="url(#wall)" />
      {/* skirting and floor */}
      <rect y={1690} width={1080} height={230} fill="#262D44" />
      <path d="M0 1690 H1080" stroke={INK} strokeWidth={5} />

      {/* the window: night sky, the city, the moon, a plant on the sill */}
      <g clipPath="url(#win)">
        <rect x={70} y={250} width={360} height={470} fill="url(#sky)" />
        <circle cx={340} cy={330} r={34} fill="#F3E7C2" />
        <circle cx={352} cy={322} r={30} fill="#1C2342" />
        {[
          [70, 560, 70, 160],
          [140, 500, 60, 220],
          [200, 590, 80, 130],
          [280, 470, 54, 250],
          [334, 540, 96, 180],
        ].map(([x, y, w, h], i) => (
          <g key={i}>
            <rect x={x} y={y} width={w} height={h} fill={["#222A49", "#273055", "#1F2744", "#2A335A", "#222A49"][i]} stroke={INK} strokeWidth={3} />
            {Array.from({ length: 6 }, (_, j) => (
              <rect key={j} x={x + 10 + (j % 2) * (w / 2 - 6)} y={y + 16 + Math.floor(j / 2) * 34} width={10} height={14} fill="#F2C76B" opacity={(i + j) % 3 === 0 ? 0.9 : 0.15} />
            ))}
          </g>
        ))}
      </g>
      <rect x={70} y={250} width={360} height={470} fill="none" stroke={INK} strokeWidth={8} />
      <path d="M250 250 V720 M70 485 H430" stroke="#4B5878" strokeWidth={12} />
      <path d="M250 250 V720 M70 485 H430" stroke={INK} strokeWidth={3} opacity={0.6} />
      <rect x={52} y={716} width={396} height={22} fill="#556283" stroke={INK} strokeWidth={4} />
      {/* curtain */}
      <path d="M30 230 C70 330 40 470 76 600 C92 660 70 720 96 760 L30 760 Z" fill="#7B5A86" stroke={INK} strokeWidth={5} />
      <path d="M52 300 C62 420 54 560 74 700" stroke="#5E4468" strokeWidth={4} fill="none" />
      <path d="M20 232 H470" stroke="#7A6A55" strokeWidth={10} strokeLinecap="round" />
      {/* plant on the sill */}
      <path d="M370 716 L382 668 L420 668 L432 716 Z" fill="#B0643E" stroke={INK} strokeWidth={4} />
      {[[-28, -60], [-6, -84], [18, -66], [34, -40]].map(([dx, dy], i) => (
        <path key={i} d={`M401 668 q${dx / 2} ${dy / 2 - 10} ${dx} ${dy}`} stroke="#4E8A5A" strokeWidth={9} fill="none" strokeLinecap="round" />
      ))}

      {/* fairy lights across the wall */}
      <path d="M0 150 C260 230 600 110 1080 180" stroke={INK} strokeWidth={3} fill="none" />
      {Array.from({ length: 13 }, (_, i) => {
        const u = i / 12;
        const x = u * 1080;
        const y = 150 + Math.sin(u * Math.PI) * 30 + (u < 0.3 ? u * 120 : 36 - (u - 0.3) * 30);
        return (
          <g key={i}>
            <circle cx={x} cy={y + 14} r={26} fill="url(#warm)" opacity={0.7} />
            <ellipse cx={x} cy={y + 14} rx={8} ry={11} fill={["#FFD58A", "#FFB3A0", "#FFE7A8"][i % 3]} stroke={INK} strokeWidth={2.5} />
          </g>
        );
      })}

      {/* the wall clock: 1:43 */}
      <g transform="translate(880 330)">
        <circle r={88} fill="#E9E1CF" stroke={INK} strokeWidth={7} />
        <circle r={74} fill="none" stroke="#C9BFA8" strokeWidth={3} />
        {Array.from({ length: 12 }, (_, i) => (
          <path key={i} d={`M0 -66 V-56`} stroke={INK} strokeWidth={4} transform={`rotate(${i * 30})`} />
        ))}
        <path d="M0 0 L0 -40" stroke={INK} strokeWidth={8} strokeLinecap="round" transform="rotate(51.5)" />
        <path d="M0 0 L0 -60" stroke={INK} strokeWidth={5} strokeLinecap="round" transform="rotate(258)" />
        <circle r={7} fill="#C0392B" />
      </g>

      {/* shelf with books, a mug and a cactus */}
      <rect x={600} y={560} width={440} height={20} fill="#7A5638" stroke={INK} strokeWidth={4} />
      {[
        [620, 26, 120, "#B5503C"],
        [650, 22, 104, "#3E6E8E"],
        [676, 30, 128, "#D8A548"],
        [710, 20, 96, "#6B8F5B"],
      ].map(([x, w, h, c], i) => (
        <rect key={i} x={x as number} y={560 - (h as number)} width={w as number} height={h as number} fill={c as string} stroke={INK} strokeWidth={4} />
      ))}
      <path d="M742 560 L800 452 L822 462 L766 560 Z" fill="#8E6BA8" stroke={INK} strokeWidth={4} />
      <path d="M880 560 L874 500 L930 500 L924 560 Z" fill="#EDE6D6" stroke={INK} strokeWidth={4} />
      <path d="M930 512 q24 4 0 34" stroke={INK} strokeWidth={4} fill="none" />
      <path d="M980 560 L972 520 L1020 520 L1012 560 Z" fill="#B0643E" stroke={INK} strokeWidth={4} />
      <path d="M996 520 C984 470 1010 450 1000 420 C1020 450 1024 480 1008 520 Z" fill="#5E9A62" stroke={INK} strokeWidth={4} />

      {/* corkboard with the deadline */}
      <rect x={720} y={640} width={330} height={250} fill="#B88B5A" stroke={INK} strokeWidth={6} />
      <g transform="translate(70 0) rotate(-4 740 720)">
        <rect x={670} y={665} width={150} height={120} fill="#F7D45E" stroke={INK} strokeWidth={3} />
        <text x={745} y={712} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={28} fill={INK}>
          DBMS PPT
        </text>
        <text x={745} y={752} textAnchor="middle" fontFamily="Kalam" fontWeight={700} fontSize={30} fill="#B3261E">
          9 AM!!
        </text>
      </g>
      <g transform="translate(40 0) rotate(5 920 740)">
        <rect x={850} y={680} width={140} height={170} fill="#F4F1E8" stroke={INK} strokeWidth={3} />
        {[0, 1, 2, 3, 4].map((i) => (
          <path key={i} d={`M864 ${712 + i * 26} H976`} stroke="#9AA3B5" strokeWidth={3} />
        ))}
      </g>
      <circle cx={815} cy={668} r={7} fill="#D9453B" stroke={INK} strokeWidth={2} />
      <circle cx={960} cy={684} r={7} fill="#3E7DD6" stroke={INK} strokeWidth={2} />

      {/* ---------- the bed ---------- */}
      {/* headboard */}
      <path d="M-20 880 C80 850 300 850 420 880 L420 1300 L-20 1300 Z" fill="#6E4C35" stroke={INK} strokeWidth={6} />
      <path d="M20 905 C110 885 290 885 380 905" stroke="#856049" strokeWidth={8} fill="none" />
      {/* pillow behind her */}
      <path d="M40 1000 C30 930 140 900 300 925 C420 945 470 990 455 1060 C445 1120 360 1150 230 1140 C100 1130 50 1080 40 1000 Z" fill="#DCD6E8" stroke={INK} strokeWidth={6} />
      <path d="M110 1010 C170 990 260 1000 330 1030" stroke="#B9B1CC" strokeWidth={5} fill="none" />
      {/* mattress edge and bed frame */}
      <path d="M-20 1560 L1100 1560 L1100 1700 L-20 1700 Z" fill="#7A5638" stroke={INK} strokeWidth={6} />
      <path d="M-20 1600 H1100" stroke="#5E412A" strokeWidth={5} />

      {/* ---------- Meera ---------- */}
      {/* torso in an oversized tee */}
      <path d="M528 872 C470 888 404 900 382 958 C366 1010 356 1120 352 1290 L780 1290 C776 1120 768 1010 748 958 C728 900 660 888 600 872 Z" fill={TEE} stroke={INK} strokeWidth={6} strokeLinejoin="round" />
      <path d="M400 1000 C420 1100 410 1200 404 1290" stroke={TEE_SH} strokeWidth={14} fill="none" opacity={0.8} />
      <path d="M720 990 C716 1080 726 1190 730 1290" stroke={TEE_SH} strokeWidth={14} fill="none" opacity={0.8} />
      {/* neck and collar */}
      <path d="M530 820 L530 885 C548 900 584 900 600 885 L600 820 Z" fill={SKIN_SH} stroke={INK} strokeWidth={5} />
      <path d="M520 878 C540 914 592 914 610 878" stroke={INK} strokeWidth={6} fill="none" />

      {/* arms: short sleeves, elbows out, forearms bringing the phone up to her chest */}
      <Limb d="M398 990 L372 1170" w={66} fill={SKIN} />
      <Limb d="M732 990 L758 1170" w={66} fill={SKIN} />
      <Limb d="M376 1170 Q420 1160 470 1150" w={52} fill={SKIN} />
      <Limb d="M754 1170 Q710 1160 660 1150" w={52} fill={SKIN} />
      {/* elbow creases */}
      <path d="M352 1160 q16 14 30 6" stroke={SKIN_SH} strokeWidth={4} fill="none" />
      <path d="M778 1160 q-16 14 -30 6" stroke={SKIN_SH} strokeWidth={4} fill="none" />
      {/* sleeves */}
      <path d="M392 950 C350 970 340 1040 352 1080 L428 1090 C432 1050 436 1000 440 966 Z" fill={TEE} stroke={INK} strokeWidth={6} strokeLinejoin="round" />
      <path d="M738 950 C780 970 790 1040 778 1080 L702 1090 C698 1050 694 1000 690 966 Z" fill={TEE} stroke={INK} strokeWidth={6} strokeLinejoin="round" />

      {/* the phone, held up, screen to her: light pours onto her face */}
      <ellipse cx={565} cy={860} rx={250} ry={300} fill="url(#phoneGlow)" style={{ mixBlendMode: "screen" }} />
      <g transform="rotate(-4 565 1060)">
        <rect x={490} y={908} width={150} height={300} rx={24} fill="#2A2E3D" stroke={INK} strokeWidth={6} />
        <rect x={505} y={924} width={48} height={66} rx={14} fill="#3A4054" stroke={INK} strokeWidth={3} />
        <circle cx={529} cy={944} r={10} fill="#151821" stroke={INK} strokeWidth={2} />
        <circle cx={529} cy={972} r={8} fill="#151821" />
        <path d="M490 935 V1180" stroke="#9CC9FF" strokeWidth={4} opacity={0.8} />
        <path d="M640 935 V1180" stroke="#9CC9FF" strokeWidth={4} opacity={0.8} />
        <path d="M520 910 H612" stroke="#BFE0FF" strokeWidth={5} opacity={0.9} />
        <GripHandL x={494} y={1080} dir={1} />
        <GripHandL x={636} y={1080} dir={-1} />
      </g>

      {/* head */}
      {/* bun, scrunchie and flyaways */}
      <circle cx={590} cy={448} r={72} fill={HAIR} stroke={INK} strokeWidth={5} />
      <path d="M560 420 C580 395 620 400 632 430" stroke="#3A2E3A" strokeWidth={6} fill="none" />
      <path d="M540 510 C560 496 620 496 640 510 L636 530 C616 518 564 518 544 530 Z" fill="#E36A5C" stroke={INK} strokeWidth={4} />
      <path d="M620 390 C650 350 690 360 700 330" stroke={HAIR} strokeWidth={5} fill="none" strokeLinecap="round" />
      <path d="M560 386 C540 350 560 330 545 300" stroke={HAIR} strokeWidth={5} fill="none" strokeLinecap="round" />
      <path d="M655 420 C700 410 720 440 745 430" stroke={HAIR} strokeWidth={5} fill="none" strokeLinecap="round" />
      {/* ears */}
      <ellipse cx={438} cy={706} rx={20} ry={30} fill={SKIN} stroke={INK} strokeWidth={5} />
      <ellipse cx={692} cy={706} rx={20} ry={30} fill={SKIN} stroke={INK} strokeWidth={5} />
      {/* face */}
      <path d="M444 640 C440 548 512 520 566 520 C622 520 690 548 688 640 C690 748 640 838 566 842 C492 838 440 748 444 640 Z" fill={SKIN} stroke={INK} strokeWidth={6} />
      {/* phone light on the lower face (screen blend) */}
      <path d="M452 700 C470 800 520 840 566 842 C612 840 662 800 680 700 C640 770 600 790 566 790 C530 790 490 770 452 700 Z" fill="#9CCBFF" opacity={0.22} style={{ mixBlendMode: "screen" }} />
      {/* hair: messy bangs with a parting, side locks */}
      <path d="M432 700 C420 590 470 508 566 504 C662 500 712 590 700 700 C690 640 664 600 628 590 C640 624 612 640 590 610 C576 640 534 650 518 612 C500 640 470 650 452 640 C446 660 438 680 432 700 Z" fill={HAIR} stroke={INK} strokeWidth={5} strokeLinejoin="round" />
      <path d="M436 660 C420 720 428 790 446 830 C452 790 452 730 460 690 Z" fill={HAIR} stroke={INK} strokeWidth={4} />
      <path d="M696 660 C712 720 704 790 686 830 C680 790 680 730 672 690 Z" fill={HAIR} stroke={INK} strokeWidth={4} />
      <path d="M500 560 C520 540 560 532 590 540" stroke="#3A2E3A" strokeWidth={5} fill="none" />
      {/* shadow under the bangs */}
      <path d="M470 646 C500 660 540 652 566 640 C600 656 640 660 668 646 C640 672 600 676 566 668 C530 676 494 672 470 646 Z" fill={SKIN_SH} opacity={0.55} />
      {/* eyebrows, shot right up */}
      <path d="M476 612 C492 588 524 584 548 596" stroke={INK} strokeWidth={11} fill="none" strokeLinecap="round" />
      <path d="M584 596 C608 584 640 588 656 612" stroke={INK} strokeWidth={11} fill="none" strokeLinecap="round" />
      {/* eyes: wide open, pupils pinned small */}
      <ellipse cx={512} cy={690} rx={36} ry={42} fill="#FBF7F0" stroke={INK} strokeWidth={5} />
      <ellipse cx={620} cy={690} rx={36} ry={42} fill="#FBF7F0" stroke={INK} strokeWidth={5} />
      <circle cx={514} cy={688} r={10} fill={INK} />
      <circle cx={618} cy={688} r={10} fill={INK} />
      <circle cx={518} cy={684} r={3} fill="#FFFFFF" />
      <circle cx={622} cy={684} r={3} fill="#FFFFFF" />
      {/* 1:43 a.m. under the eyes */}
      <path d="M484 742 q28 12 56 0" stroke={SKIN_SH} strokeWidth={4} fill="none" />
      <path d="M592 742 q28 12 56 0" stroke={SKIN_SH} strokeWidth={4} fill="none" />
      {/* nose */}
      <path d="M566 716 L558 760 L574 762" stroke={INK} strokeWidth={4} fill="none" strokeLinejoin="round" />
      {/* mouth: a gasp */}
      <path d="M540 792 C540 772 592 772 592 792 C592 822 540 822 540 792 Z" fill="#5A1E2A" stroke={INK} strokeWidth={5} />
      <path d="M546 786 C556 780 576 780 586 786" stroke="#F4EDE6" strokeWidth={6} fill="none" />
      <path d="M552 810 C560 802 574 802 582 810" stroke="#C4545E" strokeWidth={6} fill="none" />
      {/* blush and a sweat drop */}
      <ellipse cx={482} cy={762} rx={26} ry={12} fill="#E07A6A" opacity={0.45} />
      <ellipse cx={650} cy={762} rx={26} ry={12} fill="#E07A6A" opacity={0.45} />
      <path d="M712 600 C724 624 728 642 712 650 C696 642 700 624 712 600 Z" fill="#BFE3FF" stroke={INK} strokeWidth={4} />
      {/* shock lines */}
      {[-150, -120, -90, -60, -30].map((a) => (
        <path key={a} d={`M0 -250 L0 -290`} stroke={INK} strokeWidth={6} strokeLinecap="round" transform={`translate(566 690) rotate(${a + 90})`} />
      ))}

      {/* ---------- the blanket over her legs, knees up ---------- */}
      <path d="M260 1262 C400 1238 700 1236 840 1256 C900 1200 1000 1210 1040 1270 C1080 1320 1100 1360 1100 1420 L1100 1580 C900 1610 300 1615 -20 1585 L-20 1300 C80 1278 170 1268 260 1262 Z" fill="#C98F4A" stroke={INK} strokeWidth={6} strokeLinejoin="round" />
      {[0, 1, 2, 3, 4, 5, 6].map((i) => (
        <path key={i} d={`M${-20 + i * 170} 1300 C${30 + i * 170} 1400 ${10 + i * 170} 1500 ${40 + i * 170} 1590`} stroke="#B07838" strokeWidth={10} fill="none" opacity={0.7} />
      ))}
      <path d="M300 1330 C380 1300 450 1320 520 1300" stroke={INK} strokeWidth={4} fill="none" />
      <path d="M600 1350 C680 1320 760 1340 800 1300" stroke={INK} strokeWidth={4} fill="none" />
      <path d="M860 1280 C900 1330 960 1340 1000 1320" stroke={INK} strokeWidth={4} fill="none" />
      <path d="M120 1420 C220 1400 300 1440 380 1420" stroke={INK} strokeWidth={4} fill="none" />

      {/* ---------- the floor ---------- */}
      <ellipse cx={560} cy={1820} rx={420} ry={60} fill="#3A4462" stroke={INK} strokeWidth={4} />
      {/* slippers */}
      <path d="M150 1790 C150 1760 230 1760 240 1790 C246 1810 160 1816 150 1790 Z" fill="#E36A5C" stroke={INK} strokeWidth={4} />
      <path d="M230 1830 C232 1800 310 1802 318 1832 C320 1852 236 1856 230 1830 Z" fill="#E36A5C" stroke={INK} strokeWidth={4} />
      {/* a chai glass and a charger cable */}
      <path d="M840 1800 L850 1730 L890 1730 L900 1800 Z" fill="#DCEBF5" opacity={0.85} stroke={INK} strokeWidth={4} />
      <path d="M845 1790 L897 1790 L900 1800 L840 1800 Z" fill="#A5672F" />
      <path d="M700 1720 C640 1760 760 1790 690 1840 C640 1874 740 1890 760 1920" stroke="#E7E4DD" strokeWidth={6} fill="none" />
    </g>
    </g>

    {/* the message, popping out of the phone (outside the wobble so it reads cleanly) */}
    <g transform="rotate(3 830 1010)">
      <path d="M690 960 C690 930 712 912 742 912 L986 912 C1016 912 1036 932 1036 962 L1036 1046 C1036 1074 1016 1094 986 1094 L780 1094 L706 1140 L728 1094 C706 1092 690 1072 690 1046 Z" fill="#FFFDF7" stroke={INK} strokeWidth={6} strokeLinejoin="round" />
      <text x={712} y={962} fontFamily="Inter" fontWeight={800} fontSize={28} fill="#DB2777">
        Riya
      </text>
      <text fontFamily="Kalam" fontWeight={700} fontSize={40} fill={INK}>
        <tspan x={712} y={1010}>wait… where are</tspan>
        <tspan x={712} y={1058}>slides 8–12??</tspan>
      </text>
    </g>

    {/* light, grain and vignette */}
    <rect width={1080} height={1920} fill="#1B2240" opacity={0.18} style={{ mixBlendMode: "multiply" }} />
    <rect width={1080} height={1920} filter="url(#paper)" opacity={0.12} style={{ mixBlendMode: "overlay" }} />
    <rect width={1080} height={1920} fill="url(#vig)" />
  </svg>
);

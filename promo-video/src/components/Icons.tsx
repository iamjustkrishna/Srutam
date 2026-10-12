// Minimal line icons drawn to match the app's Material icons.
type P = { size: number; color?: string };

export const MicIcon: React.FC<P> = ({ size, color = "#FFFFFF" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <rect x={9} y={3} width={6} height={11} rx={3} fill={color} />
    <path
      d="M6 11a6 6 0 0 0 12 0M12 17v4"
      stroke={color}
      strokeWidth={2}
      fill="none"
      strokeLinecap="round"
    />
  </svg>
);

export const PauseIcon: React.FC<P> = ({ size, color = "#FFFFFF" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <rect x={6} y={5} width={4} height={14} rx={1} fill={color} />
    <rect x={14} y={5} width={4} height={14} rx={1} fill={color} />
  </svg>
);

export const StopIcon: React.FC<P> = ({ size, color = "#FFFFFF" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <rect x={6} y={6} width={12} height={12} rx={2} fill={color} />
  </svg>
);

export const CloseIcon: React.FC<P> = ({ size, color = "#FFFFFF" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <path
      d="M6 6l12 12M18 6L6 18"
      stroke={color}
      strokeWidth={2.2}
      strokeLinecap="round"
    />
  </svg>
);

export const LockIcon: React.FC<P> = ({ size, color = "#0D9488" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <rect x={5} y={10} width={14} height={11} rx={2} fill={color} />
    <path
      d="M8 10V7a4 4 0 0 1 8 0v3"
      stroke={color}
      strokeWidth={2.2}
      fill="none"
    />
  </svg>
);

export const WaveIcon: React.FC<P> = ({ size, color = "#2563EB" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    {[4, 8, 12, 16, 20].map((x, i) => {
      const h = [6, 12, 16, 10, 6][i];
      return (
        <rect
          key={x}
          x={x - 1}
          y={12 - h / 2}
          width={2}
          height={h}
          rx={1}
          fill={color}
        />
      );
    })}
  </svg>
);

export const CheckIcon: React.FC<P> = ({ size, color = "#FFFFFF" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <path
      d="M5 12.5l4.5 4.5L19 7.5"
      stroke={color}
      strokeWidth={2.6}
      fill="none"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

export const BellIcon: React.FC<P> = ({ size, color = "#2563EB" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z" fill={color} />
    <circle cx={12} cy={20.5} r={1.8} fill={color} />
  </svg>
);

export const BulbIcon: React.FC<P> = ({ size, color = "#D97706" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <path d="M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z" fill={color} />
    <rect x={9} y={17.5} width={6} height={2.5} rx={1} fill={color} />
  </svg>
);

export const SparkleIcon: React.FC<P> = ({ size, color = "#2563EB" }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <path
      d="M12 2l2.2 7.8L22 12l-7.8 2.2L12 22l-2.2-7.8L2 12l7.8-2.2z"
      fill={color}
    />
  </svg>
);

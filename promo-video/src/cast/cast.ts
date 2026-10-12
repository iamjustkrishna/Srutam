// The supporting cast for the next two story videos (and anything after).
import type { ComponentProps } from "react";
import type { Person } from "../storyboards/People";

type Look = Omit<ComponentProps<typeof Person>, "x" | "y" | "s" | "expr">;

/** Mr. Rao: the boss. Bald, beard, glasses, navy blazer. */
export const RAO: Look = { hair: "bald", outfit: "blazer", hairColor: "#3B2F2F", skin: "#C98B62", cloth: "#BFDBFE", glasses: true, beard: true, moustache: true, face: "square", nose: "broad", eyeScale: 0.8, bw: 1.12, pocketSquare: true, iris: "#4A3428" };
/** Arjun: sales, eager, always "on it". */
export const ARJUN: Look = { hair: "short", outfit: "shirt", hairColor: "#1F1512", skin: "#B87A55", cloth: "#2563EB", face: "oval", nose: "straight", eyeScale: 0.88, bw: 1.04 };
/** Neha: design lead, side-parted hair, mustard kurta-shirt. */
export const NEHA: Look = { hair: "side", outfit: "kurta", hairColor: "#3A2418", skin: "#E3A982", cloth: "#D97706", accent: "#FDE68A", face: "heart", nose: "straight", eyeScale: 0.94, earrings: true, bindi: true, iris: "#7A5236" };
/** Prof. Iyer: economics, grey curls, kurta, reading glasses. */
export const PROF: Look = { hair: "curly", outfit: "kurta", hairColor: "#A8A29E", skin: "#C98B62", cloth: "#E7E5E4", accent: "#92400E", glasses: true, aged: true, moustache: true, face: "long", nose: "broad", eyeScale: 0.78, bw: 1.02, vest: "#7C5134" };
/** Riya: hostel friend, pink hoodie, ponytail. */
export const RIYA: Look = { hair: "ponytail", outfit: "hoodie", hairColor: "#1F1512", skin: "#D9A07C", cloth: "#EC4899", accent: "#FDF2F8", face: "round", nose: "button", eyeScale: 1, bw: 0.96 };
/** Kabir: hostel friend, curly hair, green hoodie. */
export const KABIR: Look = { hair: "curly", outfit: "hoodie", hairColor: "#1F1512", skin: "#B87A55", cloth: "#16A34A", accent: "#ECFDF5", face: "square", nose: "straight", eyeScale: 0.88, bw: 1.06, stubble: true, headphones: true };

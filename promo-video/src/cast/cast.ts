// The supporting cast for the next two story videos (and anything after).
import type { ComponentProps } from "react";
import type { Person } from "../storyboards/People";

type Look = Omit<ComponentProps<typeof Person>, "x" | "y" | "s" | "expr">;

/** Mr. Rao: the boss. Bald, beard, glasses, navy blazer. */
export const RAO: Look = { hair: "bald", outfit: "blazer", hairColor: "#3B2F2F", skin: "#C98B62", cloth: "#BFDBFE", glasses: true, beard: true, moustache: true };
/** Arjun: sales, eager, always "on it". */
export const ARJUN: Look = { hair: "short", outfit: "shirt", hairColor: "#1F1512", skin: "#B87A55", cloth: "#2563EB" };
/** Neha: design lead, side-parted hair, mustard kurta-shirt. */
export const NEHA: Look = { hair: "side", outfit: "kurta", hairColor: "#3A2418", skin: "#E3A982", cloth: "#D97706", accent: "#FDE68A" };
/** Prof. Iyer: economics, grey curls, kurta, reading glasses. */
export const PROF: Look = { hair: "curly", outfit: "kurta", hairColor: "#A8A29E", skin: "#C98B62", cloth: "#E7E5E4", accent: "#92400E", glasses: true, aged: true, moustache: true };
/** Riya: hostel friend, pink hoodie, ponytail. */
export const RIYA: Look = { hair: "ponytail", outfit: "hoodie", hairColor: "#1F1512", skin: "#D9A07C", cloth: "#EC4899", accent: "#FDF2F8" };
/** Kabir: hostel friend, curly hair, green hoodie. */
export const KABIR: Look = { hair: "curly", outfit: "hoodie", hairColor: "#1F1512", skin: "#B87A55", cloth: "#16A34A", accent: "#ECFDF5" };

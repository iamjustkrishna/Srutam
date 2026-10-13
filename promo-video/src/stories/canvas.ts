// The canvas a scene draws on. Vertical cuts are 1080 x 1920 (the default); the
// landscape cuts wrap their scenes in a 1920 x 1080 canvas so Frame, Camera, Grade
// and LightLeak size themselves to it.
import { createContext, useContext } from "react";

export const CanvasCtx = createContext<{ w: number; h: number }>({ w: 1080, h: 1920 });

export const useCanvas = () => useContext(CanvasCtx);

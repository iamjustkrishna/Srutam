// Lets a vertical video know it is being reframed into a landscape cut, so it
// leaves its burned-in captions out (the landscape cut sets its own).
import { createContext, useContext } from "react";

export const FrameCtx = createContext<{ wide: boolean }>({ wide: false });

export const useWide = () => useContext(FrameCtx).wide;

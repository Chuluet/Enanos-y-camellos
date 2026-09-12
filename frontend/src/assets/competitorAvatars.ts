import type { CompetitorType } from "../api/types";
import camelMascot from "./camel-mascot.png";
import dwarfAvatar from "./dwarf-avatar.jpg";
import groserias from "./TOP10GROSERIASVENESOLANAS.jpeg"
import other from "./other.jpg"

// Swap MEDIUM/OTHER for their own art here once you have it —
// nothing else in the app needs to change.
export const COMPETITOR_AVATAR: Record<CompetitorType, string> = {
    CAMEL: camelMascot,
    DWARF: dwarfAvatar,
    MEDIUM: groserias,
    OTHER: other,
};
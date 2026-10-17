# Compare Bundle — Handover Notes

## Dark Theme Diff Colors

### Problem

The diff highlight colors in the Compare editor (both 2-way compare and unified diff) have
poor contrast in Eclipse Dark Theme. The issue was reported in
[eclipse-platform#2993](https://github.com/eclipse-platform/eclipse.platform/issues/2993).

### Color Architecture

Both the **2-way compare** (`TextMergeViewer`) and the **unified diff code mining**
(`UnifiedDiffCodeMiningProvider`) derive their diff background colors from two base colors
registered in the JFace color registry:

| Registry key    | Default value     | Configured via                          |
|-----------------|-------------------|-----------------------------------------|
| `ADDITION_COLOR` | `COLOR_DARK_GREEN` | Eclipse Preferences → Colors and Fonts |
| `DELETION_COLOR` | `COLOR_RED`        | Eclipse Preferences → Colors and Fonts |

These base colors are intentionally **not used directly** as background colors. Instead,
`TextMergeViewer.ColorPalette` derives three tints by interpolating the base color toward
the editor background:

```
normal   = interpolate(base, background, scale_normal)   // stroke/border
fill     = interpolate(base, background, scale_fill)     // line background
textFill = interpolate(base, background, scale_textFill) // inline text highlight
```

`UnifiedDiffCodeMiningProvider` uses the same interpolation for the code mining (overlay)
path. The annotation painter path reads final colors directly from the preference store
keys `inlineDiffColor` / `deletionInlineDiffColor` etc., which are overridden per-theme
via `resources/css/dark.css`.

### The Dark Theme Problem

With a light theme background (~`255,255,255`) the interpolation at scale 0.9 produces a
soft pastel tint — clearly visible. With a dark theme background (~`30,31,34`) the same
scale produces a color nearly indistinguishable from the background. For example:

- `DELETION_COLOR` = `COLOR_RED` = `(255,0,0)`, dark bg = `(30,31,34)`
- fill at scale 0.9 → `(52,27,30)` — very dark, barely visible

It is **not possible** to reach a well-contrasted dark theme color by only changing the
base color (`ADDITION_COLOR` / `DELETION_COLOR`), because the 0.9 scale washes out any
base color. Working backwards, even pure `(255,0,0)` only yields `(52,27,30)` as the fill.

### Solution

We preserve the existing user-configurable color architecture (users set `ADDITION_COLOR`
/ `DELETION_COLOR` in Preferences and the three tints are derived from those). To improve
dark theme contrast, the **scale factors are reduced when the dark theme is active**:

| Theme | `normal` scale | `fill` scale | `detail` scale |
|-------|---------------|-------------|----------------|
| Light | 0.6           | 0.9         | 0.8            |
| Dark  | 0.3           | 0.79        | 0.5            |

The scales are centralised in three static methods `normalScale(boolean dark)`,
`fillScale(boolean dark)` and `detailScale(boolean dark)` on
`UnifiedDiffCodeMiningProvider`, which all three painting paths call.

Dark theme detection uses `INFORMATION_BACKGROUND_COLOR` from the theme's color registry
as a proxy (sum of RGB channels < 3×128 = dark).

Note: `ADDITION_COLOR` and `DELETION_COLOR` are **JFace color registry keys**, not
preference store keys. They cannot be overridden via the `IEclipsePreferences` CSS
mechanism in `e4-dark_preferencestyle.css` — that mechanism only affects preference store
entries. The default values `COLOR_DARK_GREEN=(0,128,0)` and `COLOR_RED=(255,0,0)` are
therefore always the base colors fed into the interpolation.

With the reduced dark scale factors these produce:

| Color | Target (Option C) | Produced |
|-------|-------------------|---------|
| Addition fill   | `(13,59,13)`  | `(23,51,26)`  |
| Addition detail | `(20,82,20)`  | `(15,79,17)`  |
| Deletion fill   | `(59,13,13)`  | `(77,24,26)`  |
| Deletion detail | `(82,20,20)`  | `(142,15,17)` |

The annotation painter path (`dark.css`) is updated to match the same computed values so
that both rendering paths produce consistent colors.

### Files Changed

| File | Change |
|------|--------|
| `compare/org/eclipse/compare/unifieddiff/internal/UnifiedDiffManager.java` | `UnifiedDiffPaintListener` uses dark-aware scale factor |
| `compare/org/eclipse/compare/contentmergeviewer/TextMergeViewer.java` | `ColorPalette` uses dark-aware scale factors |
| `compare/org/eclipse/compare/unifieddiff/internal/UnifiedDiffCodeMiningProvider.java` | Code mining path uses dark-aware scale factors; `isDarkTheme()` helper added |
| `resources/css/dark.css` | Annotation painter colors updated to match computed values |

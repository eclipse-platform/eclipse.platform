# Fix: Terminal character clipping on Windows after Eclipse 2024-06 update

## Root cause

Eclipse 2024-06 (SWT 4.32) switched the Windows text rendering backend from
**GDI to Direct2D (D2D)** by default. D2D uses sub-pixel precision for font
glyph positioning:

- **GDI (old):** `gc.drawString("ABCDEF", x, y)` places each glyph at integer
  positions that exactly match `charWidth * index`. A monospace font is truly
  grid-aligned.
- **D2D (new):** `gc.drawString("ABCDEF", x, y)` uses fractional glyph advances
  internally. The total rendered width of a multi-character string can be
  **1–2 pixels wider** than `charWidth * length`, because sub-pixel positions
  accumulate differently than integer multiplication.

In `TextLineRenderer.drawText()`, the non-proportional (monospace) branch draws
each text segment as a single string:

```java
gc.drawString(text, x + offset, y, false);
```

With D2D, this string is rendered slightly wider than the integer grid expects.
Each character's glyph bleeds 1–2 pixels into the next grid cell. The next
segment's background fill then overwrites that overflow — clipping the rightmost
pixel columns of each glyph, making text look broken.

**Note:** The `StyleMap.updateFont()` fractional-pixel check (Bug 475422) was
originally written for macOS Retina. With D2D it now also triggers on Windows,
but only for the string-length comparison — individual character widths still
match for monospace fonts like Consolas, so `fProportional` may remain `false`.

### Quick workaround (not a fix)

Adding `-Dswt.disableD2D=true` to the SolutionCenter `.ini` file reverts to
GDI rendering and makes the symptom disappear. This is not a proper solution.

---

## Fix 1 — TextLineRenderer.java

**File:**
`bundles/org.eclipse.terminal.control/src/org/eclipse/terminal/internal/textcanvas/TextLineRenderer.java`

In the `drawText` method, replace **only** the `else` branch (non-proportional path).
Leave the `if (fStyleMap.isFontProportional())` branch completely unchanged.

### Remove this else branch:

```java
		} else {
			text = text.replace('\000', ' ');
			gc.drawString(text, x + offset, y, false);
		}
```

### Replace with:

```java
		} else {
			// Draw each character individually aligned to its grid cell.
			// Since Eclipse 2024-06, SWT on Windows uses Direct2D by default.
			// D2D uses sub-pixel glyph advances, so drawing a whole string at
			// once makes it render 1-2 pixels wider than charWidth * length.
			// By drawing each character at its exact integer grid position we
			// avoid the accumulated sub-pixel drift.
			int cellWidth = getCellWidth();
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == '\000') {
					c = ' ';
				}
				gc.drawString(String.valueOf(c), x + offset + i * cellWidth, y, false);
			}
		}
```

---

## Fix 2 — ConsoleUIPlugin.java (cleanup)

**File:**
`plugins/at.bachmann.ui.console/src/at/bachmann/ui/console/ConsoleUIPlugin.java`

Remove the unused dead-code line left over from a debugging session:

```java
		Font myFont = JFaceResources.getFontRegistry().get(ITerminalConstants.FONT_DEFINITION);
```

Remove the now-unused import:

```java
import org.eclipse.swt.graphics.Font;
```
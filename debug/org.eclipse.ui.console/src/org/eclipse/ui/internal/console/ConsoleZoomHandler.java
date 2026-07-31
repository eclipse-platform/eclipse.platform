/*******************************************************************************
 * Copyright (c) 2026 Advantest Europe GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 				Raghunandana Murthappa
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Command handler to increase or decrease the font size of the console
 * currently shown in the active console view.
 * <p>
 * Zoom is tracked per {@link IConsole#getType() console type} (since types can
 * have different natural font sizes) and applied to every registered console
 * of that type, as well as to ones added later. Font changes coming from
 * elsewhere (e.g. a preference page) are never reverted; they are instead
 * adopted as the new base font, resetting the zoom delta to zero.
 * </p>
 * <p>
 * This single class backs both the zoom-in and zoom-out commands: the
 * direction is supplied as executable extension data in {@code plugin.xml},
 * e.g. {@code class="...ConsoleZoomHandler:-1"} for zooming out. Each
 * {@code <handler>} registration gets its own instance, so there is no shared
 * mutable state between the two directions.
 * </p>
 */
public class ConsoleZoomHandler extends AbstractHandler implements IExecutableExtension {

	/**
	 * Remember the custom fonts created for each console, so they can be disposed.
	 */
	private static final Map<TextConsole, List<Font>> fontsMap = new HashMap<>();

	/**
	 * Key used to remember, on the console itself, that a mismatching font change
	 * was already reasserted once for it; see {@link #onFontChanged(TextConsole)}.
	 */
	private static final String REENFORCED_ATTRIBUTE = ConsoleZoomHandler.class.getName() + ".reenforced"; //$NON-NLS-1$

	/**
	 * The smallest font height, in points, a console can be zoomed out to.
	 */
	public static final int MIN_FONT_SIZE = 6;

	/**
	 * The largest font height, in points, a console can be zoomed in to.
	 */
	public static final int MAX_FONT_SIZE = 72;

	/**
	 * The zoom direction/step size for this particular handler instance, in points;
	 * positive to zoom in, negative to zoom out. Set from the executable extension
	 * data configured in {@code plugin.xml} (defaults is zoom in).
	 */
	private int step = 1;

	/**
	 * Key used, in the per-type persisted zoom string, for consoles that report a
	 * <code>null</code> {@link IConsole#getType() type}.
	 */
	private static final String DEFAULT_TYPE_KEY = "$default$"; //$NON-NLS-1$

	private static final String TYPE_ENTRY_SEPARATOR = ":"; //$NON-NLS-1$
	private static final String TYPE_VALUE_SEPARATOR = "="; //$NON-NLS-1$
	private static final String BASE_DELTA_SEPARATOR = "|"; //$NON-NLS-1$

	/**
	 * Preference key under which the per console type zoom state is persisted,
	 * as <code>type1=base1|delta1:type2=base2|delta2</code>. There is
	 * intentionally no preference page for this.
	 */
	public static final String PREF_ZOOM_FONT_HEIGHTS = ConsolePlugin.getUniqueIdentifier() + ".zoomFontHeights"; //$NON-NLS-1$

	/**
	 * The base (natural, un-zoomed) font height and the zoom delta currently
	 * applied on top of it, for consoles of a given type.
	 */
	private record ZoomState(int base, int delta) {
		int height() {
			return base + delta;
		}
	}

	/**
	 * Shared zoom bookkeeping, keyed by {@link IConsole#getType() console type}.
	 * A missing entry means that type has never been zoomed.
	 */
	private static final Map<String, ZoomState> sZoomByType = new ConcurrentHashMap<>();

	/**
	 * Re-applies the expected zoom whenever a console's font changes after the
	 * console was added, unless the new height doesn't match what we expect - in
	 * which case it's an external change, so it's adopted as the new base font
	 * instead of being reverted; see {@link #onFontChanged(TextConsole)}.
	 */
	private static final IPropertyChangeListener FONT_ENFORCER = event -> {
		if (!IConsoleConstants.P_FONT.equals(event.getProperty())) {
			return;
		}
		if (event.getSource() instanceof TextConsole textConsole) {
			onFontChanged(textConsole);
		}
	};

	/**
	 * Applies the current zoom to newly added consoles and attaches
	 * {@link #FONT_ENFORCER} to them, and disposes each console's custom zoom
	 * font when it is removed.
	 */
	private static final IConsoleListener ZOOM_FONT_LISTENER = new IConsoleListener() {
		@Override
		public void consolesAdded(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (console instanceof TextConsole textConsole) {
					textConsole.addPropertyChangeListener(FONT_ENFORCER);
					ZoomState state = sZoomByType.get(typeKey(textConsole));
					if (state != null) {
						Display.getDefault().asyncExec(() -> applyHeight(textConsole, state.height()));
					}
				}
			}
		}

		@Override
		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (console instanceof TextConsole textConsole) {
					textConsole.removePropertyChangeListener(FONT_ENFORCER);
					Display.getDefault().asyncExec(() -> disposeZoomFonts(textConsole));
				}
			}
		}
	};

	/**
	 * Registers {@link #ZOOM_FONT_LISTENER} and loads any persisted zoom state.
	 * Must be called with the manager directly (not via
	 * {@link ConsolePlugin#getConsoleManager()}), since this runs from
	 * {@code ConsoleManager}'s own constructor.
	 *
	 * @param consoleManager the console manager being initialized
	 */
	public static void startup(IConsoleManager consoleManager) {
		consoleManager.addConsoleListener(ZOOM_FONT_LISTENER);
		sZoomByType.putAll(loadPersistedZoomStates());
	}

	/**
	 * Returns the key under which zoom state for the given console's type is tracked.
	 *
	 * @param console the console
	 * @return the console's type, or {@link #DEFAULT_TYPE_KEY} if it has none
	 */
	private static String typeKey(IConsole console) {
		String type = console.getType();
		return type != null ? type : DEFAULT_TYPE_KEY;
	}

	/**
	 * Reacts to a font change on the given console: matching the expected zoom
	 * height is a no-op. A mismatch is reasserted (zoom re-applied) the first
	 * time it occurs for that console instance - since consoles like
	 * {@code ProcessConsole} set their own font asynchronously right after being
	 * added, which would otherwise silently undo the zoom just applied to them.
	 * Any further mismatch afterwards is a genuine external change (e.g. a
	 * preference page), so it is adopted as the new base font instead (delta
	 * reset to 0).
	 *
	 * @param textConsole the console whose font changed
	 */
	private static void onFontChanged(TextConsole textConsole) {
		Integer currentHeight = getFontHeight(textConsole);
		if (currentHeight == null) {
			return;
		}
		String type = typeKey(textConsole);
		ZoomState state = sZoomByType.get(type);
		if (state != null) {
			if (state.height() == currentHeight.intValue()) {
				return;
			}
			if (!Boolean.TRUE.equals(textConsole.getAttribute(REENFORCED_ATTRIBUTE))) {
				textConsole.setAttribute(REENFORCED_ATTRIBUTE, Boolean.TRUE);
				applyHeight(textConsole, state.height());
				return;
			}
		}
		sZoomByType.put(type, new ZoomState(currentHeight.intValue(), 0));
		persistZoomStates();
	}

	/**
	 * Loads the zoom state persisted from a previous session, if any.
	 *
	 * @return the persisted per-type zoom state; empty if none or unparsable
	 */
	private static Map<String, ZoomState> loadPersistedZoomStates() {
		String persisted = getPreferences().get(PREF_ZOOM_FONT_HEIGHTS, ""); //$NON-NLS-1$
		Map<String, ZoomState> result = new HashMap<>();
		if (persisted.isEmpty()) {
			return result;
		}
		for (String entry : persisted.split(TYPE_ENTRY_SEPARATOR)) {
			int eq = entry.indexOf(TYPE_VALUE_SEPARATOR);
			int bar = entry.indexOf(BASE_DELTA_SEPARATOR);
			if (eq <= 0 || bar < eq) {
				continue;
			}
			try {
				String type = entry.substring(0, eq);
				int base = Integer.parseInt(entry.substring(eq + 1, bar));
				int delta = Integer.parseInt(entry.substring(bar + 1));
				int height = clamp(base + delta);
				result.put(type, new ZoomState(base, height - base));
			} catch (NumberFormatException e) {
				// ignore malformed entry
			}
		}
		return result;
	}

	/**
	 * Persists the current per-type zoom state.
	 */
	private static void persistZoomStates() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, ZoomState> entry : sZoomByType.entrySet()) {
			if (sb.length() > 0) {
				sb.append(TYPE_ENTRY_SEPARATOR);
			}
			ZoomState state = entry.getValue();
			sb.append(entry.getKey()).append(TYPE_VALUE_SEPARATOR).append(state.base())
					.append(BASE_DELTA_SEPARATOR).append(state.delta());
		}
		IEclipsePreferences preferences = getPreferences();
		preferences.put(PREF_ZOOM_FONT_HEIGHTS, sb.toString());
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			ConsolePlugin.log(e);
		}
	}

	private static IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(ConsolePlugin.getUniqueIdentifier());
	}

	private static int clamp(int height) {
		return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, height));
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		if (data instanceof String stepData) {
			try {
				step = Integer.parseInt(stepData.trim());
			} catch (NumberFormatException e) {
				// keep default (zoom in) if the configured data is not a valid integer
			}
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		applyZoom(part, step);
		return Status.OK_STATUS;
	}

	/**
	 * Applies a font zoom step to every registered console of the same type as
	 * the one in the given console view, and remembers the new zoom level for
	 * that type. Must run on the UI thread; re-dispatches itself otherwise.
	 *
	 * @param part  the active part; must be an {@link IConsoleView} for anything to happen
	 * @param delta the font size delta to apply, in points
	 */
	public static void applyZoom(IWorkbenchPart part, int delta) {
		if (!(part instanceof IConsoleView consoleView)) {
			return;
		}
		IConsole console = consoleView.getConsole();
		if (!(console instanceof TextConsole textConsole)) {
			return;
		}

		String type = typeKey(textConsole);
		ZoomState currentState = sZoomByType.get(type);
		int base;
		int currentDelta;
		if (currentState != null) {
			base = currentState.base();
			currentDelta = currentState.delta();
		} else {
			Integer currentHeight = getFontHeight(textConsole);
			if (currentHeight == null) {
				return;
			}
			base = currentHeight.intValue();
			currentDelta = 0;
		}

		int newHeight = clamp(base + currentDelta + delta);
		int newDelta = newHeight - base;
		if (currentState != null && newDelta == currentState.delta()) {
			return;
		}
		sZoomByType.put(type, new ZoomState(base, newDelta));
		persistZoomStates();

		IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
		for (IConsole registered : consoles) {
			if (registered instanceof TextConsole registeredTextConsole && type.equals(typeKey(registeredTextConsole))) {
				applyHeight(registeredTextConsole, newHeight);
			}
		}
	}

	/**
	 * Returns the current font height (in points) of the given console, or
	 * <code>null</code> if it cannot be determined. Must run on the UI thread.
	 *
	 * @param textConsole the console
	 * @return the font height in points, or <code>null</code>
	 */
	private static Integer getFontHeight(TextConsole textConsole) {
		Font font = textConsole.getFont();
		if (font == null || font.isDisposed()) {
			return null;
		}
		FontData[] fontData = font.getFontData();
		if (fontData == null || fontData.length == 0) {
			return null;
		}
		return Integer.valueOf(fontData[0].getHeight());
	}

	/**
	 * Applies the given font height to the console, preserving its font
	 * family/style, and disposes the previous zoom font. Must run on the UI
	 * thread; re-dispatches itself otherwise.
	 *
	 * @param textConsole the console to update
	 * @param height      the font height to apply, in points
	 */
	private static void applyHeight(TextConsole textConsole, int height) {
		// make sure this console's font is (still) being watched, in case it was
		// registered before the zoom handler class got loaded, or the listener
		// was otherwise not yet attached
		textConsole.addPropertyChangeListener(FONT_ENFORCER);

		Font currentFont = textConsole.getFont();
		if (currentFont == null || currentFont.isDisposed()) {
			return;
		}
		FontData[] fontData = currentFont.getFontData();
		if (fontData == null || fontData.length == 0 || fontData[0].getHeight() == height) {
			return;
		}
		// getFontData() already returns a fresh array/copy that is safe to mutate;
		// only the height is changed so that every other attribute (including any
		// platform-specific data) is preserved as-is
		for (FontData fd : fontData) {
			fd.setHeight(height);
		}

		fontsMap.compute(textConsole, (console, oldZoomFonts) -> {
			Font newZoomFont = new Font(currentFont.getDevice(), fontData);
			textConsole.setFont(newZoomFont);
			List<Font> oldFonts = oldZoomFonts;
			if (oldFonts == null) {
				oldFonts = new ArrayList<>();
			}
			oldFonts.add(newZoomFont);
			return oldFonts;
		});
	}

	/**
	 * Disposes all custom fonts after console is removed on a later UI cycle rather
	 * than immediately.
	 * <p>
	 * A font that was just replaced on a console (e.g. via
	 * {@link TextConsole#setFont(Font)}) may still be referenced for a little while
	 * by the viewer's internal rendering caches (e.g.
	 * {@code StyledText}/{@code TextLayout} keep per-line layouts that are only
	 * refreshed on their next repaint). Disposing it synchronously can therefore
	 * cause a later, asynchronously dispatched repaint to fail with an
	 * {@code IllegalArgumentException} ("Argument not valid") when it tries to use
	 * the now-disposed font. Deferring the actual disposal by one UI cycle gives
	 * any such pending repaint a chance to pick up the new font first.
	 * </p>
	 *
	 * @param textConsole the console whose zoom fonts should be disposed
	 */
	private static void disposeZoomFonts(TextConsole textConsole) {
		List<Font> oldFonts = fontsMap.remove(textConsole);
		if (oldFonts != null && !oldFonts.isEmpty()) {
			Display.getDefault().timerExec(100, () -> {
				for (Font font : oldFonts) {
					if (font != null && !font.isDisposed()) {
						font.dispose();
					}
				}
			});
		}
	}
}


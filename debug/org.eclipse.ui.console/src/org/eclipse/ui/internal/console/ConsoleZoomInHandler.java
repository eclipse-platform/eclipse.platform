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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
 * Command handler to increase the font size of the console currently shown in
 * the active console view.
 * <p>
 * Zoom is tracked per {@link IConsole#getType() console type} (since types can
 * have different natural font sizes) and applied to every registered console
 * of that type, as well as to ones added later. Font changes coming from
 * elsewhere (e.g. a preference page) are never reverted; they are instead
 * adopted as the new base font, resetting the zoom delta to zero.
 * </p>
 */
public class ConsoleZoomInHandler extends AbstractHandler {

	/**
	 * Key used to remember, on the console itself, the custom font created for
	 * zooming, so it can be reused/replaced and eventually disposed.
	 */
	static final String ZOOM_FONT_ATTRIBUTE = ConsoleZoomInHandler.class.getName() + ".zoomFont"; //$NON-NLS-1$

	/**
	 * Key used to remember, on the console itself, that a mismatching font change
	 * was already reasserted once for it; see {@link #onFontChanged(TextConsole)}.
	 */
	private static final String REENFORCED_ATTRIBUTE = ConsoleZoomInHandler.class.getName() + ".reenforced"; //$NON-NLS-1$

	private static final int MIN_FONT_SIZE = 6;
	private static final int MAX_FONT_SIZE = 72;
	private static final int STEP = 1;

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
	private static final String PREF_ZOOM_FONT_HEIGHTS = ConsolePlugin.getUniqueIdentifier() + ".zoomFontHeights"; //$NON-NLS-1$

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
						applyHeight(textConsole, state.height());
					}
				}
			}
		}

		@Override
		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (console instanceof TextConsole textConsole) {
					textConsole.removePropertyChangeListener(FONT_ENFORCER);
					disposeZoomFont(textConsole);
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
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		applyZoom(part, STEP);
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
		if (Display.getCurrent() == null) {
			Display.getDefault().asyncExec(() -> applyZoom(part, delta));
			return;
		}

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
		if (Display.getCurrent() == null) {
			Display.getDefault().asyncExec(() -> applyHeight(textConsole, height));
			return;
		}

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
		FontData[] newFontData = new FontData[fontData.length];
		for (int i = 0; i < fontData.length; i++) {
			FontData fd = fontData[i];
			newFontData[i] = new FontData(fd.getName(), height, fd.getStyle());
		}

		Object oldAttribute = textConsole.getAttribute(ZOOM_FONT_ATTRIBUTE);
		Font oldZoomFont = oldAttribute instanceof Font f ? f : null;

		Font newZoomFont = new Font(currentFont.getDevice(), newFontData);
		// remember/apply before disposing the old one, in case they are the same object
		textConsole.setAttribute(ZOOM_FONT_ATTRIBUTE, newZoomFont);
		textConsole.setFont(newZoomFont);

		if (oldZoomFont != null && !oldZoomFont.isDisposed()) {
			oldZoomFont.dispose();
		}
	}

	/**
	 * Disposes the custom zoom font remembered on the given console, if any.
	 * Dispatches to the UI thread if necessary.
	 *
	 * @param textConsole the console whose zoom font should be disposed
	 */
	private static void disposeZoomFont(TextConsole textConsole) {
		Object attribute = textConsole.getAttribute(ZOOM_FONT_ATTRIBUTE);
		if (!(attribute instanceof Font font) || font.isDisposed()) {
			return;
		}
		if (Display.getCurrent() == null) {
			// see applyHeight(...) above for why asyncExec (not syncExec) is used
			Display.getDefault().asyncExec(() -> disposeZoomFont(textConsole));
			return;
		}
		if (!font.isDisposed()) {
			font.dispose();
		}
	}
}


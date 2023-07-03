/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.ui.internal;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

public class HyperlinkHandler implements MouseListener, MouseTrackListener, PaintListener, Listener {
	public static final int UNDERLINE_NEVER = 1;
	public static final int UNDERLINE_ROLLOVER = 2;
	public static final int UNDERLINE_ALWAYS = 3;
	private Cursor hyperlinkCursor;
	private Cursor busyCursor;
	private boolean hyperlinkCursorUsed = true;
	private int hyperlinkUnderlineMode = UNDERLINE_ALWAYS;
	private Color background;
	private Color foreground;
	private Color activeBackground;
	private Color activeForeground;
	private Hashtable<Control, IHyperlinkListener> hyperlinkListeners;
	private Control lastLink;
	/**
	 * HyperlinkHandler constructor comment.
	 */
	public HyperlinkHandler() {
		hyperlinkListeners = new Hashtable<>();
		hyperlinkCursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND);
		busyCursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT);
	}
	/**
	 * @return org.eclipse.swt.graphics.Color
	 */
	public Color getActiveBackground() {
		return activeBackground;
	}
	/**
	 * @return org.eclipse.swt.graphics.Color
	 */
	public Color getActiveForeground() {
		return activeForeground;
	}
	/**
	 * @return org.eclipse.swt.graphics.Color
	 */
	public Color getBackground() {
		return background;
	}
	/**
	 * @return org.eclipse.swt.graphics.Color
	 */
	public Color getForeground() {
		return foreground;
	}
	/**
	 * @return int
	 */
	public int getHyperlinkUnderlineMode() {
		return hyperlinkUnderlineMode;
	}
	/**
	 * @return org.eclipse.swt.widgets.Control
	 */
	public Control getLastLink() {
		return lastLink;
	}
	/**
	 * @return boolean
	 */
	public boolean isHyperlinkCursorUsed() {
		return hyperlinkCursorUsed;
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (e.button == 1)
			return;
		lastLink = (Control) e.widget;
	}

	@Override
	public void mouseEnter(MouseEvent e) {
		Control control = (Control) e.widget;

		if (isHyperlinkCursorUsed())
			control.setCursor(hyperlinkCursor);
		if (activeBackground != null)
			control.setBackground(activeBackground);
		if (activeForeground != null)
			control.setForeground(activeForeground);
		if (hyperlinkUnderlineMode == UNDERLINE_ROLLOVER)
			underline(control, true);
		IHyperlinkListener action = getLinkListener(control);
		if (action != null)
			action.linkEntered(control);
	}

	@Override
	public void mouseExit(MouseEvent e) {
		Control control = (Control) e.widget;

		if (isHyperlinkCursorUsed())
			control.setCursor(null);
		if (hyperlinkUnderlineMode == UNDERLINE_ROLLOVER)
			underline(control, false);
		if (background != null)
			control.setBackground(background);
		if (foreground != null)
			control.setForeground(foreground);
		IHyperlinkListener action = getLinkListener(control);
		if (action != null)
			action.linkExited(control);
	}

	@Override
	public void mouseHover(MouseEvent e) {
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (e.button != 1)
			return;
		IHyperlinkListener action = getLinkListener((Control) e.widget);

		if (action != null) {
			Control c = (Control) e.widget;
			c.setCursor(busyCursor);
			action.linkActivated(c);
			if (!c.isDisposed())
				c.setCursor(isHyperlinkCursorUsed() ? hyperlinkCursor : null);
		}
	}

	@Override
	public void paintControl(PaintEvent e) {
		Control control = (Control) e.widget;
		if (hyperlinkUnderlineMode == UNDERLINE_ALWAYS)
			HyperlinkHandler.underline(control, true);
	}
	/**
	 * @param control
	 *            org.eclipse.swt.widgets.Control
	 * @param listener
	 *            org.eclipse.help.ui.internal.IHyperlinkListener
	 */
	public void registerHyperlink(Control control, IHyperlinkListener listener) {
		if (background != null)
			control.setBackground(background);
		if (foreground != null)
			control.setForeground(foreground);
		control.addMouseListener(this);
		control.addMouseTrackListener(this);
		control.addListener(SWT.DefaultSelection, this);

		if (hyperlinkUnderlineMode == UNDERLINE_ALWAYS)
			control.addPaintListener(this);
		hyperlinkListeners.put(control, listener);
		removeDisposedLinks();
	}
	public IHyperlinkListener getLinkListener(Control c) {
		if (c instanceof Label)
			c = c.getParent();
		return hyperlinkListeners.get(c);
	}

	private void removeDisposedLinks() {
		for (Enumeration<Control> keys = hyperlinkListeners.keys(); keys
				.hasMoreElements();) {
			Control control = keys.nextElement();
			if (control.isDisposed()) {
				hyperlinkListeners.remove(control);
			}
		}
	}
	/**
	 */
	public void reset() {
		hyperlinkListeners.clear();
	}
	/**
	 * @param newActiveBackground
	 *            org.eclipse.swt.graphics.Color
	 */
	public void setActiveBackground(Color newActiveBackground) {
		activeBackground = newActiveBackground;
	}
	/**
	 * @param newActiveForeground
	 *            org.eclipse.swt.graphics.Color
	 */
	public void setActiveForeground(Color newActiveForeground) {
		activeForeground = newActiveForeground;
	}
	/**
	 * @param newBackground
	 *            org.eclipse.swt.graphics.Color
	 */
	public void setBackground(Color newBackground) {
		background = newBackground;
	}
	/**
	 * @param newForeground
	 *            org.eclipse.swt.graphics.Color
	 */
	public void setForeground(Color newForeground) {
		foreground = newForeground;
	}

	/**
	 * @param newHyperlinkCursorUsed
	 *                                   boolean
	 */
	public void setHyperlinkCursorUsed(boolean newHyperlinkCursorUsed) {
		hyperlinkCursorUsed = newHyperlinkCursorUsed;
	}

	/**
	 * @param newHyperlinkUnderlineMode
	 *                                      int
	 */
	public void setHyperlinkUnderlineMode(int newHyperlinkUnderlineMode) {
		hyperlinkUnderlineMode = newHyperlinkUnderlineMode;
	}
	/**
	 * @param control
	 *            org.eclipse.swt.widgets.Control
	 * @param inside
	 *            boolean
	 */
	public static void underline(Control control, boolean inside) {

		if (control instanceof HyperlinkLabel)
			control = ((HyperlinkLabel) control).getLabel();

		Composite parent = control.getParent();
		Rectangle bounds = control.getBounds();
		GC gc = new GC(parent);
		Color color = inside ? control.getForeground() : control
				.getBackground();
		gc.setForeground(color);
		int y = bounds.y + bounds.height;
		gc.drawLine(bounds.x, y, bounds.x + bounds.width, y);
		gc.dispose();
	}

	/**
	 * Sent when an event that the receiver has registered for occurs.
	 *
	 * @param event
	 *            the event which occurred
	 */
	@Override
	public void handleEvent(Event event) {
		IHyperlinkListener listener = getLinkListener((Control) event.widget);
		listener.linkActivated((Control) event.widget);
	}
}

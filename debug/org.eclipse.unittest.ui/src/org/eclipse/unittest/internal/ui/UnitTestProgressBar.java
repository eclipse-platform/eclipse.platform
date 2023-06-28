/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A progress bar with a red/green indication for success or failure.
 */
public class UnitTestProgressBar extends Canvas {
	private static final int DEFAULT_WIDTH = 160;
	private static final int DEFAULT_HEIGHT = 18;

	private int fCurrentTickCount = 0;
	private int fMaxTickCount = 0;
	private int fColorBarWidth = 0;
	private Color fOKColor;
	private Color fFailureColor;
	private Color fStoppedColor;
	private boolean fError;
	private boolean fStopped = false;

	/**
	 * Constructs a Unit Test Progress Bar object
	 *
	 * @param parent a parent composite
	 */
	public UnitTestProgressBar(Composite parent) {
		super(parent, SWT.NONE);

		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				fColorBarWidth = scale(fCurrentTickCount);
				redraw();
			}
		});
		addPaintListener(this::paint);
		addDisposeListener(event -> {
			fFailureColor.dispose();
			fOKColor.dispose();
			fStoppedColor.dispose();
		});
		Display display = parent.getDisplay();
		fFailureColor = new Color(display, 159, 63, 63);
		fOKColor = new Color(display, 95, 191, 95);
		fStoppedColor = new Color(display, 120, 120, 120);
	}

	/**
	 * Sets a maximum ticks count
	 *
	 * @param max a value of maximum ticks count
	 */
	public void setMaximum(int max) {
		fMaxTickCount = max;
	}

	/**
	 * Resets the progress bar
	 */
	public void reset() {
		fError = false;
		fStopped = false;
		fCurrentTickCount = 0;
		fMaxTickCount = 0;
		fColorBarWidth = 0;
		redraw();
	}

	/**
	 * Resets the progress bar using new initial values
	 *
	 * @param hasErrors <code>true</code> if a test has errors, otherwise
	 *                  <code>false</code>
	 * @param stopped   <code>true</code> if a test is stopped, otherwise
	 *                  <code>false</code>
	 * @param ticksDone a number of ticks done
	 * @param maximum   a maximum ticks count
	 */
	public void reset(boolean hasErrors, boolean stopped, int ticksDone, int maximum) {
		boolean noChange = fError == hasErrors && fStopped == stopped && fCurrentTickCount == ticksDone
				&& fMaxTickCount == maximum;
		fError = hasErrors;
		fStopped = stopped;
		fCurrentTickCount = ticksDone;
		fMaxTickCount = maximum;
		fColorBarWidth = scale(ticksDone);
		if (!noChange)
			redraw();
	}

	private void paintStep(int startX, int endX) {
		GC gc = new GC(this);
		setStatusColor(gc);
		Rectangle rect = getClientArea();
		startX = Math.max(1, startX);
		gc.fillRectangle(startX, 1, endX - startX, rect.height - 2);
		gc.dispose();
	}

	private void setStatusColor(GC gc) {
		if (fStopped)
			gc.setBackground(fStoppedColor);
		else if (fError)
			gc.setBackground(fFailureColor);
		else
			gc.setBackground(fOKColor);
	}

	/**
	 * Sets a stopped flag on the progress bar
	 */
	public void stopped() {
		fStopped = true;
		redraw();
	}

	private int scale(int value) {
		if (fMaxTickCount > 0) {
			Rectangle r = getClientArea();
			if (r.width != 0)
				return Math.max(0, value * (r.width - 2) / fMaxTickCount);
		}
		return value;
	}

	private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topleft, Color bottomright) {
		gc.setForeground(topleft);
		gc.drawLine(x, y, x + w - 1, y);
		gc.drawLine(x, y, x, y + h - 1);

		gc.setForeground(bottomright);
		gc.drawLine(x + w, y, x + w, y + h);
		gc.drawLine(x, y + h, x + w, y + h);
	}

	private void paint(PaintEvent event) {
		GC gc = event.gc;
		Display disp = getDisplay();

		Rectangle rect = getClientArea();
		gc.fillRectangle(rect);
		drawBevelRect(gc, rect.x, rect.y, rect.width - 1, rect.height - 1,
				disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
				disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

		setStatusColor(gc);
		fColorBarWidth = Math.min(rect.width - 2, fColorBarWidth);
		gc.fillRectangle(1, 1, fColorBarWidth, rect.height - 2);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		Point size = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		if (wHint != SWT.DEFAULT)
			size.x = wHint;
		if (hHint != SWT.DEFAULT)
			size.y = hHint;
		return size;
	}

	/**
	 * Steps the progress according to failures count
	 *
	 * @param failures a failures count
	 */
	public void step(int failures) {
		fCurrentTickCount++;
		int x = fColorBarWidth;

		fColorBarWidth = scale(fCurrentTickCount);

		if (!fError && failures > 0) {
			fError = true;
			x = 1;
		}
		if (fCurrentTickCount == fMaxTickCount)
			fColorBarWidth = getClientArea().width - 1;
		paintStep(x, fColorBarWidth);
	}

	/**
	 * Refreshes the progress bar
	 *
	 * @param hasErrors <code>true</code> if a test has errors, otherwise
	 *                  <code>false</code>
	 */
	public void refresh(boolean hasErrors) {
		fError = hasErrors;
		redraw();
	}

}

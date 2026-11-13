/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Anton Leherbauer (Wind River) - [420928] Terminal widget leaks memory
 *******************************************************************************/
package org.eclipse.terminal.internal.textcanvas;

import org.eclipse.swt.widgets.Display;
import org.eclipse.terminal.model.ITerminalTextDataSnapshot;

/**
 * @author Michael.Scharf@scharf-software.com
 *
 */
public class PollingTextCanvasModel extends AbstractTextCanvasModel {
	private static final int DEFAULT_POLL_INTERVAL = 50;
	/**
	 * How long a program is given to finish a screen it said it was drawing. A
	 * program that says so and then stops must not leave the view frozen.
	 */
	private static final int REDRAW_GRACE = 200;
	int fPollInterval = -1;
	private boolean fRedrawing;
	// whether the view held back since it last looked, and since when (System.nanoTime)
	private boolean fHeldBack;
	private long fHeldBackSince;
	// where a scroll or resize moved the view, or -1: applied with the next copy, as moving
	// the window over a snapshot held back would show its lines in the wrong places
	private int fWindowStart = -1, fWindowSize;

	/**
	 * A program that draws a screen in pieces can say where a screen begins and
	 * ends, and while it is between the two the view leaves it alone rather than
	 * catching it half drawn.
	 *
	 * @param redrawing whether a screen is being drawn
	 */
	public synchronized void setSynchronizedOutput(boolean redrawing) {
		fRedrawing = redrawing;
	}

	/**
	 * Copies the text unless a screen is being drawn. Every look at the text
	 * comes here, the polls and a scroll or resize alike, and the check and the
	 * copy are one step, so a screen cannot begin between them. The view is held
	 * back for at most {@link #REDRAW_GRACE} ms since it last looked, however many
	 * screens begin meanwhile, on a clock that does not move with the time of day.
	 */
	@Override
	protected synchronized boolean takeSnapshot() {
		long now = System.nanoTime();
		if (fRedrawing && !fHeldBack) {
			fHeldBack = true;
			fHeldBackSince = now;
		}
		if (fRedrawing && now - fHeldBackSince < REDRAW_GRACE * 1_000_000L) {
			return false;
		}
		fHeldBack = false;
		if (fWindowStart >= 0) {
			getSnapshot().setInterestWindow(fWindowStart, fWindowSize);
			fWindowStart = -1;
		}
		return super.takeSnapshot();
	}

	@Override
	protected synchronized boolean isOutOfDate() {
		return fWindowStart >= 0 || super.isOutOfDate();
	}

	/** A scroll or resize moves the window with the next copy of the text, not before. */
	@Override
	public void setVisibleRectangle(int startLine, int startCol, int height, int width) {
		synchronized (this) {
			fWindowStart = Math.max(0, startLine);
			fWindowSize = Math.max(1, height);
		}
		update();
	}

	/**
	 *
	 */
	public PollingTextCanvasModel(ITerminalTextDataSnapshot snapshot) {
		super(snapshot);
		startPolling();
	}

	public void setUpdateInterval(int t) {
		fPollInterval = t;
	}

	public void stopPolling() {
		// timerExec only dispatches if the delay is >=0
		fPollInterval = -1;
	}

	public void startPolling() {
		if (fPollInterval < 0) {
			fPollInterval = DEFAULT_POLL_INTERVAL;
			Display.getDefault().timerExec(fPollInterval, new Runnable() {
				@Override
				public void run() {
					update();
					Display.getDefault().timerExec(fPollInterval, this);
				}
			});
		}
	}
}

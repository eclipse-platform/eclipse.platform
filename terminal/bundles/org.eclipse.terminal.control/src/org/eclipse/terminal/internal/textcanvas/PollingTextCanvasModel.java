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
	private volatile long fRedrawingUntil;

	/**
	 * A program that draws a screen in pieces can say where a screen begins and
	 * ends, and while it is between the two the view leaves it alone rather than
	 * catching it half drawn.
	 */
	public void setSynchronizedOutput(boolean redrawing) {
		fRedrawingUntil = redrawing ? System.currentTimeMillis() + REDRAW_GRACE : 0;
	}

	private boolean mayLook() {
		return fRedrawingUntil == 0 || System.currentTimeMillis() > fRedrawingUntil;
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
					if (mayLook()) {
						update();
					}
					Display.getDefault().timerExec(fPollInterval, this);
				}
			});
		}
	}
}

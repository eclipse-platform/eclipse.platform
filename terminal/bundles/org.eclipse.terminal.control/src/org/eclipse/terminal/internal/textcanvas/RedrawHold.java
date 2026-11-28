/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.textcanvas;

/**
 * Synchronized output (DEC mode 2026): whether the view leaves the text alone
 * because a program is drawing a screen. One for a terminal, handed to every
 * model it makes, so a model made after a move holds as the one before did, and
 * decided here only.
 */
public final class RedrawHold {

	/**
	 * How long a program is given to finish a screen it said it was drawing. A
	 * program that says so and then stops must not leave the view frozen.
	 */
	private static final long GRACE_NANOS = 200 * 1_000_000L;

	private boolean fRedrawing;
	/** since when (System.nanoTime) the view has been held back, -1 while it is not */
	private long fSince = -1;

	/** @param redrawing whether a program says it is drawing a screen */
	public synchronized void setRedrawing(boolean redrawing) {
		fRedrawing = redrawing;
	}

	/**
	 * Asked each time the view would look at the text. The view is held back for at
	 * most {@link #GRACE_NANOS} since it last looked, however many screens begin
	 * meanwhile, on a clock that does not move with the time of day. Past that the
	 * mode ends, as in kitty: a program that said it was drawing and stopped does
	 * not hold back every later screen.
	 *
	 * @param now System.nanoTime()
	 * @return whether the view keeps what it shows
	 */
	synchronized boolean holds(long now) {
		if (fRedrawing && fSince < 0) {
			fSince = now;
		}
		if (fRedrawing && now - fSince < GRACE_NANOS) {
			return true;
		}
		fRedrawing = false;
		fSince = -1;
		return false;
	}
}

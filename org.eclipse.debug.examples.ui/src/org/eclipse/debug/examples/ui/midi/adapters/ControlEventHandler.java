/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.midi.adapters;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.examples.core.midi.launcher.ClockControl;
import org.eclipse.debug.examples.core.midi.launcher.MidiLaunch;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.update.DebugEventHandler;

/**
 * Listens to events from sequencer controls and fires corresponding
 * deltas to update the viewer.
 *
 * @since 1.0
 */
public class ControlEventHandler extends DebugEventHandler {

	/**
	 * Associated launch
	 */
	private MidiLaunch fLaunch;

	/**
	 * Timer used to update clock
	 */
	private Timer fTimer;

	/**
	 * @param proxy
	 */
	public ControlEventHandler(SequencerControlsModelProxy proxy) {
		super(proxy);
		fLaunch = proxy.getMidiLaunch();
	}

	protected void init() {
		if (!fLaunch.isSuspended() && !fLaunch.isTerminated() && !fLaunch.isDisconnected()) {
			startTimer();
		}
	}

	@Override
	protected boolean handlesEvent(DebugEvent event) {
		return true;
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		if (fTimer != null) {
			fTimer.cancel();
		}
	}

	@Override
	protected void refreshRoot(DebugEvent event) {
		ModelDelta delta = new ModelDelta(fLaunch, IModelDelta.CONTENT);
		fireDelta(delta);
	}

	@Override
	protected void handleResume(DebugEvent event) {
		super.handleResume(event);
		startTimer();
	}

	/**
	 * Starts a timer to update the clock
	 */
	private void startTimer() {
		fTimer = new Timer(true);
		fTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				ModelDelta delta = new ModelDelta(fLaunch, IModelDelta.NO_CHANGE);
				delta = delta.addNode(new ClockControl(fLaunch), IModelDelta.STATE | IModelDelta.CONTENT);
				fireDelta(delta);
			}
		}, 0, 100);
	}

	@Override
	protected void handleSuspend(DebugEvent event) {
		super.handleSuspend(event);
		if (fTimer != null) {
			fTimer.cancel();
			fTimer = null;
		}
	}




}

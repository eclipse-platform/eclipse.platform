/*****************************************************************
 * Copyright (c) 2009 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 286310)
 *     IBM Corporation - ongoing maintenance and enhancements
 *****************************************************************/
package org.eclipse.debug.examples.ui.midi.adapters;

import javax.sound.midi.Track;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.Viewer;

public class TrackModelProxy extends AbstractModelProxy {
	protected Track fTrack;

	public TrackModelProxy(Track track) {
		fTrack = track;
	}

	@Override
	public void installed(Viewer viewer) {
		super.installed(viewer);

		ModelDelta delta = new ModelDelta(fTrack, IModelDelta.NO_CHANGE);
		for (int i = 0; i < fTrack.size(); ++i) {
			delta.addNode(fTrack.get(i), IModelDelta.INSTALL);
		}

		fireModelChanged(delta);
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		MidiEventModelProxy.gChecked.clear();
	}
}

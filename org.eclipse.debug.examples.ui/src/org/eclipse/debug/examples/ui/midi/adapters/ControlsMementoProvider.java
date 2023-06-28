/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.examples.core.midi.launcher.MidiLaunch;
import org.eclipse.debug.examples.core.midi.launcher.TempoControl;
import org.eclipse.debug.internal.ui.model.elements.DebugElementMementoProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * Provides mementos for sequencer elements.
 *
 * @since 1.0
 */
public class ControlsMementoProvider extends DebugElementMementoProvider {

	@Override
	protected String getElementName(Object element, IPresentationContext context) throws CoreException {
		if (element instanceof MidiLaunch) {
			return "SEQUENCER"; //$NON-NLS-1$
		}
		if (element instanceof TempoControl) {
			return "TEMPO_CONTROL"; //$NON-NLS-1$
		}
		return null;
	}



}

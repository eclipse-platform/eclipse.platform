/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

package org.eclipse.debug.examples.internal.memory.core;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegister;
import org.eclipse.debug.core.model.IRegisterGroup;

public class SampleRegisterGroup extends DebugElement implements IRegisterGroup {

	SampleRegister fRegister1;
	SampleRegister fRegister2;
	SampleStackFrame fFrame;

	public SampleRegisterGroup(SampleStackFrame frame) {
		super(frame.getDebugTarget());
		fFrame = frame;
	}

	@Override
	public String getName() throws DebugException {
		return Messages.SampleRegisterGroup_0;
	}

	@Override
	public IRegister[] getRegisters() throws DebugException {
		if (fRegister1 == null) {
			fRegister1 = new SampleRegister(fFrame, this, "eax"); //$NON-NLS-1$
		}

		if (fRegister2 == null) {
			fRegister2 = new SampleRegister(fFrame, this, "ebx"); //$NON-NLS-1$
		}

		return new IRegister[] { fRegister1, fRegister2 };
	}

	@Override
	public boolean hasRegisters() throws DebugException {
		return true;
	}

	@Override
	public String getModelIdentifier() {
		return fFrame.getModelIdentifier();
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return fFrame.getDebugTarget();
	}

	@Override
	public ILaunch getLaunch() {
		return fFrame.getLaunch();
	}

}

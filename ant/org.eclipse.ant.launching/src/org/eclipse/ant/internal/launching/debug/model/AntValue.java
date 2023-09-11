/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.debug.model;

import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class AntValue extends AntDebugElement implements IValue {

	private final String fValueString;
	protected static final IVariable[] EMPTY = new IVariable[0];

	public AntValue(AntDebugTarget target, String value) {
		super(target);
		fValueString = value;
	}

	@Override
	public String getReferenceTypeName() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getValueString() {
		return fValueString;
	}

	@Override
	public boolean isAllocated() {
		return true;
	}

	@Override
	public IVariable[] getVariables() {
		return EMPTY;
	}

	@Override
	public boolean hasVariables() {
		return false;
	}

	@Override
	public String getModelIdentifier() {
		return IAntDebugConstants.ID_ANT_DEBUG_MODEL;
	}
}

/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au) - bug 154907
 *******************************************************************************/
package org.eclipse.ant.internal.launching.debug.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class AntPropertiesValue extends AntDebugElement implements IValue {

	private final List<AntProperty> fProperties = new ArrayList<>();

	public AntPropertiesValue(AntDebugTarget target) {
		super(target);
	}

	@Override
	public String getReferenceTypeName() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getValueString() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean isAllocated() {
		return true;
	}

	@Override
	public IVariable[] getVariables() {
		Collections.sort(fProperties);
		return fProperties.toArray(new IVariable[fProperties.size()]);
	}

	@Override
	public boolean hasVariables() {
		return true;
	}

	protected void addProperties(List<AntProperty> properties) {
		fProperties.addAll(properties);
	}

	public List<AntProperty> getProperties() {
		return fProperties;
	}
}

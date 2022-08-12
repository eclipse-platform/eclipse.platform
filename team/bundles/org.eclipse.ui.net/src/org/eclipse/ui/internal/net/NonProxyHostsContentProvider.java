/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd   bug    Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20070201   154100 pmoogk@ca.ibm.com - Peter Moogk, Port internet code from WTP to Eclipse base.
 *******************************************************************************/
package org.eclipse.ui.internal.net;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class NonProxyHostsContentProvider implements IStructuredContentProvider {

	public NonProxyHostsContentProvider() {
		super();
	}

	@Override
	public void dispose() {
		// Do nothing
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object[] getElements(Object inputElement) {
		Collection coll = (Collection) inputElement;
		return coll.toArray(new ProxyBypassData[0]);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Do nothing
	}

}

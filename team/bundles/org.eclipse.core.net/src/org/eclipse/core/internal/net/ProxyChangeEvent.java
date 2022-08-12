/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.core.internal.net;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyData;

public class ProxyChangeEvent implements IProxyChangeEvent {

	private final int type;
	private final String[] oldHosts;
	private final String[] nonProxiedHosts;
	private final IProxyData[] oldData;
	private final IProxyData[] changeData;

	public ProxyChangeEvent(int type, String[] oldHosts,
			String[] nonProxiedHosts, IProxyData[] oldData, IProxyData[] changedData) {
				this.type = type;
				this.oldHosts = oldHosts;
				this.nonProxiedHosts = nonProxiedHosts;
				this.oldData = oldData;
				this.changeData = changedData;
	}

	@Override
	public int getChangeType() {
		return type;
	}

	@Override
	public IProxyData[] getChangedProxyData() {
		return changeData;
	}

	@Override
	public String[] getNonProxiedHosts() {
		return nonProxiedHosts;
	}

	@Override
	public String[] getOldNonProxiedHosts() {
		return oldHosts;
	}

	@Override
	public IProxyData[] getOldProxyData() {
		return oldData;
	}

}

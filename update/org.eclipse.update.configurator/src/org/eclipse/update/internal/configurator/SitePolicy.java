/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.update.internal.configurator;


@SuppressWarnings("removal")
public class SitePolicy implements org.eclipse.update.configurator.IPlatformConfiguration.ISitePolicy {

	private int type;
	private String[] list;

	public SitePolicy() {
	}

	public SitePolicy(int type, String[] list) {
		if (type != org.eclipse.update.configurator.IPlatformConfiguration.ISitePolicy.USER_INCLUDE
				&& type != org.eclipse.update.configurator.IPlatformConfiguration.ISitePolicy.USER_EXCLUDE
				&& type != org.eclipse.update.configurator.IPlatformConfiguration.ISitePolicy.MANAGED_ONLY) {
			throw new IllegalArgumentException();
		}
		this.type = type;

		if (list == null) {
			this.list = new String[0];
		} else {
			this.list = list;
		}
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public String[] getList() {
		return list;
	}

	@Override
	public synchronized void setList(String[] list) {
		if (list == null) {
			this.list = new String[0];
		} else {
			this.list = list;
		}
	}

}

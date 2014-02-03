/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import org.eclipse.update.configurator.*;
import org.eclipse.update.configurator.IPlatformConfiguration.*;


public class SitePolicy implements IPlatformConfiguration.ISitePolicy {

	private int type;
	private String[] list;

	public SitePolicy() {
	}
	public SitePolicy(int type, String[] list) {
		if (type != ISitePolicy.USER_INCLUDE && type != ISitePolicy.USER_EXCLUDE && type != ISitePolicy.MANAGED_ONLY)
			throw new IllegalArgumentException();
		this.type = type;

		if (list == null)
			this.list = new String[0];
		else
			this.list = list;
	}

	/*
	 * @see ISitePolicy#getType()
	 */
	public int getType() {
		return type;
	}

	/*
	* @see ISitePolicy#getList()
	*/
	public String[] getList() {
		return list;
	}

	/*
	 * @see ISitePolicy#setList(String[])
	 */
	public synchronized void setList(String[] list) {
		if (list == null)
			this.list = new String[0];
		else
			this.list = list;
	}

}

/*******************************************************************************
 * Copyright (c) 2008 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net.proxy.win32.winhttp;

/**
 * This holder class just helps passing parameters into a native function and retrieve information
 * from it.
 * <p>
 * The fields will be written/read by the jni glue code.
 * </p>
 */
public class AutoProxyHolder {

	public int autoDetectFlags;

	public String autoConfigUrl;

	/**
	 * Set the auto detect flags.
	 *
	 * @param autoDetectFlags
	 */
	public void setAutoDetectFlags(int autoDetectFlags) {
		this.autoDetectFlags= autoDetectFlags;
	}

	/**
	 * Get the auto config url.
	 *
	 * @return the auto config url (pac file)
	 */
	public String getAutoConfigUrl() {
		return autoConfigUrl;
	}

}

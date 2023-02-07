/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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

package org.eclipse.ui.intro.config;

import java.util.Properties;

import org.eclipse.ui.intro.IIntroSite;

/**
 * An Intro action. Classes that implement this interface can be used as valid
 * value for the "class" parameter for the following intro url:
 * <p>
 * http://org.eclipse.ui.intro/runAction?pluginId=x.y.z&amp;class=x.y.z.someClass
 * </p>
 *
 * @since 3.0
 */
public interface IIntroAction {

	/**
	 * Called to run this intro command. The properties represents the key=value
	 * pairs extracted from the intro URL query.
	 *
	 * @param site The part to execute the command on.
	 * @param params Parameters for the command as extracted from the URL.
	 */
	public void run(IIntroSite site, Properties params);

}

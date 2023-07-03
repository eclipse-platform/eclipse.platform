/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help;
/**
 * Live Help Extension. Classes that implement this interface can be used as
 * live help extensions. When user clicks a live help link in a help document,
 * the class will be loaded and run.
 *
 * @since 2.0
 */
public interface ILiveHelpAction extends Runnable {
	/**
	 * This method will be called upon instantiation of the live help extension.
	 * The data will be passed as specified in the help document live help link.
	 *
	 * @param data -
	 *            initialization data as a String
	 */
	public void setInitializationString(String data);
}

/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
package org.eclipse.help.webapp;

import org.eclipse.help.internal.webapp.servlet.TocServlet;

/**
 * Class for administering the Toc system.
 *
 * @since 3.6
 */
public class TocSystem {

	/**
	 * Use this method to invalidate the currently cached Toc.
	 * The next call for a Toc will result in a newly generated
	 * table of contents.
	 *
	 */
	public static void clearCache()
	{
		TocServlet.clearCache();
	}
}

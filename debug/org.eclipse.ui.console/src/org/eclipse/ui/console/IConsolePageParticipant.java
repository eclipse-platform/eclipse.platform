/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.ui.console;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A console page participant is notified of page lifecycle events such as
 * creation, activation, deactivation and disposal. A page participant can also
 * provide adapters for a page. Participants are contributed via the
 * <code>org.eclispe.ui.console.consolePageParticipants</code> extension point.
 * <p>
 * Participant behavior is implementation dependent. For example, a page
 * participant could add actions to a console's toolbar by accessing a its
 * page's action bars.
 * </p>
 * <p>
 * Following is an example extension definition.
 * </p>
 *
 * <pre>
 * &lt;extension point=&quot;org.eclipse.ui.console.consolePageParticipants&quot;&gt;
 *   &lt;consolePageParticipant
 *      id=&quot;com.example.ExamplePageParticipant&quot;
 *      class=&quot;com.example.ExamplePageParticipant&quot;&gt;
 *   &lt;/consolePageParticipant&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The example page participant is contributed to all console pages. An optional
 * <code>enablement</code> attribute may be specified to control which consoles
 * a page participant is applicable to.
 * <p>
 * Clients contributing console page participant extensions are intended to
 * implement this interface.
 * </p>
 *
 * @since 3.1
 */
public interface IConsolePageParticipant extends IAdaptable {
	/**
	 * Called during page initialization. Marks the start of this
	 * page participant's lifecycle.
	 *
	 * @param page the page corresponding to the given console
	 * @param console the console for which a page has been created
	 */
	void init(IPageBookViewPage page, IConsole console);

	/**
	 * Disposes this page participant. Marks the end of this
	 * page participant's lifecycle.
	 */
	void dispose();

	/**
	 * Notification this participant's page has been activated.
	 */
	void activated();

	/**
	 * Notification this participant's page has been deactivated.
	 */
	void deactivated();

}

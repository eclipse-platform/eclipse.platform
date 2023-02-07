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
 * A context registered for context-sensitive help.
 * <p>
 * This interface models the context-sensitive help that can be associated with
 * SWT menus, menu items, and controls, and with JFace actions. A help context
 * provides the text description of the object with which it is associated with,
 * as well as topic links that contain more related information. This
 * information would be displayed to the user when context sensitive help (F1)
 * is requested.
 * </p>
 * <p>
 * In the current implementation of the Help system, valid contexts can be
 * contributed through the <code>contexts</code> element of the
 * <code>"org.eclipse.help.contexts"</code> extension point. The
 * <code>IHelp.findContext(String)</code> method is used at runtime to create
 * or fetch IContext objects using there fully qualified contextIds. If there is
 * a need to override this behavior, then this IContext interface could be
 * implemented by a client and registered with the SWT control or JFace action.
 * </p>
 */
public interface IContext {
	/**
	 * Returns the related topics for this help context.
	 *
	 * @return an array of related help topics.
	 * If no related topics have been defined for this context a zero length array is returned.
	 * May not return <code>null</code>
	 * @since 2.0
	 */
	public IHelpResource[] getRelatedTopics();
	/**
	 * Returns the text description for this context.
	 *
	 * @return the text description
	 */
	public String getText();
}

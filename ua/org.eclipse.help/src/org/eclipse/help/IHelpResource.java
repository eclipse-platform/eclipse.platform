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
 * A help resource, usually a help topic.
 * <p>
 * This interface models a help resource. In general, help resources are either
 * html help files, or table of contents XML files.
 * </p>
 *
 * @since 2.0
 */
public interface IHelpResource {

	/**
	 * This is attribute name used for href in XML files.
	 */
	public final static String HREF = "href"; //$NON-NLS-1$
	/**
	 * This is attribute name used for label in XML files.
	 */
	public final static String LABEL = "label"; //$NON-NLS-1$

	/**
	 * Returns the URL (as a string) associated with this help resource.
	 *
	 * @return the URL (as a string) associated with the resource
	 *         <p>
	 *         Valid URL of a help resource is:
	 *         </p>
	 *         <ul>
	 *         <li>a <em>/pluginID/path/to/resource</em>, where
	 *         <ul>
	 *         <li><em>pluginID</em> is the unique identifier of the plugin
	 *         containing the help resource,
	 *         <li><em>path/to/document</em> is the help resource path, relative
	 *         to the plugin directory.
	 *         </ul>
	 *         For example. <em>/myplugin/mytoc.xml</em> or
	 *         <em>/myplugin/references/myclass.html</em> are vaild.
	 *         <li>string representation of URI to an external document. In this
	 *         case, all special characters have to be enoded such that the URI
	 *         is appropriate to be opened with a web browser.
	 *         <em>http://eclipse.org/documents/my%20file.html</em> and
	 *         <em>jar:file:/c:/my%20sources/src.zip!/mypackage/MyClass.html</em>
	 *         are examples of valid URIs.
	 *         </ul>
	 */
	public String getHref();
	/**
	 * Returns the label of this help resource.
	 *
	 * @return the label
	 */
	public String getLabel();
}

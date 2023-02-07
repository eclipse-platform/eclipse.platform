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
 * Former interface to the help system UI.
 *
 * @deprecated marked for deletion, see Bug 442959. This interface became
 *             obsolete in 3.0, along with the extension point that it was
 *             associated with. The functionality provided by this interface is
 *             available elsewhere. Use
 *             {@link org.eclipse.help.HelpSystem#getTocs HelpSystem.getTocs()}
 *             and {@link org.eclipse.help.HelpSystem#getContext
 *             HelpSystem.getContext(String)} to obtain help resources. Use
 *             various display methods of
 *             {@link org.eclipse.ui.help.IWorkbenchHelpSystem} to display help
 *             resources.
 */
@Deprecated
public interface IHelp {

	/**
	 * Displays the entire help bookshelf.
	 * <p>
	 * This method is called by the platform to launch the help system UI
	 * </p>
	 *
	 * @since 2.0
	 * @deprecated Use WorkbenchHelp.displayHelp() instead.
	 */
	@Deprecated
	public void displayHelp();

	/**
	 * Displays context-sensitive help for the given context.
	 * <p>
	 * (x,y) coordinates specify the location where the context sensitive help
	 * UI will be presented. These coordinates are screen-relative (ie: (0,0) is
	 * the top left-most screen corner). The platform is responsible for calling
	 * this method and supplying the appropriate location.
	 * </p>
	 *
	 *
	 * @param context
	 *            the context to display
	 * @param x
	 *            horizontal position
	 * @param y
	 *            verifical position
	 * @since 2.0
	 * @deprecated Use WorkbenchHelp.displayContext(context,x,y) instead.
	 */
	@Deprecated
	public void displayContext(IContext context, int x, int y);

	/**
	 * Displays context-sensitive help for context with the given context id.
	 * <p>
	 * (x,y) coordinates specify the location where the context sensitive help
	 * UI will be presented. These coordinates are screen-relative (ie: (0,0) is
	 * the top left-most screen corner). The platform is responsible for calling
	 * this method and supplying the appropriate location.
	 * </p>
	 *
	 * @param contextId
	 *            the help context identifier; the parameter needs to have a
	 *            form pluginID.pluginContextId, where pluginID is ID of plug-in
	 *            contributing a context, and pluginContextID is ID of context
	 *            contributed in a plug-in.
	 * @param x
	 *            horizontal position
	 * @param y
	 *            verifical position
	 * @see #getContext(String)
	 * @since 2.0
	 * @deprecated Use
	 *             WorkbenchHelp.displayContext(HelpSystem.getContext(contextId),x,y)
	 *             instead.
	 */
	@Deprecated
	public void displayContext(String contextId, int x, int y);

	/**
	 * Displays help content for the help resource with the given URL.
	 * <p>
	 * This method is called by the platform to launch the help system UI,
	 * displaying the documentation identified by the <code>href</code>
	 * parameter.
	 * </p>
	 * <p>
	 * The help system makes no guarantee that all the help resources can be
	 * displayed or how they are displayed.
	 * </p>
	 *
	 * @param href
	 *            the URL of the help resource.
	 *            <p>
	 *            Valid href are as described in
	 *            {@link  org.eclipse.help.IHelpResource#getHref() IHelpResource.getHref()}
	 *            </p>
	 * @since 2.0
	 * @deprecated Use WorkbenchHelp.displayHelpResource(href) instead.
	 */
	@Deprecated
	public void displayHelpResource(String href);

	/**
	 * Displays help content for the help resource.
	 * <p>
	 * This method is called by the platform to launch the help system UI,
	 * displaying the documentation identified by the <code>helpResource</code>
	 * parameter.
	 * <p>
	 * The help system makes no guarantee that all the help resources can be
	 * displayed or how they are displayed.
	 * </p>
	 *
	 * @see IHelp#displayHelpResource(String)
	 * @param helpResource
	 *            the help resource to display.
	 * @since 2.0
	 * @deprecated Use WorkbenchHelp.displayHelpResource(helpResource.getHref())
	 *             instead.
	 */
	@Deprecated
	public void displayHelpResource(IHelpResource helpResource);

	/**
	 * Displays help content for the toc with the given URL.
	 * <p>
	 * This method is called by the platform to launch the help system UI,
	 * displaying the documentation identified by the <code>toc</code>
	 * parameter.
	 * </p>
	 * <p>
	 * Valid toc are contributed through the <code>toc</code> element of the
	 * <code>"org.eclipse.help.toc"</code> extension point.
	 * </p>
	 *
	 * @param toc
	 *            the URL of the toc as specified in the
	 *            <code>"org.eclipse.help.toc"</code> extenstion point
	 * @deprecated Use WorkbenchHelp.displayHelpResource(toc) instead.
	 */
	@Deprecated
	public void displayHelp(String toc);

	/**
	 * This method is an extension to the
	 * <a href="#displayHelp(java.lang.String)">displayHelp(String toc) </a>
	 * method, providing the ability to open the specified help topic.
	 * <p>
	 * <code>selectedTopic</code> should be a valid help topic url contained in
	 * the specified <code>toc</code> and have the following format:
	 * <em>/pluginID/path_to_document</em><br>
	 * where
	 * </p>
	 * <dl>
	 * <dt><em>pluginID</em> is the unique identifier of the plugin containing
	 * the help topic,</dt>
	 * <dt><em>path_to_document</em> is the help topic path, relative to the
	 * plugin directory</dt>
	 * </dl>
	 *
	 * @param toc
	 *            the URL of the toc
	 * @param selectedTopic
	 *            the help topic url.
	 * @see #displayHelp(java.lang.String)
	 * @deprecated Use WorkbenchHelp.displayHelpResource(selectedTopic) instead.
	 */
	@Deprecated
	public void displayHelp(String toc, String selectedTopic);

	/**
	 * Displays context-sensitive help for context with the given context id.
	 * <p>
	 * (x,y) coordinates specify the location where the context sensitive help
	 * UI will be presented. These coordinates are screen-relative (ie: (0,0) is
	 * the top left-most screen corner). The platform is responsible for calling
	 * this method and supplying the appropriate location.
	 * </p>
	 *
	 * @param contextId
	 *            the help context identifier
	 * @param x
	 *            horizontal position
	 * @param y
	 *            verifical position
	 * @see #getContext(String)
	 * @deprecated Use
	 *             WorkbenchHelp.displayContext(HelpSystem.getContext(contextId),x,y)
	 *             instead.
	 */
	@Deprecated
	public void displayHelp(String contextId, int x, int y);

	/**
	 * Displays context-sensitive help for the given context.
	 * <p>
	 * (x,y) coordinates specify the location where the context sensitive help
	 * UI will be presented. These coordinates are screen-relative (ie: (0,0) is
	 * the top left-most screen corner). The platform is responsible for calling
	 * this method and supplying the appropriate location.
	 * </p>
	 *
	 *
	 * @param context
	 *            the context to display
	 * @param x
	 *            horizontal position
	 * @param y
	 *            verifical position
	 * @deprecated Use WorkbenchHelp.displayContext(context,x,y) instead.
	 */
	@Deprecated
	public void displayHelp(IContext context, int x, int y);

	/**
	 * Computes and returns context information for the given context id.
	 *
	 * @param contextId
	 *            the context id
	 * @return the context, or <code>null</code> if none
	 * @deprecated Use HelpSystem.getContext(contextId) instead.
	 */
	@Deprecated
	public IContext getContext(String contextId);

	/**
	 * Returns the list of all integrated tables of contents available.
	 *
	 * @return an array of TOC's
	 * @since 2.0
	 * @deprecated Use HelpSystem.getTocs() instead.
	 */
	@Deprecated
	public IToc[] getTocs();

	/**
	 * Returns <code>true</code> if the context-sensitive help window is
	 * currently being displayed, <code>false</code> if not.
	 *
	 * @deprecated Use WorkbenchHelp.isContextHelpDisplayed() instead.
	 */
	@Deprecated
	public boolean isContextHelpDisplayed();
}

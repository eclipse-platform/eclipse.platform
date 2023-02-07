/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

/**
 * An interface between a content provider and its parent container. A content
 * provider is responsible for creating dynamic intro content, while the content
 * provider site is responsible for reflowing the new content in the intro part.
 * An intro content provider site may have more than one content provider. The
 * id of the content provider can be used to distinguish the source of the
 * reflow.
 * <p>
 * This interface is not intended to be implemented or extended by clients.
 *
 * @since 3.0.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IIntroContentProviderSite {
	/**
	 * This method will be called when the IIntroContentProvider is notified
	 * that its content has become stale. For an HTML presentation, the whole
	 * page should be regenerated. An SWT presentation should cause the page's
	 * layout to be updated.
	 *
	 * @param provider
	 *            the content provider that requests a reflow
	 * @param incremental
	 *            if <code>true</code>, an attempt should be made to
	 *            incrementally reflow the page. Otherwise, the page should be
	 *            recreated from scratch. This is just a hint and the
	 *            implementation of the interface can ignore it.
	 */
	public void reflow(IIntroContentProvider provider, boolean incremental);
}

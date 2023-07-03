/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.modules;



/**
 * A help context id provider allows clients to customize F1 help content from a
 * debug view.  When the view input is changed, the view asks the input for
 * an adapter for IHelpContexIdProvider.  If a provider is returned, the provider
 * will be consulted to replace the default context help id for a given control.  If a provider
 * is not returned, then the view will use the default help context id.
 *
 * This is provided until Bug 216834 is fixed.
 *
 */
public interface IHelpContextIdProvider {

	/**
	 * Return the help context id that should be used in place of the given help context id.
	 * @param helpId the help context id to override
	 * @return the help context id that should be used in place of the given help context id
	 * or <code>null</code> if default is to be used
	 */
	String getHelpContextId(String helpId);
}

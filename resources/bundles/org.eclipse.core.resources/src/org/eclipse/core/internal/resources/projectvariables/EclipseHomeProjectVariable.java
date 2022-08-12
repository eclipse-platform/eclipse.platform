/*******************************************************************************
 * Copyright (c) 2008, 2014 Freescale Semiconductor and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Freescale Semiconductor - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources.projectvariables;

import java.net.URISyntaxException;
import java.net.URL;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.variableresolvers.PathVariableResolver;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;

/**
 * ECLIPSE_HOME project variable, pointing to the location of the eclipse install directory.
 *
 */
public class EclipseHomeProjectVariable extends PathVariableResolver {

	public static String NAME = "ECLIPSE_HOME"; //$NON-NLS-1$

	public EclipseHomeProjectVariable() {
		// nothing to do.
	}

	@Override
	public String[] getVariableNames(String variable, IResource resource) {
		return new String[] {NAME};
	}

	@Override
	public String getValue(String variable, IResource resource) {
		URL installURL = Platform.getInstallLocation().getURL();
		try {
			return URIUtil.toURI(installURL).toASCIIString();
		} catch (URISyntaxException e) {
			return null;
		}
	}
}

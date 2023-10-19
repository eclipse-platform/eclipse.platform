/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.util;

public interface PkcsConfigurationIfc {

	public String getImplementationName();

	public String getConfigurationFilename();

	public String getConfigurationLocationDir();

	public String getLibraryFilename();

	public String getLibraryLocationDir();

	public String getCertPassPhrase();

}

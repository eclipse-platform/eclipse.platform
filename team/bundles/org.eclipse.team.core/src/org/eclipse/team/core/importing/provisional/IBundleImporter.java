/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.team.core.importing.provisional;


/**
 * A bundle importer represents an instance of a bundle importer extension.
 * Clients contributing a bundle importer extension contribute an implementation
 * of {@link IBundleImporterDelegate} rather than this interface.
 * <p>
 * Clients contributing a bundle importer extension are intended to implement
 * {@link IBundleImporterDelegate}.
 * </p>
 * <p>
 * <strong>EXPERIMENTAL</strong>. This interface has been added as part of a
 * work in progress. There is no guarantee that this API will work or that it
 * will remain the same. Please do not use this API without consulting with the
 * Team team.
 * </p>
 *
 * @since 3.6
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBundleImporter extends IBundleImporterDelegate {

	/**
	 * Returns this impoter's unique identifier.
	 *
	 * @return identifier
	 */
	public String getId();

	/**
	 * Returns a short description of this importer, or <code>null</code> if unspecified.
	 *
	 * @return description or <code>null</code>
	 */
	public String getDescription();

	/**
	 * Returns a human readable name for this importer.
	 *
	 * @return name
	 */
	public String getName();
}

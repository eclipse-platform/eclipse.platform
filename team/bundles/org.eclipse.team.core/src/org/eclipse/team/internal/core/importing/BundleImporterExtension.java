/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.importing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.importing.provisional.BundleImporterDelegate;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.team.core.importing.provisional.IBundleImporterDelegate;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A bundle importer extension.
 *
 * @since 3.7
 */
public class BundleImporterExtension implements IBundleImporter {

	private IBundleImporterDelegate delegate;
	private IConfigurationElement element;

	/**
	 * Constructs a bundle importer extension on the given element.
	 *
	 * @param element contribution
	 */
	public BundleImporterExtension(IConfigurationElement element) {
		this.element = element;
	}

	@Override
	public ScmUrlImportDescription[] validateImport(Map[] manifests) {
		try {
			return getDelegate().validateImport(manifests);
		} catch (CoreException e) {
			TeamPlugin.log(e);
			return null;
		}
	}

	/**
	 * Returns underlying delegate.
	 *
	 * @return delegate
	 * @exception CoreException if unable to instantiate delegate
	 */
	private synchronized IBundleImporterDelegate getDelegate() throws CoreException {
		if (delegate == null) {
			delegate =  new BundleImporterDelegate() {
				private Set<String> supportedValues;
				private RepositoryProviderType providerType;
				@Override
				protected Set getSupportedValues() {
					if (supportedValues == null) {
						IConfigurationElement[] supported = element.getChildren("supports"); //$NON-NLS-1$
						supportedValues = new HashSet<>(supported.length);
						for (IConfigurationElement s : supported) {
							supportedValues.add(s.getAttribute("prefix")); //$NON-NLS-1$
						}
					}
					return supportedValues;
				}
				@Override
				protected RepositoryProviderType getProviderType() {
					if (providerType == null)
						providerType = RepositoryProviderType.getProviderType(element.getAttribute("repository")); //$NON-NLS-1$
					return providerType;
				}
			};
		}
		return delegate;
	}

	@Override
	public IProject[] performImport(ScmUrlImportDescription[] descriptions, IProgressMonitor monitor) throws CoreException {
		return getDelegate().performImport(descriptions, monitor);
	}

	@Override
	public String getId() {
		return element.getAttribute("id"); //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return element.getAttribute("description"); //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return element.getAttribute("name"); //$NON-NLS-1$
	}

}

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.importing;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.importing.provisional.*;
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporterDelegate#validateImport(java.util.Map[])
	 */
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
				private Set supportedValues;
				private RepositoryProviderType providerType;
				protected Set getSupportedValues() {
					if (supportedValues == null) {
						IConfigurationElement[] supported = element.getChildren("supports"); //$NON-NLS-1$
						supportedValues = new HashSet(supported.length);
						for (int i = 0; i < supported.length; i++) {
							supportedValues.add(supported[i].getAttribute("prefix")); //$NON-NLS-1$
						}
					}
					return supportedValues;
				}
				protected RepositoryProviderType getProviderType() {
					if (providerType == null)
						providerType = RepositoryProviderType.getProviderType(element.getAttribute("repository")); //$NON-NLS-1$
					return providerType;
				}
			};
		}
		return delegate;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.importing.IBundleImporterDelegate#performImport(org.eclipse.pde.core.importing.BundleImportDescription[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IProject[] performImport(ScmUrlImportDescription[] descriptions, IProgressMonitor monitor) throws CoreException {
		return getDelegate().performImport(descriptions, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporter#getId()
	 */
	public String getId() {
		return element.getAttribute("id"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporter#getDescription()
	 */
	public String getDescription() {
		return element.getAttribute("description"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporter#getName()
	 */
	public String getName() {
		return element.getAttribute("name"); //$NON-NLS-1$
	}

}

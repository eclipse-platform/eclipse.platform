/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.importing.provisional;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.team.core.*;
import org.eclipse.team.internal.core.TeamPlugin;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Abstract implementation of {@link IBundleImporterDelegate}. It is recommended
 * to subclass this class rather than implementing IBundleImporterDelegate
 * directly when providing importer delegates.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This interface has been added as part of a
 * work in progress. There is no guarantee that this API will work or that it
 * will remain the same. Please do not use this API without consulting with the
 * Team team.
 * </p>
 *
 * @since 3.6
 */
public abstract class BundleImporterDelegate implements IBundleImporterDelegate {

	private static final String ATTR_PROJECT = "project"; //$NON-NLS-1$

	public static final String ECLIPSE_SOURCE_REFERENCES = "Eclipse-SourceReferences"; //$NON-NLS-1$

	protected abstract Set getSupportedValues();

	protected abstract RepositoryProviderType getProviderType();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleImporterDelegate#validateImport(java.util.Map[])
	 */
	public ScmUrlImportDescription[] validateImport(Map[] manifests) {
		ScmUrlImportDescription[] results = new ScmUrlImportDescription[manifests.length];
		if (getProviderType() != null) {
			for (int i = 0; i < manifests.length; i++) {
				Map manifest = manifests[i];
				String value = (String) manifest.get(ECLIPSE_SOURCE_REFERENCES);
				if (value != null && value.length() > 8) {
					String prefix = value.substring(0, 8);
					if (getSupportedValues().contains(prefix)) {
						try {
							ManifestElement[] elements = ManifestElement.parseHeader(ECLIPSE_SOURCE_REFERENCES, value);
							for (int j = 0; j < elements.length; j++) {
								ManifestElement element = elements[j];
								String url = element.toString();
								String project = element.getAttribute(ATTR_PROJECT);
								if (project == null) {
									String bsn = (String) manifests[i].get(Constants.BUNDLE_SYMBOLICNAME);
									if (bsn != null) {
										ManifestElement[] bsnElement = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, bsn);
										project = bsnElement[0].getValue();
									}
								}
								results[i] = new ScmUrlImportDescription(url, project);
							}
						} catch (BundleException e) {
							TeamPlugin.log(IStatus.ERROR, "An exception occured while parsing a manifest header", e);//$NON-NLS-1$
						}
					}
				}
			}
		}
		return results;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.importing.IBundleImporterDelegate#performImport(org.eclipse.pde.core.importing.BundleImportDescription[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IProject[] performImport(ScmUrlImportDescription[] descriptions, IProgressMonitor monitor) throws CoreException {
		List references = new ArrayList();
		ProjectSetCapability psfCapability = getProviderType().getProjectSetCapability();
		IProject[] result = null;
		if (psfCapability != null) {
			// collect and validate all header values
			for (int i = 0; i < descriptions.length; i++) {
				ScmUrlImportDescription description = (ScmUrlImportDescription) descriptions[i];
				references.add(psfCapability.asReference(description.getUri(), description.getProject()));
			}
			// create projects
			if (!references.isEmpty()) {
				SubMonitor subMonitor = SubMonitor.convert(monitor, references.size());
				result = psfCapability.addToWorkspace((String[]) references.toArray(new String[references.size()]), new ProjectSetSerializationContext(), subMonitor);
				subMonitor.done();
			}
		}
		return result;
	}
}

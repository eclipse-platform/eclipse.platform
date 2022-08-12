/*******************************************************************************
 * Copyright (c) 2010, 2015 Broadcom Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Broadcom Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.Objects;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;

/**
 * Concrete implementation of a build configuration.
 *<p>
 * This class can both be used as a real build configuration in a project.
 * As well as the reference to a build configuration in another project.
 *</p>
 *<p>
 * When being used as a reference, core.resources <strong>must</strong> call
 * {@link #getBuildConfig()} to dereference the build configuration to the
 * the actual build configuration on the referenced project.
 *</p>
 */
public class BuildConfiguration extends PlatformObject implements IBuildConfiguration {

	/** Project on which this build configuration is set */
	private final IProject project;
	/** Unique human readable name of the configuration in the project */
	private final String name;

	public BuildConfiguration(IProject project) {
		this(project, IBuildConfiguration.DEFAULT_CONFIG_NAME);
	}

	public BuildConfiguration(IProject project, String configName) {
		this.project = project;
		this.name = configName;
	}

	/**
	 * @return the concrete build configuration referred to by this IBuildConfiguration
	 *         when it's being used as a reference
	 */
	public IBuildConfiguration getBuildConfig() throws CoreException {
		return project.getBuildConfig(name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BuildConfiguration other = (BuildConfiguration) obj;
		return Objects.equals(this.name, other.name) && Objects.equals(this.project, other.project);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(name);
		result = prime * result + Objects.hashCode(project);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (project != null)
			result.append(project.getName());
		else
			result.append("?"); //$NON-NLS-1$
		result.append(";"); //$NON-NLS-1$
		if (name != null)
			result.append(" [").append(name).append(']'); //$NON-NLS-1$
		else
			result.append(" [active]"); //$NON-NLS-1$
		return result.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(project))
			return (T) project;
		return super.getAdapter(adapter);
	}

}

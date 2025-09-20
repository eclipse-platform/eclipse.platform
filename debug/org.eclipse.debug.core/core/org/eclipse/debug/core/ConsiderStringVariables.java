/*******************************************************************************
 * Copyright (c) 2024, 2025 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * Default implementation for {@link ILaunchAttribute} that tries to
 * substitute variables for launch attributes with {@link String} type
 *
 * @see IStringVariableManager
 */
record ConsiderStringVariables<V>(String id, Class<V> type, Function<ILaunchConfiguration, V> defaults, String name, String description) implements ILaunchAttribute<V> {

	@Override
	public V read(ILaunchConfiguration configuration) throws CoreException {
		return readInternal(configuration);
	}

	@Override
	public Optional<V> probe(ILaunchConfiguration configuration) {
		try {
			return Optional.ofNullable(read(configuration));
		} catch (CoreException e) {
			Platform.getLog(getClass()).log(e.getStatus());
			return Optional.empty();
		}
	}

	private V readInternal(ILaunchConfiguration configuration) throws CoreException {
		if (String.class.equals(type)) {
			String raw = configuration.getAttribute(id, String.class.cast(defaults.apply(configuration)));
			if (raw == null) {
				return null;
			}
			return type.cast(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(raw));
		}
		if (Boolean.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Boolean.class.cast(defaults.apply(configuration))));
		}
		if (Integer.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Integer.class.cast(defaults.apply(configuration))));
		}
		if (List.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, List.class.cast(defaults.apply(configuration))));
		}
		if (Map.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Map.class.cast(defaults.apply(configuration))));
		}
		if (Set.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Set.class.cast(defaults.apply(configuration))));
		}
		throw new CoreException(Status.error(id, new ClassCastException()));
	}

	@Override
	public void write(ILaunchConfigurationWorkingCopy working, V value) {
		if (String.class.equals(type)) {
			working.setAttribute(id, String.class.cast(value));
		} else if (Integer.class.equals(type)) {
			working.setAttribute(id, Integer.class.cast(value).intValue());
		} else if (Boolean.class.equals(type)) {
			working.setAttribute(id, Boolean.class.cast(value).booleanValue());
		} else if (List.class.equals(type)) {
			working.setAttribute(id, List.class.cast(value));
		} else if (Map.class.equals(type)) {
			working.setAttribute(id, Map.class.cast(value));
		} else if (Set.class.equals(type)) {
			working.setAttribute(id, Set.class.cast(value));
		} else {
			working.setAttribute(id, value);
		}
	}
}
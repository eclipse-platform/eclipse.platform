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
 *
 * The definition of {@link ILaunchConfiguration} attribute convenience to:
 * <ul>
 * <li>{@link ILaunchConfiguration#getAttribute(String, String)} and similar
 * operations</li>
 * <li>{@link ILaunchConfigurationWorkingCopy#setAttribute(String, String)} and
 * similar operations</li>
 * <li>Connecting {@link ILaunchConfiguration} attributes with preferences</li>
 * <li>Representing {@link ILaunchConfiguration} attributes in UI</li>
 * </ul>
 *
 * @since 3.23
 */
public interface ILaunchAttribute<V> {

	/**
	 *
	 * @return identity for defined attribute
	 */
	ILaunchAttributeIdentity identity();

	/**
	 * Reads the value of launch configuration attribute and tries to substitute
	 * variables for String values. <br/>
	 * Returns the attribute value or fails with {@link CoreException}, may
	 * return <code>null</code> value
	 *
	 * @param configuration the launch configuration to read attribute from,
	 *            must not be <code>null</code>
	 * @return the attribute value, may return <code>null</code>
	 */
	V read(ILaunchConfiguration configuration) throws CoreException;

	/**
	 * "Probes" the value of launch configuration attribute. <br/>
	 * Returns {@link Optional} in case of read failure or <code>null</code>
	 * value to let caller decide how to proceed
	 *
	 * @param configuration the launch configuration to read attribute from,
	 *            must not be <code>null</code>
	 * @return the {@link Optional} with attribute value
	 */
	Optional<V> probe(ILaunchConfiguration configuration);

	/**
	 * Writes the supplied value to the launch configuration working copy
	 *
	 * @param working the launch configuration working copy to write value to,
	 *            must not be <code>null</code>
	 * @param value the value, may be <code>null</code>
	 */
	void write(ILaunchConfigurationWorkingCopy working, V value);

	/**
	 * creates an instance of {@link ILaunchAttribute} using primitives (useful
	 * for existing code)
	 *
	 * @param <V> the value type for the launch attribute
	 * @param identity the id of the attribute, must not be <code>null</code>
	 * @param type the value type of the attribute, must not be
	 *            <code>null</code>
	 * @param value the {@link Function} to calculate of default value of the
	 *            attribute from {@link ILaunchConfiguration}, must not be
	 *            <code>null</code>
	 * @param name the name of the attribute, must not be <code>null</code>
	 * @return created instance of {@link ILaunchAttribute}
	 */
	static <V> ILaunchAttribute<V> of(ILaunchAttributeIdentity identity, Class<V> type, Function<ILaunchConfiguration, V> value, String name) {
		return new ConsiderStringVariables<>(identity, type, value, name, name);
	}

	/**
	 * Default implementation for {@link ILaunchAttribute} that tries to
	 * substitute variables for launch attributes with {@link String} type
	 *
	 * @see IStringVariableManager
	 */
	record ConsiderStringVariables<V>(ILaunchAttributeIdentity identity, Class<V> type, Function<ILaunchConfiguration, V> defaults, String name, String description) implements ILaunchAttribute<V> {

		@Override
		public ILaunchAttributeIdentity identity() {
			return identity;
		}

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
				return type.cast(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(configuration.getAttribute(identity.id(), String.class.cast(defaults.apply(configuration)))));
			}
			if (Boolean.class.equals(type)) {
				return type.cast(configuration.getAttribute(identity.id(), Boolean.class.cast(defaults.apply(configuration))));
			}
			if (Integer.class.equals(type)) {
				return type.cast(configuration.getAttribute(identity.id(), Integer.class.cast(defaults.apply(configuration))));
			}
			if (List.class.equals(type)) {
				return type.cast(configuration.getAttribute(identity.id(), List.class.cast(defaults.apply(configuration))));
			}
			if (Map.class.equals(type)) {
				return type.cast(configuration.getAttribute(identity.id(), Map.class.cast(defaults.apply(configuration))));
			}
			if (Set.class.equals(type)) {
				return type.cast(configuration.getAttribute(identity.id(), Set.class.cast(defaults.apply(configuration))));
			}
			throw new CoreException(Status.error(identity.id(), new ClassCastException()));
		}

		@Override
		public void write(ILaunchConfigurationWorkingCopy working, V value) {
			if (String.class.equals(type)) {
				working.setAttribute(identity.id(), String.class.cast(value));
			} else if (Integer.class.equals(type)) {
				working.setAttribute(identity.id(), Integer.class.cast(value).intValue());
			} else if (Boolean.class.equals(type)) {
				working.setAttribute(identity.id(), Boolean.class.cast(value).booleanValue());
			} else if (List.class.equals(type)) {
				working.setAttribute(identity.id(), List.class.cast(value));
			} else if (Map.class.equals(type)) {
				working.setAttribute(identity.id(), Map.class.cast(value));
			} else if (Set.class.equals(type)) {
				working.setAttribute(identity.id(), Set.class.cast(value));
			} else {
				working.setAttribute(identity.id(), value);
			}
		}
	}

}

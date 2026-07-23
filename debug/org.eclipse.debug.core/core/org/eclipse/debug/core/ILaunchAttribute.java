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

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;

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
 * @since 3.24
 */
public interface ILaunchAttribute<V> {

	/**
	 *
	 * @return id for defined attribute
	 */
	String id();

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
	 * @param id the id of the attribute, must not be <code>null</code>
	 * @param type the value type of the attribute, must not be
	 *            <code>null</code>
	 * @param value the {@link Function} to calculate of default value of the
	 *            attribute from {@link ILaunchConfiguration}, must not be
	 *            <code>null</code>
	 * @param name the name of the attribute, must not be <code>null</code>
	 * @return created instance of {@link ILaunchAttribute}
	 */
	static <V> ILaunchAttribute<V> of(String id, Class<V> type, Function<ILaunchConfiguration, V> value, String name) {
		return new ConsiderStringVariables<>(id, type, value, name, name);
	}

}

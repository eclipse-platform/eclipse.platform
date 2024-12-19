/*******************************************************************************
 * Copyright (c) 2024 ArSysOp.
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

/**
 *
 * Reads the value of launch attribute
 *
 * @since 3.22
 */
public final class LaunchAttributeRead<V> {

	private final ILaunchConfiguration configuration;
	private final String id;
	private final Class<V> type;
	private final Supplier<V> value;

	public LaunchAttributeRead(ILaunchConfiguration configuration, LaunchAttributeDefined<V> defined) {
		this(configuration, defined.identity().id(), defined.metadata().valueClass(), defined.metadata()::defaultValue);
	}

	public LaunchAttributeRead(ILaunchConfiguration configuration, String id, Class<V> type, Supplier<V> value) {
		this.configuration = Objects.requireNonNull(configuration);
		this.id = Objects.requireNonNull(id);
		this.type = Objects.requireNonNull(type);
		this.value = Objects.requireNonNull(value);
	}

	public V get() throws CoreException {
		if (String.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, String.class.cast(value.get())));
		}
		if (Boolean.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Boolean.class.cast(value.get())));
		}
		if (Integer.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Integer.class.cast(value.get())));
		}
		if (List.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, List.class.cast(value.get())));
		}
		if (Map.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Map.class.cast(value.get())));
		}
		if (Set.class.equals(type)) {
			return type.cast(configuration.getAttribute(id, Set.class.cast(value.get())));
		}
		throw new CoreException(Status.error(id, new ClassCastException()));
	}

}

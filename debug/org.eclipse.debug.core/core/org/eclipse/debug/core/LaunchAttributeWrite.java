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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * Writes the value of launch attribute. Useful in conjunction with UI.
 *
 * @since 3.22
 */
public final class LaunchAttributeWrite<V> implements Consumer<ILaunchConfigurationWorkingCopy> {

	private final String id;
	private final Class<V> type;
	private final Supplier<V> value;

	public LaunchAttributeWrite(LaunchAttributeDefined<V> defined) {
		this(defined.identity().id(), defined.metadata().valueClass(), defined.metadata()::defaultValue);
	}

	public LaunchAttributeWrite(LaunchAttributeDefined<V> defined, Supplier<V> value) {
		this(defined.identity().id(), defined.metadata().valueClass(), value);
	}

	public LaunchAttributeWrite(String id, Class<V> type, Supplier<V> value) {
		this.id = Objects.requireNonNull(id);
		this.type = Objects.requireNonNull(type);
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public void accept(ILaunchConfigurationWorkingCopy working) {
		if (String.class.equals(type)) {
			working.setAttribute(id, String.class.cast(value.get()));
		} else if (Integer.class.equals(type)) {
			working.setAttribute(id, Integer.class.cast(value.get()).intValue());
		} else if (Boolean.class.equals(type)) {
			working.setAttribute(id, Boolean.class.cast(value.get()).booleanValue());
		} else if (List.class.equals(type)) {
			working.setAttribute(id, List.class.cast(value.get()));
		} else if (Map.class.equals(type)) {
			working.setAttribute(id, Map.class.cast(value.get()));
		} else if (Set.class.equals(type)) {
			working.setAttribute(id, Set.class.cast(value.get()));
		} else {
			working.setAttribute(id, value.get());
		}
	}

}

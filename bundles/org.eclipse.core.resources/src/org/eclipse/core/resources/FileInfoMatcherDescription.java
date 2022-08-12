/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
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
package org.eclipse.core.resources;

import java.util.Objects;

/**
 * A description of a file info matcher.
 * @since 3.6
 */
public final class FileInfoMatcherDescription {

	private String id;

	private Object arguments;

	public FileInfoMatcherDescription(String id, Object arguments) {
		super();
		this.id = id;
		this.arguments = arguments;
	}

	public Object getArguments() {
		return arguments;
	}

	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(arguments);
		result = prime * result + Objects.hashCode(id);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileInfoMatcherDescription other = (FileInfoMatcherDescription) obj;
		return Objects.equals(this.arguments, other.arguments) && Objects.equals(this.id, other.id);
	}
}
/*******************************************************************************
 * Copyright (c) 2023 Joerg Kubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.dtree;

import java.util.Arrays;
import java.util.Objects;
import org.eclipse.core.runtime.IPath;

/**
 * Simplified representation of an OS independent IPath used in the @{link
 * org.eclipse.core.internal.watson.ElementTree} persisted in
 * {@link org.eclipse.core.internal.resources.Workspace#getElementTree()}.
 * Should be optimized for memory consumption to keep a small memory footprint
 * of the workspace history.
 **/
public record JPath(String[] segments) {

	public static final JPath ROOT = JPath.of(); // unlike IPath do not have a leading "/"

	public static JPath of(IPath key) {
		return new JPath(key.segments());
	}

	public static JPath of(String... segments) {
		Objects.requireNonNull(segments);
		return new JPath(segments);
	}

	public static JPath[] of(IPath[] paths) {
		JPath[] result = new JPath[paths.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = JPath.of(paths[i]);
		}
		return result;
	}

	JPath append(String segment) {
		String[] newSegments = Arrays.copyOf(segments, segments.length + 1);
		newSegments[segments.length] = segment;
		return new JPath(newSegments);
	}

	public String lastSegment() {
		int len = segments.length;
		return len == 0 ? null : segments[len - 1];
	}

	public boolean isRoot() {
		return segments.length == 0;
	}

	public JPath removeLastSegment() {
		return JPath.of(Arrays.copyOf(segments, segments.length - 1));
	}

	public int segmentCount() {
		return segments.length;
	}

	public String segment(int i) {
		return segments[i];
	}

	public static IPath[] toIPath(JPath[] paths) {
		IPath[] result = new IPath[paths.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = paths[i].toIPath();
		}
		return result;
	}

	private static final String IPATH_SEPARATOR = Character.toString(IPath.SEPARATOR);

	public IPath toIPath() {
		return IPath.ROOT.append(String.join(IPATH_SEPARATOR, segments));
	}

	@Override
	public final String toString() {
		return String.join(IPATH_SEPARATOR, segments);
	}

	@Override
	public final int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean equals(Object arg0) {
		throw new UnsupportedOperationException();
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.Objects;

import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.eclipse.ui.IEditorInput;

/**
 * The result of a source lookup contains the source element, editor id, and
 * editor input resolved for a debug artifact.
 *
 * @since 3.1
 */
public class SourceLookupResult implements ISourceLookupResult {

	/**
	 * Element that source was resolved for.
	 */
	private final Object fArtifact;
	/**
	 * Corresponding source element, or <code>null</code>
	 * if unknown.
	 */
	private final Object fSourceElement;
	/**
	 * Associated editor id, used to display the source element,
	 * or <code>null</code> if unknown.
	 */
	private final String fEditorId;
	/**
	 * Associated editor input, used to display the source element,
	 * or <code>null</code> if unknown.
	 */
	private final IEditorInput fEditorInput;

	/**
	 * Creates a source lookup result on the given artifact, source element,
	 * editor id, and editor input.
	 */
	public SourceLookupResult(Object artifact, Object sourceElement, String editorId, IEditorInput editorInput) {
		fArtifact = artifact;
		fSourceElement = sourceElement;
		fEditorId = editorId;
		fEditorInput = editorInput;
	}

	@Override
	public Object getArtifact() {
		return fArtifact;
	}

	@Override
	public Object getSourceElement() {
		return fSourceElement;
	}

	@Override
	public String getEditorId() {
		return fEditorId;
	}

	@Override
	public IEditorInput getEditorInput() {
		return fEditorInput;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fArtifact, fEditorId, fEditorInput, fSourceElement);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof SourceLookupResult)) {
			return false;
		}

		SourceLookupResult other = (SourceLookupResult) obj;
		return Objects.equals(fEditorId, other.fEditorId)
				&& Objects.equals(fArtifact, other.fArtifact)
				&& Objects.equals(fEditorInput, other.fEditorInput)
				&& Objects.equals(fSourceElement, other.fSourceElement);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SourceLookupResult ["); //$NON-NLS-1$
		if (fEditorId != null) {
			builder.append("editorId="); //$NON-NLS-1$
			builder.append(fEditorId);
			builder.append(", "); //$NON-NLS-1$
		}
		if (fEditorInput != null) {
			builder.append("editorInput="); //$NON-NLS-1$
			builder.append(fEditorInput);
			builder.append(", "); //$NON-NLS-1$
		}
		if (fArtifact != null) {
			builder.append("artifact="); //$NON-NLS-1$
			builder.append(fArtifact);
			builder.append(", "); //$NON-NLS-1$
		}
		if (fSourceElement != null) {
			builder.append("sourceElement="); //$NON-NLS-1$
			builder.append(fSourceElement);
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}

}

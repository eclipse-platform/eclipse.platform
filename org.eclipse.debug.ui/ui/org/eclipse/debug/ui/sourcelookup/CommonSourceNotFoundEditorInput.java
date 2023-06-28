/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui.sourcelookup;

import java.text.MessageFormat;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * Editor input for the <code>CommonSourceNotFoundEditor</code>. The editor
 * input can be created on an artifact that has a source association.
 * <p>
 * This class may be instantiated and subclassed.
 * </p>
 * @see CommonSourceNotFoundEditor
 * @since 3.2
 */
public class CommonSourceNotFoundEditorInput extends PlatformObject implements IEditorInput {

	/**
	 * input element label (cached on creation)
	 */
	private String fLabel;
	/**
	 * the artifact that the editor is being opened for
	 */
	private Object fArtifact;

	/**
	 * Constructs an editor input for the given artifact associated with source.
	 *
	 * @param artifact artifact associated with source
	 */
	public CommonSourceNotFoundEditorInput(Object artifact) {
		fArtifact = artifact;
		if (artifact != null) {
			IDebugModelPresentation pres = DebugUITools.newDebugModelPresentation();
			fLabel = pres.getText(artifact);
			pres.dispose();
		}
		if (fLabel == null) {
			fLabel = IInternalDebugCoreConstants.EMPTY_STRING;
		}
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugUITools.getDefaultImageDescriptor(fArtifact);
	}

	@Override
	public String getName() {
		return fLabel;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return MessageFormat.format(SourceLookupUIMessages.addSourceLocation_editorMessage, new Object[] { fLabel });
	}

	/**
	 * Returns the artifact that source was not found for.
	 *
	 * @return artifact that source was not found for
	 */
	public Object getArtifact(){
		return fArtifact;
	}

}

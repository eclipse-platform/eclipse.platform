/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.model;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.xml.sax.Attributes;

public class AntImportNode extends AntTaskNode {

	private String fFile = null;

	public AntImportNode(Task task, Attributes attributes) {
		super(task);
		fFile = attributes.getValue(IAntCoreConstants.FILE);
	}

	public String getFile() {
		return fFile;
	}

	@Override
	protected ImageDescriptor getBaseImageDescriptor() {
		return AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_IMPORT);
	}

	@Override
	public String getLabel() {
		if (fLabel == null) {
			StringBuffer label = new StringBuffer(getTask().getTaskName());
			label.append(' ');
			label.append(fFile);

			if (isExternal()) {
				appendEntityName(label);
			}
			fLabel = label.toString();
		}
		return fLabel;
	}

	/**
	 * Execute the import. Returns <code>true</code> as the import adds to the Ant model
	 */
	@Override
	public boolean configure(boolean validateFully) {
		if (fConfigured) {
			return false;
		}
		try {
			getTask().maybeConfigure();
			getTask().execute();
			fConfigured = true;
			return true;
		}
		catch (BuildException be) {
			handleBuildException(be, AntEditorPreferenceConstants.PROBLEM_IMPORTS);
		}
		catch (AntSecurityException se) {
			// either a system exit or setting of system property was attempted
			handleBuildException(new BuildException(AntModelMessages.AntImportNode_0), AntEditorPreferenceConstants.PROBLEM_SECURITY);
		}
		return false;
	}

	@Override
	public IFile getIFile() {
		IFile file;
		if (isExternal()) {
			file = AntUtil.getFileForLocation(getFilePath(), null);
		} else {
			String path = getFile();
			file = AntUtil.getFileForLocation(path, getAntModel().getEditedFile().getParentFile());
		}
		return file;
	}

	@Override
	public String getReferencedElement(int offset) {
		if (fFile != null) {
			String textToSearch = getAntModel().getText(getOffset(), offset - getOffset());
			if (textToSearch != null && textToSearch.length() != 0) {
				String attributeString = AntEditorCompletionProcessor.getAttributeStringFromDocumentStringToPrefix(textToSearch);
				if (IAntCoreConstants.FILE.equals(attributeString)) {
					return fFile;
				}
			}
		}
		return null;
	}
}

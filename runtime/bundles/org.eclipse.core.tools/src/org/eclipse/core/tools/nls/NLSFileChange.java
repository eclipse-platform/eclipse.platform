/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.core.tools.nls;

import java.io.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

public class NLSFileChange extends TextFileChange {
	private IFile file;
	private String contents;

	public NLSFileChange(IFile file) {
		super("Message bundle properties file change", file); //$NON-NLS-1$
		this.file = file;
		setTextType("text"); //$NON-NLS-1$
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	void setContents(String text) {
		this.contents = text;
	}

	@Override
	protected void commit(IDocument document, IProgressMonitor monitor) throws CoreException {
		if (contents == null) {
			System.err.println("Cannot write null contents to file: " + file); //$NON-NLS-1$
			return;
		}
		byte[] bytes = null;
		try {
			bytes = contents.getBytes(file.getCharset(true));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (bytes == null)
			bytes = contents.getBytes();
		InputStream input = new BufferedInputStream(new ByteArrayInputStream(bytes));
		file.setContents(input, IResource.FORCE, null);
	}

	@Override
	public Object getModifiedElement() {
		return file;
	}

}

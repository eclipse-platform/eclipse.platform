/*******************************************************************************
 * Copyright (c) 2002, 2007 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH,
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.IProblemRequestor;
import org.eclipse.ant.internal.ui.model.LocationProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class AntEditorDocumentProvider extends TextFileDocumentProvider {

	private final AntDocumentSetupParticipant fAntDocumentSetupParticipant;

	/**
	 * Remembers a Ant document model for each element.
	 */
	protected static class AntFileInfo extends FileInfo {

		public AntModel fAntModel;

		public AntFileInfo() {
		}
	}

	public AntEditorDocumentProvider() {
		IDocumentProvider provider = new TextFileDocumentProvider(new AntStorageDocumentProvider());
		setParentDocumentProvider(provider);
		fAntDocumentSetupParticipant = new AntDocumentSetupParticipant();
	}

	public AntModel getAntModel(Object element) {
		FileInfo info = getFileInfo(element);
		if (info instanceof AntFileInfo xmlInfo) {
			return xmlInfo.fAntModel;
		}
		return null;
	}

	@Override
	protected IAnnotationModel createAnnotationModel(IFile file) {
		return new AntAnnotationModel(file);
	}

	protected AntModel createAntModel(Object element, IDocument document, IAnnotationModel annotationModel) {
		IProblemRequestor requestor = annotationModel instanceof IProblemRequestor i ? i : null;
		return new AntModel(document, requestor, new LocationProvider(element instanceof IEditorInput i ? i : null));
	}

	@Override
	protected FileInfo createFileInfo(Object element) throws CoreException {
		FileInfo info = super.createFileInfo(element);
		if (!(info instanceof AntFileInfo)) {
			return null;
		}

		// This is a required workaround for the disconnect between workbench file associations
		// and content types based document setup and creation
		// This ensures that a workbench file association for the AntEditor will have a document
		// that is setup with the correct document setup participant since it was "missed" by the
		// document setup extensions (bug 72598).
		IDocument document = info.fTextFileBuffer.getDocument();
		if (document instanceof IDocumentExtension3 extension) {
			if (extension.getDocumentPartitioner(AntDocumentSetupParticipant.ANT_PARTITIONING) == null) {
				fAntDocumentSetupParticipant.setup(document);
			}
		}

		// Check if the annotation model has been set by the annotation model factory extension for Ant UI
		// and is an annotation model that was specified by the extension (all are IProblemRequestors).
		// If we do not have an annotation model or not a correct annotation model, defer to the annotation model
		// created from this document provider. The document provider is only queried for an annotation model for workspace files.
		// Therefore if the annotation model is still null we are dealing with an external file that is associated with
		// the Ant editor from a user preference setting.
		// In all cases the determined annotation model is set for the file info to be used in the editor.
		AntFileInfo xmlInfo = (AntFileInfo) info;
		IAnnotationModel annotationModel = xmlInfo.fTextFileBuffer.getAnnotationModel();
		if (annotationModel instanceof IProblemRequestor) {
			xmlInfo.fModel = annotationModel;
		} else {
			annotationModel = xmlInfo.fModel;
		}

		if (annotationModel == null) {
			annotationModel = new AntExternalAnnotationModel();
			xmlInfo.fModel = annotationModel;
		}

		AntModel antModel = createAntModel(element, document, annotationModel);
		antModel.install();
		xmlInfo.fAntModel = antModel;

		return xmlInfo;
	}

	@Override
	protected void disposeFileInfo(Object element, FileInfo info) {
		if (info instanceof AntFileInfo xmlInfo) {
			if (xmlInfo.fAntModel != null) {
				IDocument doc = xmlInfo.fTextFileBuffer.getDocument();
				Object lock = null;
				if (doc instanceof ISynchronizable) {
					lock = ((ISynchronizable) doc).getLockObject();
				} else {
					lock = xmlInfo.fAntModel;
				}
				if (lock == null) {
					xmlInfo.fAntModel.dispose();
					xmlInfo.fAntModel = null;
				} else {
					synchronized (lock) {
						xmlInfo.fAntModel.dispose();
						xmlInfo.fAntModel = null;
					}
				}
			}
		}
		super.disposeFileInfo(element, info);
	}

	@Override
	protected FileInfo createEmptyFileInfo() {
		return new AntFileInfo();
	}
}
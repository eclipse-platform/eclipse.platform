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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.*;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.ui.contentassist.ContentAssistHandler;

/**
 * A content assist processor for tags for use with Text widgets.
 */
public class TagContentAssistProcessor implements ISubjectControlContentAssistProcessor {

	private FilteredTagList tags;
	private Map<ImageDescriptor, Image> images = new HashMap<>();

	public static void createContentAssistant(Text text, TagSource tagSource, int includeFlags) {
		final TagContentAssistProcessor tagContentAssistProcessor = new TagContentAssistProcessor(tagSource, includeFlags);
		text.addDisposeListener(e -> tagContentAssistProcessor.dispose());
		ContentAssistHandler.createHandlerForText(text, createSubjectContentAssistant(tagContentAssistProcessor));
	}

	private static SubjectControlContentAssistant createSubjectContentAssistant(IContentAssistProcessor processor) {
		final SubjectControlContentAssistant contentAssistant= new SubjectControlContentAssistant();
		
		contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
		//ContentAssistPreference.configure(contentAssistant, JavaPlugin.getDefault().getPreferenceStore());
		
		contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		contentAssistant.setInformationControlCreator(parent -> new DefaultInformationControl(parent));
		
		return contentAssistant;
	}
	
	public TagContentAssistProcessor(TagSource tagSource, int includeFlags) {
		tags = new FilteredTagList(tagSource, TagSource.convertIncludeFlaqsToTagTypes(includeFlags));
	}
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		Control c = contentAssistSubjectControl.getControl();
		int docLength = contentAssistSubjectControl.getDocument().getLength();
		if (c instanceof Text) {
			Text t = (Text)c;
			String filter = t.getText();
			tags.setPattern(filter);
			CVSTag[] matching = tags.getMatchingTags();
			if (matching.length > 0) {
				List<CompletionProposal> proposals = new ArrayList<>();
				for (CVSTag tag : matching) {
					String name = tag.getName();
					ImageDescriptor desc = TagElement.getImageDescriptor(tag);
					Image image = null;
					if (desc != null) {
						image = images.get(desc);
						if (image == null) {
							image = desc.createImage();
							images.put(desc, image);
						}
					}
					CompletionProposal proposal = new CompletionProposal(name, 0, docLength, name.length(), image, name, null, null);
					proposals.add(proposal);
				}
				return proposals.toArray(new ICompletionProposal[proposals.size()]);
			}
		}
		return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		return null;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}
	
	/**
	 * Dispose of any images created by the assistant
	 */
	public void dispose() {
		for (Iterator iter = images.values().iterator(); iter.hasNext();) {
			Image image = (Image) iter.next();
			image.dispose();
		}
	}
	
}

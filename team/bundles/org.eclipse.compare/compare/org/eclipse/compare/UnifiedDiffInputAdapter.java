/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.SharedDocumentAdapterWrapper;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IComparedWithProvider;

public class UnifiedDiffInputAdapter implements IAdapterFactory {

	private static final Class<?>[] ADAPTERS = { IUnifiedDiffInputAdapter.class, IComparedWithProvider.class };
	private static final Map<IEditorInput, CompareContext> COMPARE_INPUTS = Collections
			.synchronizedMap(new WeakHashMap<>());

	private static final class CompareContext {
		CompareEditorInput compareInput;
		WeakReference<Object> ownerEditor= new WeakReference<>(null);
	}

	public static final class UnifiedDiffInputDataAdapter implements IUnifiedDiffInputAdapter, IComparedWithProvider {
		@Override
		public void setCompareInput(IEditorInput inp, CompareEditorInput input) {
			if (inp == null) {
				return;
			}
			if (input == null) {
				COMPARE_INPUTS.remove(inp);
				return;
			}
			CompareContext context= COMPARE_INPUTS.computeIfAbsent(inp, __ -> new CompareContext());
			context.compareInput= input;
			// A new compare operation re-opens claiming for the next editor instance.
			context.ownerEditor= new WeakReference<>(null);
		}

		@Override
		@SuppressWarnings("nls")
		public String getComparedWithLabel(IEditorInput inp, Object editorKey) {
			CompareContext context= COMPARE_INPUTS.get(inp);
			if (context == null || !claimOrMatch(context, editorKey)) {
				return null;
			}
			CompareEditorInput compareInput = context.compareInput;
			if (compareInput == null || !(compareInput.getCompareResult() instanceof ICompareInput input)) {
				return null;
			}
			ITypedElement left = input.getLeft();
			ITypedElement right = input.getRight();
			IEditorInput leftInput = documentKeyOf(input.getLeft());
			IEditorInput rightInput = documentKeyOf(input.getRight());
			if (inp.equals(leftInput)) {
				return comparedWithLabel(left, right);
			}
			if (inp.equals(rightInput)) {
				return comparedWithLabel(right, left);
			}
			return compareInput.getTitle();
		}

		@Override
		public void clearComparedWithLabel(IEditorInput inp, Object editorKey) {
			if (inp == null || editorKey == null) {
				return;
			}
			CompareContext context= COMPARE_INPUTS.get(inp);
			if (context == null) {
				return;
			}
			synchronized (context) {
				Object owner= context.ownerEditor.get();
				if (owner == editorKey) {
					COMPARE_INPUTS.remove(inp);
				}
			}
		}

		private static boolean claimOrMatch(CompareContext context, Object editorKey) {
			if (editorKey == null) {
				return true;
			}
			synchronized (context) {
				Object owner = context.ownerEditor.get();
				if (owner == null || owner == editorKey) {
					context.ownerEditor= new WeakReference<>(editorKey);
					return true;
				}
				return false;
			}
		}

		private static IEditorInput documentKeyOf(ITypedElement element) {
			if (element == null) {
				return null;
			}
			ISharedDocumentAdapter adapter = SharedDocumentAdapterWrapper.getAdapter(element);
			return adapter == null ? null : adapter.getDocumentKey(element);
		}

		private static String labelOf(ITypedElement element) {
			if (element == null) {
				return null;
			}
			String name = element.getName();
			return name == null || name.isBlank() ? null : name;
		}

		private static String comparedWithLabel(ITypedElement current, ITypedElement opposite) {
			String oppositeName = labelOf(opposite);
			String oppositeDate = modificationDateOf(opposite);
			if (oppositeDate == null) {
				return oppositeName;
			}
			String currentName = labelOf(current);
			if (currentName != null && currentName.equals(oppositeName)) {
				// Same file name on both sides: the timestamp is the useful distinction.
				return oppositeDate;
			}
			return oppositeName == null ? oppositeDate : oppositeName + " (" + oppositeDate + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		private static String modificationDateOf(ITypedElement element) {

			if (!(element instanceof IModificationDate dateProvider)) {
				return null;
			}

			long timestamp = dateProvider.getModificationDate();
			if (timestamp <= 0) {
				return null;
			}
			return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp));
		}
	}

	private static final IUnifiedDiffInputAdapter ADAPTER = new UnifiedDiffInputDataAdapter();

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if ((adapterType == IUnifiedDiffInputAdapter.class || adapterType == IComparedWithProvider.class)
				&& adaptableObject instanceof IEditorInput) {
			return adapterType.cast(ADAPTER);
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTERS;
	}

}

/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.debug.internal.ui.codemining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

public class DebugValueCodeMiningProvider extends AbstractCodeMiningProvider {

	private volatile IDebugEventSetListener listener;

	@Override
	public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
			IProgressMonitor monitor) {
		if (!isEnabled()) {
			return CompletableFuture.completedFuture(List.of());
		}
		final IDocument document = viewer.getDocument();
		if (viewer instanceof ISourceViewerExtension5 && listener == null) {
			synchronized (this) {
				if (listener == null) {
					IDebugEventSetListener debugListener = e -> ((ISourceViewerExtension5) viewer).updateCodeMinings();
					DebugPlugin.getDefault().addDebugEventListener(debugListener);
					listener = debugListener;
				}
			}
		}
		if (!hasDebugRunning()) {
			return CompletableFuture.completedFuture(List.of());
		}
		return CompletableFuture.supplyAsync(() -> {
			List<DebugValueCodeMining> res = new ArrayList<>();
			for (int line = 0; line < document.getNumberOfLines(); line++) {
				LinkedHashMap<String, IVariable> variablesOnLine = new LinkedHashMap<>();
				try {
					IRegion lineInfo = document.getLineInformation(line);
					for (int offsetInLine = 0; offsetInLine < lineInfo.getLength(); offsetInLine++) {
						IVariable variableAtOffset = Adapters.adapt(
								new TextSelection(document, lineInfo.getOffset() + offsetInLine, 0), IVariable.class);
						if (variableAtOffset != null) {
							try {
								variablesOnLine.putIfAbsent(variableAtOffset.getName(), variableAtOffset);
								offsetInLine += variableAtOffset.getName().length();
							} catch (DebugException ex) {
								DebugUIPlugin.log(ex);
							}
						}
					}
					for (IVariable variableOnLine : variablesOnLine.values()) {
						res.add(new DebugValueCodeMining(document, line, variableOnLine, this));
					}
				} catch (BadLocationException e) {
					DebugUIPlugin.log(e);
				}
			}
			return res;
		});
	}

	private boolean hasDebugRunning() {
		return Stream.of(DebugPlugin.getDefault().getLaunchManager().getLaunches()) //
				.filter(Predicate.not(ILaunch::isTerminated)) //
				.anyMatch(launch -> ILaunchManager.DEBUG_MODE.equals(launch.getLaunchMode()));
	}

	private boolean isEnabled() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_SHOW_VARIABLES_INLINE);
	}

	@Override
	public void dispose() {
		IDebugEventSetListener debugListener = listener;
		if (debugListener != null) {
			DebugPlugin.getDefault().removeDebugEventListener(debugListener);
			listener = null;
		}
	}
}

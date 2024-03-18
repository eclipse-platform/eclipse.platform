/*******************************************************************************
 * Copyright (c) 2012, 2024 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi.participants;

import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

public class AnsiConsolePageParticipant implements IConsolePageParticipant {
	// Remember all the viewers we added, so that we can remove them.
	private static final Map<StyledText, IConsolePageParticipant> viewers = new HashMap<>();

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public void activated() {
		// Nothing to do, but we are forced to implement it for IConsolePageParticipant
	}

	@Override
	public void deactivated() {
		// Nothing to do, but we are forced to implement it for IConsolePageParticipant
	}

	@Override
	public void dispose() {
		removeViewerWithPageParticipant(this);
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		if (page.getControl() instanceof StyledText) {
			StyledText viewer = (StyledText) page.getControl();
			IDocument document = getDocument(viewer);
			if (document == null) {
				return;
			}
			AnsiConsoleStyleListener myListener = new AnsiConsoleStyleListener(document);
			viewer.addLineStyleListener(myListener);
			addViewer(viewer, this);
		}
	}

	// Find the document associated with the viewer
	static IDocument getDocument(StyledText viewer) {
		return viewer.getTypedListeners(ST.LineGetStyle, EventListener.class).filter(ITextViewer.class::isInstance)
				.findFirst().map(v -> ((ITextViewer) v).getDocument()).orElse(null);
	}

	private void addViewer(StyledText viewer, IConsolePageParticipant participant) {
		viewers.put(viewer, participant);
	}

	private void removeViewerWithPageParticipant(IConsolePageParticipant participant) {
		Set<StyledText> toRemove = new HashSet<>();

		for (Entry<StyledText, IConsolePageParticipant> entry : viewers.entrySet()) {
			if (entry.getValue() == participant) {
				toRemove.add(entry.getKey());
			}
		}

		for (StyledText viewer : toRemove) {
			viewers.remove(viewer);
		}
	}
}

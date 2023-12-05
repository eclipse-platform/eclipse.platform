/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor.text;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;

public class NotifyingReconciler extends MonoReconciler {

	private final ArrayList<IReconcilingParticipant> fReconcilingParticipants = new ArrayList<>();

	/**
	 * Constructor for NotifyingReconciler.
	 */
	public NotifyingReconciler(IReconcilingStrategy strategy) {
		super(strategy, false);
	}

	@Override
	protected void process(DirtyRegion dirtyRegion) {
		super.process(dirtyRegion);
		notifyReconcilingParticipants();
	}

	public void addReconcilingParticipant(IReconcilingParticipant participant) {
		fReconcilingParticipants.add(participant);
	}

	public void removeReconcilingParticipant(IReconcilingParticipant participant) {
		fReconcilingParticipants.remove(participant);
	}

	protected void notifyReconcilingParticipants() {
		Iterator<IReconcilingParticipant> i = new ArrayList<>(fReconcilingParticipants).iterator();
		while (i.hasNext()) {
			i.next().reconciled();
		}
	}

	@Override
	protected void initialProcess() {
		super.initialProcess();
		notifyReconcilingParticipants();
	}
}

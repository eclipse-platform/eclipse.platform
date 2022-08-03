/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.swt.graphics.Image;

/**
 * An abstract compare input whose purpose is to support change notification
 * through a {@link CompareInputChangeNotifier}.
 */
public abstract class AbstractCompareInput implements ICompareInput {

	private ITypedElement ancestor;
	private ITypedElement left;
	private ITypedElement right;
	private int kind;
	private final ListenerList<ICompareInputChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

	public AbstractCompareInput(int kind,
			ITypedElement ancestor,
			ITypedElement left,
			ITypedElement right) {
				this.kind = kind;
				this.ancestor = ancestor;
				this.left = left;
				this.right = right;
	}

	@Override
	public void addCompareInputChangeListener(
			ICompareInputChangeListener listener) {
		if (!containsListener(listener)) {
			listeners.add(listener);
			getChangeNotifier().connect(this);
		}
	}

	@Override
	public void removeCompareInputChangeListener(
			ICompareInputChangeListener listener) {
		if (containsListener(listener)) {
			listeners.remove(listener);
			getChangeNotifier().disconnect(this);
		}
	}

	/**
	 * Fire a compare input change event.
	 * This method must be called from the UI thread.
	 */
	protected void fireChange() {
		if (!listeners.isEmpty()) {
			Object[] allListeners = listeners.getListeners();
			for (Object l : allListeners) {
				final ICompareInputChangeListener listener = (ICompareInputChangeListener) l;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.compareInputChanged(AbstractCompareInput.this);
					}
					@Override
					public void handleException(Throwable exception) {
						// Logged by the safe runner
					}
				});
			}
		}
	}

	private boolean containsListener(ICompareInputChangeListener listener) {
		if (listeners.isEmpty())
			return false;
		Object[] allListeners = listeners.getListeners();
		for (Object object : allListeners) {
			if (object == listener)
				return true;
		}
		return false;
	}

	@Override
	public void copy(boolean leftToRight) {
		Assert.isTrue(false, "Copy is not support by this type of compare input"); //$NON-NLS-1$
	}

	@Override
	public ITypedElement getAncestor() {
		return ancestor;
	}

	@Override
	public Image getImage() {
		return getMainElement().getImage();
	}

	/**
	 * Return the main non-null element that identifies
	 * this input. By default, the left is returned if non-null.
	 * If the left is null, the right is returned. If both the
	 * left and right are null the ancestor is returned.
	 * @return the main non-null element that identifies
	 * this input
	 */
	private ITypedElement getMainElement() {
		if (left != null)
			return left;
		if (right != null)
			return right;
		return ancestor;
	}

	@Override
	public int getKind() {
		return kind;
	}

	/**
	 * Set the kind of this compare input
	 * @param kind the new kind
	 */
	public void setKind(int kind) {
		this.kind = kind;
	}

	@Override
	public ITypedElement getLeft() {
		return left;
	}

	@Override
	public String getName() {
		return getMainElement().getName();
	}

	@Override
	public ITypedElement getRight() {
		return right;
	}

	/**
	 * Return the change notifier that will call {@link #fireChange()}
	 * when the state of the compare input changes.
	 * @return the change notifier
	 */
	protected abstract CompareInputChangeNotifier getChangeNotifier();

	/**
	 * Set the ancestor of this compare input.
	 * @param ancestor the ancestor
	 */
	public void setAncestor(ITypedElement ancestor) {
		this.ancestor = ancestor;
	}

	/**
	 * Set the left element of this compare input.
	 * @param left the left element
	 */
	public void setLeft(ITypedElement left) {
		this.left = left;
	}

	/**
	 * Set the right element of this compare input.
	 * @param right the right element
	 */
	public void setRight(ITypedElement right) {
		this.right = right;
	}

	/**
	 * Update the compare input and fire change notification.
	 */
	public abstract void update();

	/**
	 * Return whether this compare input needs to be updated.
	 * @return whether this compare input needs to be updated
	 */
	public abstract boolean needsUpdate();


}

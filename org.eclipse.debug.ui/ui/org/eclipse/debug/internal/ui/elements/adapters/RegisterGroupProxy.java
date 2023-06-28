/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.elements.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.core.commands.Request;
import org.eclipse.debug.internal.ui.viewers.model.ViewerAdapterService;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementCompareRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IMemento;

/**
 * Used as input to the registers view for a stack frame. Insulates register groups
 * that do not change across stack frame selection to avoid register groups collapsing
 * while stepping between frames.
 * <p>
 * The standard debug model {@link IStackFrame} uses an
 * {@link org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputProvider} to
 * create a register group proxy for the register view's input.
 * </p>
 * <p>
 * This class delegates to the underlying stack frame for the following adapters. This way,
 * if a standard model provides custom adapters they are still used to present custom content
 * in the view and provide stable register groups while stepping.
 * <ul>
 * <li>{@link IModelProxyFactory}</li>
 * <li>{@link IColumnPresentationFactory}</li>
 * <li>{@link IElementContentProvider}</li>
 * <li>{@link IElementMementoProvider}</li>
 * </ul>
 * </p>
 * @since 3.4
 */
public class RegisterGroupProxy implements IModelProxyFactory, IColumnPresentationFactory, IElementContentProvider, IElementMementoProvider {

	private IRegisterGroup[] fGroups;
	private IStackFrame fFrame;

	private static final String HASH_CODE = "HASH_CODE"; //$NON-NLS-1$

	/**
	 * Local implementation of a viewer update request. This class delegates to the underlying frame
	 * for viewer requests. The requests have to be wrapped such that the request's element provided
	 * for existing clients is the underlying frame, rather than the register group proxy (as existing
	 * models do not know or need to know about the proxy).
	 */
	private class Update extends Request implements IViewerUpdate {
		private IViewerUpdate fViewerUpdate;

		Update(IViewerUpdate update) {
			fViewerUpdate = update;
		}

		@Override
		public Object getElement() {
			return fFrame;
		}

		@Override
		public TreePath getElementPath() {
			return TreePath.EMPTY;
		}

		@Override
		public IPresentationContext getPresentationContext() {
			return fViewerUpdate.getPresentationContext();
		}

		@Override
		public void done() {
			fViewerUpdate.setStatus(getStatus());
			fViewerUpdate.done();
		}

		@Override
		public Object getViewerInput() {
			return fFrame;
		}

	}

	private class CountUpdate extends Update implements IChildrenCountUpdate {

		private IChildrenCountUpdate fUpdate;

		CountUpdate(IChildrenCountUpdate delegate) {
			super(delegate);
			fUpdate = delegate;
		}

		@Override
		public void setChildCount(int numChildren) {
			fUpdate.setChildCount(numChildren);
		}

	}

	private class HasUpdate extends Update implements IHasChildrenUpdate {

		private IHasChildrenUpdate fUpdate;

		HasUpdate(IHasChildrenUpdate delegate) {
			super(delegate);
			fUpdate = delegate;
		}

		@Override
		public void setHasChilren(boolean hasChildren) {
			fUpdate.setHasChilren(hasChildren);
		}

	}

	private class ChildrenUpdate extends Update implements IChildrenUpdate {

		private IChildrenUpdate fUpdate;

		ChildrenUpdate(IChildrenUpdate delegate) {
			super(delegate);
			fUpdate = delegate;
		}

		@Override
		public int getLength() {
			return fUpdate.getLength();
		}

		@Override
		public int getOffset() {
			return fUpdate.getOffset();
		}

		@Override
		public void setChild(Object child, int offset) {
			fUpdate.setChild(child, offset);
		}

	}

	/**
	 * The memento request has to override {@link #getElement()} to provide the element
	 * that a memento is requested for (which could be any element in the view, not just
	 * the root stack frame).
	 */
	private class MementoRequest extends Update implements IElementMementoRequest {

		private IElementMementoRequest fUpdate;
		MementoRequest(IElementMementoRequest request) {
			super(request);
			fUpdate = request;
		}

		@Override
		public IMemento getMemento() {
			return fUpdate.getMemento();
		}
		@Override
		public Object getElement() {
			return fUpdate.getElement();
		}
		@Override
		public TreePath getElementPath() {
			return fUpdate.getElementPath();
		}

	}

	private class ElementCompare extends MementoRequest implements IElementCompareRequest {

		private IElementCompareRequest fRequest;
		ElementCompare(IElementCompareRequest request) {
			super(request);
			fRequest = request;
		}

		@Override
		public void setEqual(boolean equal) {
			fRequest.setEqual(equal);
		}

	}

	/**
	 * Creates a new register group proxy for the given stack frame.
	 *
	 * @param frame stack frame
	 * @throws DebugException exception if unable to retrieve register groups
	 */
	public RegisterGroupProxy(IStackFrame frame) throws DebugException {
		fFrame = frame;
		init(frame);
	}

	/*
	 * A register group proxy is equal to other stack frames that have the same
	 * register groups.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RegisterGroupProxy) {
			return Arrays.equals(fGroups, ((RegisterGroupProxy)obj).fGroups);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int code = getClass().hashCode();
		for (IRegisterGroup fGroup : fGroups) {
			code += fGroup.hashCode();
		}
		return code;
	}

	/**
	 * Initializes the register groups for this stack frame.
	 *
	 * @param frame stack frame
	 */
	private void init(IStackFrame frame) throws DebugException {
		fGroups = frame.getRegisterGroups();
	}

	/**
	 * Returns cached register groups for this stack frame.
	 *
	 * @return register groups
	 */
	protected IRegisterGroup[] getRegisterGroups() {
		return fGroups;
	}

	@Override
	public IModelProxy createModelProxy(Object element, IPresentationContext context) {
		IModelProxyFactory factory = ViewerAdapterService.getModelProxyFactory(fFrame);
		if (factory != null) {
			return factory.createModelProxy(fFrame, context);
		}
		return null;
	}

	@Override
	public IColumnPresentation createColumnPresentation(IPresentationContext context, Object element) {
		IColumnPresentationFactory factory = ViewerAdapterService.getColumnPresentationFactory(fFrame);
		if (factory != null) {
			return factory.createColumnPresentation(context, fFrame);
		}
		return null;
	}

	@Override
	public String getColumnPresentationId(IPresentationContext context, Object element) {
		IColumnPresentationFactory factory = ViewerAdapterService.getColumnPresentationFactory(fFrame);
		if (factory != null) {
			return factory.getColumnPresentationId(context, fFrame);
		}
		return null;
	}

	@Override
	public void update(IChildrenCountUpdate[] updates) {
		IElementContentProvider provider = ViewerAdapterService.getContentProvider(fFrame);
		if (provider != null) {
			IChildrenCountUpdate[] others = new IChildrenCountUpdate[updates.length];
			for (int i = 0; i < updates.length; i++) {
				others[i] = new CountUpdate(updates[i]);
			}
			provider.update(others);
		} else {
			cancelUpdates(updates);
		}
	}

	@Override
	public void update(IChildrenUpdate[] updates) {
		IElementContentProvider provider = ViewerAdapterService.getContentProvider(fFrame);
		if (provider != null) {
			IChildrenUpdate[] others = new IChildrenUpdate[updates.length];
			for (int i = 0; i < updates.length; i++) {
				others[i] = new ChildrenUpdate(updates[i]);
			}
			provider.update(others);
		} else {
			cancelUpdates(updates);
		}

	}

	@Override
	public void update(IHasChildrenUpdate[] updates) {
		IElementContentProvider provider = ViewerAdapterService.getContentProvider(fFrame);
		if (provider != null) {
			IHasChildrenUpdate[] others = new IHasChildrenUpdate[updates.length];
			for (int i = 0; i < updates.length; i++) {
				others[i] = new HasUpdate(updates[i]);
			}
			provider.update(others);
		} else {
			cancelUpdates(updates);
		}
	}

	/**
	 * Cancels a collection of update requests.
	 *
	 * @param updates updates to cancel
	 */
	private void cancelUpdates(IViewerUpdate[] updates) {
		for (IViewerUpdate update : updates) {
			update.setStatus(Status.CANCEL_STATUS);
			update.done();
		}
	}

	@Override
	public void compareElements(IElementCompareRequest[] requests) {
		IElementMementoProvider provider = ViewerAdapterService.getMementoProvider(fFrame);
		if (provider != null) {
			List<IElementCompareRequest> others = new ArrayList<>(requests.length);
			for (IElementCompareRequest request : requests) {
				if (request.getElement().equals(this)) {
					Integer integer = request.getMemento().getInteger(HASH_CODE);
					if (integer != null) {
						request.setEqual(integer.intValue() == hashCode());
					} else {
						request.setEqual(false);
					}
					request.done();
				} else {
					others.add(new ElementCompare(request));
				}
			}
			if (!others.isEmpty()) {
				provider.compareElements(others.toArray(new IElementCompareRequest[others.size()]));
			}
		} else {
			cancelUpdates(requests);
		}
	}

	@Override
	public void encodeElements(IElementMementoRequest[] requests) {
		IElementMementoProvider provider = ViewerAdapterService.getMementoProvider(fFrame);
		if (provider != null) {
			List<IElementMementoRequest> others = new ArrayList<>(requests.length);
			for (IElementMementoRequest request : requests) {
				if (request.getElement().equals(this)) {
					request.getMemento().putInteger(HASH_CODE, this.hashCode());
					request.done();
				} else {
					others.add(new MementoRequest(request));
				}
			}
			if (!others.isEmpty()) {
				provider.encodeElements(others.toArray(new IElementMementoRequest[others.size()]));
			}
		} else {
			cancelUpdates(requests);
		}
	}

}

/*******************************************************************************
 *  Copyright (c) 2004, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     WindRiver - Bug 192028 [Memory View] Memory view does not
 *                 display memory blocks that do not reference IDebugTarget
 *     Wind River Systems - Ted Williams - [Memory View] Memory View: Workflow Enhancements (Bug 215432)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.memory.AbstractMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingBindingsListener;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * The rendering to allow users to create a rendering.
 * @since 3.1
 */
public class CreateRendering extends AbstractMemoryRendering implements IMemoryRenderingBindingsListener {

	private ListViewer fViewer;
	private Label fMemoryBlockLabel;
	private IMemoryRenderingContainer fContainer;
	private Composite fCanvas;
	private String fLabel;

	private String fTabLabel;

	public CreateRendering(IMemoryRenderingContainer container)
	{
		super("org.eclipse.debug.internal.ui.views.createrendering"); //$NON-NLS-1$
		fContainer = container;
	}

	static class MemoryRenderingLabelProvider implements ILabelProvider
	{

		@Override
		public Image getImage(Object element) {
			return DebugUIPlugin.getImageDescriptorRegistry().get(
					DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_MONITOR_EXPRESSION));
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IMemoryRenderingType)
			{
				String label = ((IMemoryRenderingType)element).getLabel();
				return label;
			}
			return element.toString();
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

	}

	static class MemoryRenderingContentProvider implements IStructuredContentProvider
	{

		@Override
		public Object[] getElements(Object inputElement) {
			IMemoryRenderingType[] renderings = DebugUITools.getMemoryRenderingManager().getRenderingTypes((IMemoryBlock)inputElement);
			return renderings;
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	@Override
	public Control createControl(Composite parent) {
		fCanvas = new Composite(parent, SWT.NONE);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.numColumns = 2;
		compositeLayout.makeColumnsEqualWidth = false;
		fCanvas.setLayout(compositeLayout);

		GridData comositeSpec= new GridData();
		comositeSpec.grabExcessVerticalSpace= true;
		comositeSpec.grabExcessHorizontalSpace= true;
		comositeSpec.horizontalAlignment= GridData.FILL;
		comositeSpec.verticalAlignment= GridData.CENTER;
		fCanvas.setLayoutData(comositeSpec);

		fMemoryBlockLabel = new Label(fCanvas, SWT.BORDER);

		String memoryBlockLabel = " "; //$NON-NLS-1$
		memoryBlockLabel = getLabel();

		fMemoryBlockLabel.setText("  " + DebugUIMessages.CreateRenderingTab_Memory_monitor + memoryBlockLabel + "  "); //$NON-NLS-1$ //$NON-NLS-2$
		GridData textLayout = new GridData();
		textLayout.verticalAlignment=GridData.CENTER;
		textLayout.horizontalAlignment=GridData.BEGINNING;
		fMemoryBlockLabel.setLayoutData(textLayout);

		Label renderingLabel = new Label(fCanvas, SWT.NONE);
		renderingLabel.setText(DebugUIMessages.CreateRenderingTab_Select_renderings_to_create);
		GridData renderingLayout = new GridData();
		renderingLayout.horizontalAlignment = GridData.BEGINNING;
		renderingLayout.verticalAlignment = GridData.CENTER;
		renderingLayout.horizontalSpan = 2;
		renderingLabel.setLayoutData(renderingLayout);

		fViewer = new ListViewer(fCanvas);
		fViewer.setContentProvider(new MemoryRenderingContentProvider());
		fViewer.setLabelProvider(new MemoryRenderingLabelProvider());
		fViewer.setInput(getMemoryBlock());

		if (fViewer.getElementAt(0) != null)
		{
			fViewer.getList().select(0);
		}

		GridData listLayout = new GridData(GridData.FILL_BOTH);
		listLayout.horizontalSpan = 1;
		fViewer.getControl().setLayoutData(listLayout);

		fViewer.addDoubleClickListener(event -> addRenderings());

		// listen for enter being pressed
		fViewer.getList().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR) {
					addRenderings();
				}
			}});

		Button addButton = new Button(fCanvas, SWT.NONE);
		addButton.setText(DebugUIMessages.CreateRenderingTab_Add_renderings);
		GridData buttonLayout = new GridData();
		buttonLayout.horizontalAlignment = GridData.BEGINNING;
		buttonLayout.verticalAlignment = GridData.BEGINNING;
		addButton.setLayoutData(buttonLayout);

		addButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				addRenderings();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				addRenderings();
			}});

		DebugUITools.getMemoryRenderingManager().addListener(this);

		return fCanvas;
	}

	private void addRenderings()
	{
		ISelection selection = fViewer.getSelection();
		Object[] renderings = null;

		if (selection instanceof IStructuredSelection)
		{
			IStructuredSelection strucSelection = (IStructuredSelection)selection;

			renderings = strucSelection.toArray();
		}

		if (renderings == null)
		{
			Status stat = new Status(
					IStatus.ERROR,DebugUIPlugin.getUniqueIdentifier(),
					DebugException.INTERNAL_ERROR, DebugUIMessages.CreateRenderingTab_0, null);
			DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), DebugUIMessages.CreateRenderingTab_1, DebugUIMessages.CreateRenderingTab_2, stat); //
			return;
		}

		// ask for debug target and memory block retrieval
		IMemoryBlockRetrieval standardMemRetrieval = MemoryViewUtil.getMemoryBlockRetrieval(getMemoryBlock());

		if (standardMemRetrieval == null) {
			return;
		}

		// make a copy of the container, may be diposed when a rendering is added
		IMemoryRenderingContainer container = fContainer;
		// add memory renderings to Memory Rendering Manager
		for (Object r : renderings) {
			if (r instanceof IMemoryRenderingType) {
				try {
					IMemoryRendering rendering = ((IMemoryRenderingType) r).createRendering();
					if (rendering != null)
					{
						rendering.init(container, getMemoryBlock());
						container.addMemoryRendering(rendering);
					}
				} catch (CoreException e) {

					MemoryViewUtil.openError(DebugUIMessages.CreateRendering_0, DebugUIMessages.CreateRendering_1, e);  //
				}
			}
		}
	}

	@Override
	public void dispose() {
		fViewer = null;
		fCanvas = null;
		fContainer = null;
		fMemoryBlockLabel = null;
		DebugUITools.getMemoryRenderingManager().removeListener(this);
	}

	@Override
	public Control getControl() {
		return fCanvas;
	}
	@Override
	public void memoryRenderingBindingsChanged() {
		if (fViewer != null) {
			fViewer.refresh();
		}
	}

	@Override
	public String getLabel() {
		if (fTabLabel == null)
		{
			fTabLabel = DebugUIMessages.CreateRendering_2;
			updateRenderingLabel();
		}

		return fTabLabel;
	}

	@Override
	public Image getImage() {
		return DebugUIPlugin.getImageDescriptorRegistry().get(
				DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_MONITOR_EXPRESSION));
	}

	@Override
	public void becomesVisible() {
		// Do nothing, do not want to connect/disconnect from clients
	}

	@Override
	public void becomesHidden() {
		// Do nothing, do not want to connect/disconnect from clients
	}

	protected void updateRenderingLabel()
	{
		Job job = new Job("Update Rendering Label"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				fLabel = CreateRendering.super.getLabel();
				fTabLabel = DebugUIMessages.CreateRenderingTab_label ;

				firePropertyChangedEvent(new PropertyChangeEvent(CreateRendering.this, IBasicPropertyConstants.P_TEXT, null, fTabLabel));

				WorkbenchJob wbJob = new WorkbenchJob("Create Rendering Update Label"){ //$NON-NLS-1$

					@Override
					public IStatus runInUIThread(IProgressMonitor wbMonitor) {
						if (fMemoryBlockLabel != null)
						{
							fMemoryBlockLabel.setText(DebugUIMessages.CreateRenderingTab_Memory_monitor + " " + fLabel );  //$NON-NLS-1$
							fMemoryBlockLabel.getParent().layout();
						}
						return Status.OK_STATUS;
					}};
				wbJob.setSystem(true);
				wbJob.schedule();
				return Status.OK_STATUS;
			}};
		job.setSystem(true);
		job.schedule();
	}
}

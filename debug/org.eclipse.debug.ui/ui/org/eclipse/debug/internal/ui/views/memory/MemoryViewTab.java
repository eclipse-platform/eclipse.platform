/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
 *     Wind River Systems - Ted Williams - [Memory View] Memory View: Workflow Enhancements (Bug 215432)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.views.memory.renderings.ErrorRendering;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Represents a tab in the Memory View. This is where memory renderings are
 * hosted in the Memory View.
 *
 * @since 3.1
 */
public class MemoryViewTab implements IMemoryViewTab, IPropertyChangeListener, Listener {

	private IMemoryRendering fRendering;
	private CTabItem fTabItem;
	private DisposeListener fDisposeListener;
	private boolean fEnabled;
	private boolean fIsDisposed = false;
	private Control fControl;
	private RenderingViewPane fContainer;

	public MemoryViewTab(CTabItem tabItem, IMemoryRendering rendering, RenderingViewPane container) {
		fTabItem = tabItem;
		fRendering = rendering;
		fContainer = container;

		// set the rendering as the synchronization provider
		// as the new rendering should be in focus and have control
		// after it's created

		if (container.getMemoryRenderingSite().getSynchronizationService() != null) {
			container.getMemoryRenderingSite().getSynchronizationService().setSynchronizationProvider(rendering);
		}
		Control control = createViewTab();

		control.addListener(SWT.Activate, this);
		control.addListener(SWT.Deactivate, this);

		fTabItem.setControl(control);
		fTabItem.setData(this);
		fTabItem.setText(getLabel());
		fTabItem.setImage(getImage());

		fTabItem.addDisposeListener(fDisposeListener = e -> {
			fTabItem.removeDisposeListener(fDisposeListener);
			dispose();
		});
	}

	private Control createViewTab() {
		ISafeRunnable safeRunnable = new ISafeRunnable() {

			@Override
			public void handleException(Throwable exception) {
				// create an error rendering to fill the view tab
				ErrorRendering rendering = new ErrorRendering(fRendering.getRenderingId(), exception);
				rendering.init(fContainer, fRendering.getMemoryBlock());

				// dispose the rendering
				fRendering.dispose();

				fRendering = rendering;
				fControl = rendering.createControl(fTabItem.getParent());
			}

			@Override
			public void run() throws Exception {
				fControl = fRendering.createControl(fTabItem.getParent());
				fRendering.addPropertyChangeListener(getInstance());
			}
		};

		SafeRunner.run(safeRunnable);
		return fControl;
	}

	private String getLabel() {
		return fRendering.getLabel();
	}

	private Image getImage() {
		return fRendering.getImage();
	}

	@Override
	public void dispose() {

		if (fIsDisposed) {
			return;
		}

		fIsDisposed = true;

		fRendering.removePropertyChangeListener(this);

		if (!fControl.isDisposed()) {
			fControl.removeListener(SWT.Activate, this);
			fControl.removeListener(SWT.Deactivate, this);
		}

		// always deactivate rendering before disposing it.
		fRendering.deactivated();

		fRendering.dispose();
	}

	@Override
	public boolean isDisposed() {
		return fIsDisposed;
	}

	@Override
	public boolean isEnabled() {
		return fEnabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		fEnabled = enabled;

		if (fEnabled) {
			fRendering.becomesVisible();
		} else {
			fRendering.becomesHidden();
		}

	}

	@Override
	public void setTabLabel(String label) {
		fTabItem.setText(label);
	}

	@Override
	public String getTabLabel() {
		return fTabItem.getText();
	}

	@Override
	public IMemoryRendering getRendering() {
		return fRendering;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {

		// make sure this runs on the UI thread, otherwise, it
		// will get to a swt exception

		WorkbenchJob job = new WorkbenchJob("MemoryViewTab PropertyChanged") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (isDisposed()) {
					return Status.OK_STATUS;
				}

				if (event.getSource() == fRendering) {
					if (event.getProperty().equals(IBasicPropertyConstants.P_TEXT)) {
						Object value = event.getNewValue();
						if (value != null && value instanceof String) {
							String label = (String) value;
							setTabLabel(label);
						} else {
							setTabLabel(fRendering.getLabel());
						}
					}

					if (event.getProperty().equals(IBasicPropertyConstants.P_IMAGE)) {
						Object value = event.getNewValue();
						if (value != null && value instanceof Image) {
							Image image = (Image) value;
							fTabItem.setImage(image);
						} else {
							fTabItem.setImage(fRendering.getImage());
						}
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	private MemoryViewTab getInstance() {
		return this;
	}

	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.Activate) {
			fRendering.activated();
			fContainer.setRenderingSelection(fRendering);
		}
		if (event.type == SWT.Deactivate) {
			fRendering.deactivated();
		}
	}
}

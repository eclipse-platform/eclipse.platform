/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.contexts;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.contexts.ISuspendTrigger;
import org.eclipse.debug.ui.contexts.ISuspendTriggerListener;

/**
 * @since 3.2
 */
public class LaunchSuspendTrigger implements ISuspendTrigger, IDebugEventSetListener {

	private ListenerList<ISuspendTriggerListener> fListeners = new ListenerList<>();
	private SuspendTriggerAdapterFactory fFactory = null;
	private ILaunch fLaunch = null;

	public LaunchSuspendTrigger(ILaunch launch, SuspendTriggerAdapterFactory factory) {
		fFactory = factory;
		fLaunch = launch;
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	public ILaunch getLaunch() {
		return fLaunch;
	}

	protected void dispose() {
		DebugPlugin.getDefault().removeDebugEventListener(this);
		fListeners = null;
		fFactory.dispose(this);
	}

	@Override
	public void addSuspendTriggerListener(ISuspendTriggerListener listener) {
		if (fListeners != null) {
			fListeners.add(listener);
		}
	}

	@Override
	public void removeSuspendTriggerListener(ISuspendTriggerListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
		if (fListeners.size() == 0) {
			dispose();
		}
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		// open the debugger if this is a suspend event and the debug view is not yet open
		// and the preferences are set to switch
		for (DebugEvent event : events) {
			if (event.getKind() == DebugEvent.SUSPEND && !event.isEvaluation() && event.getDetail() != DebugEvent.STEP_END) {
				//				 Don't switch perspective for evaluations or stepping
				Object source = event.getSource();
				if (source instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) source;
					ILaunch launch = adaptable.getAdapter(ILaunch.class);
					if (fLaunch.equals(launch)) {
						// only notify for this launch
						notifySuspend(event);
					}
				}

			}
		}
	}

	/**
	 * @param event
	 */
	private void notifySuspend(DebugEvent event) {
		Object source = event.getSource();
		if (source instanceof IDebugElement) {
			final ILaunch launch = ((IDebugElement)source).getLaunch();
			Object context = null;
			if (source instanceof IThread) {
				try {
					context = ((IThread)source).getTopStackFrame();
				} catch (DebugException e) {
				}
			} else if (source instanceof IDebugTarget) {
				context = source;
			}
			final Object temp = context;
			ListenerList<ISuspendTriggerListener> list = fListeners;
			if (list != null) {
				for (ISuspendTriggerListener iSuspendTriggerListener : list) {
					final ISuspendTriggerListener listener = iSuspendTriggerListener;
					SafeRunner.run(new ISafeRunnable() {
						@Override
						public void run() throws Exception {
							listener.suspended(launch, temp);
						}

						@Override
						public void handleException(Throwable exception) {
							DebugUIPlugin.log(exception);
						}

					});
				}
			}

		}

	}

}

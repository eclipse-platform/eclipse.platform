/****************************************************************************
 * Copyright (c) 2017, 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Wim Jongman <wim.jongman@remainsoftware.com> - initial API and implementation
 *****************************************************************************/
package org.eclipse.tips.core;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tips.core.internal.FinalTip;
import org.eclipse.tips.core.internal.LogUtil;

/**
 * Class to provide tips to the tip framework. It is the job of this provider to
 * manage its tips. Examples of managing tips are:
 *
 * <ul>
 * <li>Loading tips from the Internet</li>
 * <li>Serve next, previous and current tip on request</li>
 * </ul>
 *
 * After the TipProvider is instantiated by the {@link ITipManager}, the
 * TipManager will insert itself by calling {@link #setManager(ITipManager)}.
 * Then the TipManager will asynchronous call this providers'
 * {@link #loadNewTips(IProgressMonitor)} method. The job of the load() method
 * is to do long work like fetching new tips from the Internet and storing them
 * locally. There is no defined method on how tips should be stored locally,
 * implementers are free to do what is needed.
 *
 * The constructor must return fast, meaning that tips may not be fetched from
 * the Internet in the constructor. This should be done in the
 * {@link #loadNewTips(IProgressMonitor)} method.
 *
 * To indicate that this provider is ready to serve tips, it should call the
 * {@link #setTips(List)} method which then sets its <code>ready</code> flag.
 *
 */
public abstract class TipProvider {

	/**
	 * Ready property.
	 */
	public static final String PROP_READY = "PR"; //$NON-NLS-1$

	private ITipManager fTipManager;
	private int fTipIndex;
	private List<Tip> fTips = new ArrayList<>();
	private Tip fCurrentTip;
	private boolean fReady;
	private PropertyChangeSupport fChangeSupport = new PropertyChangeSupport(this);
	private Tip fFinalTip = new FinalTip(getID());
	private String fExpression;

	/**
	 * A predicate that tests if a tip is valid based on its read state and the
	 * requirement to only serve read tips. Subclasses may replace this predicate if
	 * they want to add some additional tests.
	 */
	private Predicate<Tip> fUnreadTipPredicate = pTip -> {
		if (getManager().mustServeReadTips()) {
			return true;
		}
		return !getManager().isRead(pTip);
	};

	/**
	 * The zero argument constructor must be able to instantiate the TipProvider.
	 * This method may also be used to quickly set the available tips by calling the
	 * {@link #setTips(List)} method. The constructor may not be used to load tips
	 * from the Internet. Use the {@link #loadNewTips(IProgressMonitor)} method for
	 * this purpose.
	 *
	 * @see #loadNewTips(IProgressMonitor)
	 * @see #setTips(List)
	 */
	public TipProvider() {
	}

	/**
	 * Provides the opportunity to release all held resources.
	 */
	public abstract void dispose();

	/**
	 * @return the short description of this provider.
	 */
	public abstract String getDescription();

	/**
	 * @return the ID of this provider
	 */
	public abstract String getID();

	/**
	 * The image used by the UI for low resolution
	 *
	 * @return a 48x48 {@link TipImage}
	 */
	public abstract TipImage getImage();

	/**
	 * @return the {@link Tip} that was last returned by {@link #getNextTip()} or
	 *         {@link #getPreviousTip()}
	 */
	public synchronized Tip getCurrentTip() {
		if (fCurrentTip == null) {
			return getNextTip();
		}
		return fCurrentTip;
	}

	/**
	 * The next {@link Tip} is returned based on the read status of the Tip and the
	 * fact if already read tips must be served or not which is known by the
	 * {@link ITipManager}: ({@link ITipManager#mustServeReadTips()}).
	 *
	 * @return the next {@link Tip}
	 * @see #getPreviousTip()
	 * @see #getCurrentTip()
	 */
	public synchronized Tip getNextTip() {
		List<Tip> list = getTips(fUnreadTipPredicate);
		if (list.isEmpty()) {
			return setCurrentTip(fFinalTip);
		}
		if (fCurrentTip != null && (getManager().mustServeReadTips() || getManager().isRead(fCurrentTip))) {
			fTipIndex++;
		}
		if (fTipIndex >= list.size()) {
			fTipIndex = 0;
		}
		return setCurrentTip(list.get(fTipIndex));
	}

	/**
	 * @return the previous {@link Tip}
	 * @see #getNextTip()
	 * @see #getCurrentTip()
	 */
	public Tip getPreviousTip() {
		List<Tip> list = getTips(fUnreadTipPredicate);
		if (list.isEmpty()) {
			return setCurrentTip(fFinalTip);
		}
		fTipIndex--;
		if (fTipIndex < 0) {
			fTipIndex = list.size() - 1;
		}
		return setCurrentTip(list.get(fTipIndex));
	}

	/**
	 * @return the {@link ITipManager} of this provider, never null.
	 */
	public synchronized ITipManager getManager() {
		return fTipManager;
	}

	/**
	 * @return true if the provider is ready to deliver tips
	 */
	public final boolean isReady() {
		return fReady;
	}

	/**
	 * Is called asynchronously during startup of the TipManager to gather new tips.
	 *
	 * The provider is not available to the UI unless it has called it's
	 * {@link #setTips(List)} method. It is therefore possible that the provider is
	 * not immediately visible in the tip UI but will be added later.
	 * <p>
	 * If you run out of tips and you feel that you should load more tips on your
	 * own then you can also asynchronously call this method. A good place would be
	 * to override {@link #getTips(boolean)}, check if the supply of tips is
	 * sufficient and then call this method asynchronously.
	 * <p>
	 * One strategy is to do a long running fetch in this method and then store the
	 * tips locally. On the next run of the TipManager, the fetched tips can be
	 * served from the constructor (i.e. by calling {@link #setTips(List)}), making
	 * them available immediately
	 *
	 * @param monitor The monitor to report back progress.
	 * @return the status in case you want to report problems.
	 * @see TipProvider#setTips(List)
	 * @see TipProvider#isReady()
	 */
	public abstract IStatus loadNewTips(IProgressMonitor monitor);

	private synchronized Tip setCurrentTip(Tip pTip) {
		fCurrentTip = pTip;
		return fCurrentTip;
	}

	/**
	 * Sets the TipManager. You should probably not call this method directly. This
	 * method is normally called after the provider is instantiated by the
	 * {@link ITipManager}. If you create the provider yourself you should register
	 * the provider with {@link ITipManager#register(TipProvider)} which in turn
	 * will call this method. Subclasses may override but must not forget to call
	 * super in order to save the {@link ITipManager}.
	 *
	 * @param tipManager the {@link ITipManager}
	 * @return this
	 */
	public synchronized TipProvider setManager(ITipManager tipManager) {
		fTipManager = tipManager;
		return this;
	}

	/**
	 * A convenience method to get the list of tips based on the read status of the
	 * tip and the requirement to serve unread or all tips.
	 *
	 * @return the list of tips based on the description above
	 * @see ITipManager#mustServeReadTips()
	 * @see ITipManager#isRead(Tip)
	 */
	public List<Tip> getTips() {
		return getTips(fUnreadTipPredicate);
	}

	/**
	 * Get a list of tips filtered by the passed predicate.
	 *
	 * @param predicate a {@link Predicate} targeting a Tip object or null to return all tips
	 * @return an unmodifiable list of tips.
	 */
	public synchronized List<Tip> getTips(Predicate<Tip> predicate) {
		if (predicate != null) {
			return fTips.stream().filter(predicate) //
					.sorted(Comparator.comparing(Tip::getCreationDate).reversed()) //
					.toList();
		}
		return Collections.unmodifiableList(fTips);
	}

	/**
	 * Sets the tips for this provider, replacing the current set of tips, and sets
	 * the <code>ready</code> flag to true. This method is typically called from the
	 * constructor of the {@link TipProvider} but may also be called from the
	 * asynchronous {@link #loadNewTips(IProgressMonitor)} method.
	 *
	 * @param tips a list of {@link Tip} objects
	 * @return this
	 * @see #addTips(List)
	 * @see #isReady()
	 * @see #loadNewTips(IProgressMonitor)
	 */
	public TipProvider setTips(List<Tip> tips) {
//		if (!getManager().isOpen()) {
//			return this;
//		}
		getManager().log(LogUtil.info(Messages.TipProvider_0));
		doSetTips(tips, true);
		fReady = true;
		fChangeSupport.firePropertyChange(PROP_READY, false, true);
		return this;
	}

	/**
	 * Adds the passed tips to the set of tips this provider already has sets the
	 * <code>ready</code> flag to true. This method is typically called from the
	 * constructor of the {@link TipProvider} but may also be called from the
	 * asynchronous {@link #loadNewTips(IProgressMonitor)} method.
	 *
	 * @param tips a list of {@link Tip} objects
	 * @return this
	 * @see #setTips(List)
	 * @see #isReady()
	 * @see #loadNewTips(IProgressMonitor)
	 */
	public TipProvider addTips(List<Tip> tips) {
		doSetTips(tips, false);
		fReady = true;
		fChangeSupport.firePropertyChange(PROP_READY, false, true);
		return this;
	}

	private synchronized void doSetTips(List<Tip> tips, boolean replace) {
		if (replace) {
			fTips.clear();
		}
		fTips.addAll(tips);
	}

	/**
	 * Gets the change support so that interested parties can subscribe to the
	 * property change events of this provider.
	 *
	 * @return the {@link PropertyChangeSupport}
	 */
	public PropertyChangeSupport getChangeSupport() {
		return fChangeSupport;
	}

	/**
	 * Returns an expression that is used by the {@link ITipManager} to determine
	 * the priority of this provider. The expression can be used to advice the
	 * TipManager when the tips of this provider deserve priority. The Eclipse IDE
	 * TipManager uses the core expression from the o.e.core.runtime bundle.
	 * Example: The expression
	 *
	 * <pre>
	 *  &lt;with
	 *     variable="activeWorkbenchWindow.activePerspective"&gt;
	 *         &lt;equals value="org.eclipse.jdt.ui.JavaPerspective"&gt;&lt;/equals&gt;
	 *  &lt;/with&gt;
	 * </pre>
	 *
	 * will give the provider priority when the java perspective is active in the
	 * IDE
	 *
	 * @return the expression which can be empty or null.
	 */
	public String getExpression() {
		return fExpression;
	}

	/**
	 * Sets the expression to determine the priority of the provider.
	 *
	 * @param expression the expression, may be null.
	 *
	 * @see #getExpression()
	 */
	public void setExpression(String expression) {
		fExpression = expression;
	}
}
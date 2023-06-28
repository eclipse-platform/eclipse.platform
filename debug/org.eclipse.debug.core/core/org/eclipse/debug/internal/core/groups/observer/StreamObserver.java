/*******************************************************************************
 *  Copyright (c) 2012, 2016 SSI Schaefer and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      SSI Schaefer
 *******************************************************************************/
package org.eclipse.debug.internal.core.groups.observer;

import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

/**
 * The {@code StreamObserver} observes a given {@linkplain IStreamsProxy output stream} instance and
 * notifies a {@linkplain CountDownLatch synchronization object} when a given string appears in the
 * output.
 */
public class StreamObserver implements Runnable {
	private final String stringPattern;
	private final IProcess process;
	private final CountDownLatch countDownLatch;
	private final IProgressMonitor pMonitor;

	public StreamObserver(IProgressMonitor monitor, IProcess process, String pattern,
			CountDownLatch countDownLatch) {
		this.process = process;
		this.pMonitor = monitor;
		this.stringPattern = pattern;
		this.countDownLatch = countDownLatch;
	}

	@Override
	public void run() {
		// append wild card if not provided
		StringBuilder patternBuilder = new StringBuilder();
		if (!stringPattern.startsWith(".*")) { //$NON-NLS-1$
			patternBuilder.append(".*"); //$NON-NLS-1$
		}
		patternBuilder.append(stringPattern);
		if (!stringPattern.endsWith(".*")) { //$NON-NLS-1$
			patternBuilder.append(".*"); //$NON-NLS-1$
		}
		// create pattern and start listening to the output
		final Pattern pattern = Pattern.compile(patternBuilder.toString(), Pattern.MULTILINE);
		final IStreamMonitor outputStreamMonitor = process.getStreamsProxy()
				.getOutputStreamMonitor();
		outputStreamMonitor.addListener(new IStreamListener() {
			@Override
			public void streamAppended(String text, IStreamMonitor monitor) {
				if (countDownLatch.getCount() == 0) {
					outputStreamMonitor.removeListener(this);
					return;
				}

				Matcher matcher = pattern.matcher(text);
				if (!matcher.find() && !pMonitor.isCanceled()) {
					return;
				}
				countDownLatch.countDown();
			}
		});
	}
}

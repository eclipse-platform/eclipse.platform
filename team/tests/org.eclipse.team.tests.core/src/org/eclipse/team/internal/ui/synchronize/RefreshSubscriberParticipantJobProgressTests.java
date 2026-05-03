/*******************************************************************************
 * Copyright (c) 2026 Foxy BOA and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Foxy BOA - initial implementation, issue #2645
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.tests.core.mapping.ScopeTestSubscriber;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.junit.jupiter.api.Test;

/**
 * Regression test for issue #2645: {@link RefreshSubscriberParticipantJob#doRefresh
 * RefreshSubscriberParticipantJob.doRefresh} must split its progress monitor between
 * {@code subscriber.refresh} and {@code waitForCollector} instead of handing the same
 * monitor to both.
 */
public class RefreshSubscriberParticipantJobProgressTests {

	private static class CountingMonitor extends NullProgressMonitor {
		volatile double total;

		@Override
		public void worked(int work) {
			total += work;
		}

		@Override
		public void internalWorked(double work) {
			total += work;
		}
	}

	private static class ConsumingSubscriber extends ScopeTestSubscriber {
		@Override
		public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) {
			SubMonitor sm = SubMonitor.convert(monitor, 100);
			sm.worked(100);
			sm.done();
		}
	}

	private static class TestableJob extends RefreshSubscriberParticipantJob {
		private final Subscriber subscriberOverride;

		TestableJob(SubscriberParticipant participant, IResource[] resources, Subscriber subscriber) {
			super(participant, "test-job", "test-task", resources, null);
			this.subscriberOverride = subscriber;
		}

		@Override
		protected Subscriber getSubscriber() {
			return subscriberOverride;
		}

		@Override
		protected void waitForCollector(IProgressMonitor monitor) {
			SubMonitor sm = SubMonitor.convert(monitor, 100);
			sm.worked(100);
			sm.done();
		}

		void invokeDoRefresh(RefreshChangeListener changeDescription, IProgressMonitor monitor) throws TeamException {
			doRefresh(changeDescription, monitor);
		}
	}

	@Test
	public void doRefreshDoesNotOverConsumeParentMonitor() throws Exception {
		ConsumingSubscriber subscriber = new ConsumingSubscriber();
		SubscriberParticipant participant = new SubscriberParticipant() {
			@Override
			public Subscriber getSubscriber() {
				return subscriber;
			}

			@Override
			protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
				// not needed for this test
			}
		};

		TestableJob job = new TestableJob(participant, new IResource[0], subscriber);
		RefreshChangeListener changeDescription = new RefreshChangeListener(new IResource[0], null);

		CountingMonitor parent = new CountingMonitor();
		parent.beginTask("test", 100);
		job.invokeDoRefresh(changeDescription, parent);
		parent.done();

		// Without the fix, both consumers receive the same monitor and parent.total ~= 2000.
		// With the fix, doRefresh splits the parent 80/20 and parent.total ~= 1000.
		assertTrue(parent.total <= 1500,
				"Parent monitor over-consumed: total=" + parent.total + " (expected <= 1500)");
	}
}

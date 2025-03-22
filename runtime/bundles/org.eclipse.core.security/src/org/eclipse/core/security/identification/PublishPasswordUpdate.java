/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.security.identification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.eclipse.core.security.ActivateSecurity;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
@Component(immediate = true)
public class PublishPasswordUpdate implements PublishPasswordUpdateIfc {
	private static PublishPasswordUpdate INSTANCE;
	private final ExecutorService executor = Executors.newFixedThreadPool(10);
	private List<Subscriber<? super String>> subscribers = new ArrayList<>();

	private PublishPasswordUpdate() {
	}

	public static PublishPasswordUpdate getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new PublishPasswordUpdate();
		}
		return INSTANCE;
	}

	public void subscribe(Subscriber subscriber) {
		subscribers.add(subscriber);
	}

	public int getSubscriberCount() {
		return subscribers.size();
	}

	public void publishMessage(String message) {
		subscribers.forEach(subscriber -> {
			executor.submit(() -> {
				subscriber.onNext(message);
			});
		});
	}

	public void close() {
		subscribers.forEach(Subscriber::onComplete);
		executor.shutdown();
	}

	@Reference
	void bindSubscriberService(PublishPasswordUpdateIfc updateService) {
		// Call the service and print out result!
		//System.out.println("Current time on remote is: " + updateService.close());
		System.out.println("SUBSCRIBER COUNT:"+updateService.getSubscriberCount());
		ActivateSecurity.getInstance().log("PublishPasswordUpdate bindSubscriberService");
	}

	// Called by DS upon ITimeService undiscovery
	void unbindSubscriberService(PublishPasswordUpdateIfc updateService) {
		System.out.println("Undiscovered ITimeService via DS.  Instance=" + updateService);
	}
}

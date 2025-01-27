package org.eclipse.core.tests.pki.auth;
/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import org.eclipse.core.pki.pkiselection.SecurityOpRequest;

public class PKITestSubscriber<T> implements Subscriber<T> {
	private Subscription subscription;
	private String name;
	//private SSLContext sslContext;
	
	public PKITestSubscriber() {}
	public PKITestSubscriber(String name) {
		this.name = name;
	}
	public void setName(String s) {
		this.name=s;
	}
	public String getName() {
		return this.name;
	}
	public void onSubscribe(Subscription subscription) {
		// TODO Auto-generated method stub
		this.subscription = subscription;
	}
	public void onNext(Object item) {
		// TODO Auto-generated method stub
		if ( subscription == null) {
			SecurityOpRequest.INSTANCE.setConnected(false);
			subscription.cancel();
		}
		SecurityOpRequest.INSTANCE.setConnected(true);
	}

	public void onError(Throwable throwable) {
		// TODO Auto-generated method stub
	}

	public void onComplete() {
		// TODO Auto-generated method stub
	}
}

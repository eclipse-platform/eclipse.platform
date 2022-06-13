package org.eclipse.e4.core.internal.tests.contexts.inject;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(service = TestServiceController.class)
public class TestServiceController {

	ComponentContext context;

	@Activate
	void activate(ComponentContext context) {
		this.context = context;
	}

	public void enableTestServiceA() {
		this.context.enableComponent(TestServiceA.class.getName());
	}

	public void disableTestServiceA() {
		this.context.disableComponent(TestServiceA.class.getName());
	}

	public void enableTestServiceB() {
		this.context.enableComponent(TestServiceB.class.getName());
	}

	public void disableTestServiceB() {
		this.context.disableComponent(TestServiceB.class.getName());
	}
}

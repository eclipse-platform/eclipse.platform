package org.eclipse.e4.core.internal.tests.contexts.inject;

import org.osgi.service.component.annotations.Component;

@Component(enabled = false)
public class TestServiceA implements TestService, TestOtherService {

}

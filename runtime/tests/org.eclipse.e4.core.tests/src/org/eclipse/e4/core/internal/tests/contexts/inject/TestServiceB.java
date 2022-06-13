package org.eclipse.e4.core.internal.tests.contexts.inject;

import org.osgi.service.component.annotations.Component;

@Component(enabled = false, property = "service.ranking:Integer=5")
public class TestServiceB implements TestService, TestOtherService {

}

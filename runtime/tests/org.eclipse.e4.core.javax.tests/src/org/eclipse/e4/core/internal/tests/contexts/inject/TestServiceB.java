package org.eclipse.e4.core.internal.tests.contexts.inject;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;

@Component(enabled = false)
@ServiceRanking(5)
public class TestServiceB implements TestService, TestOtherService {

}

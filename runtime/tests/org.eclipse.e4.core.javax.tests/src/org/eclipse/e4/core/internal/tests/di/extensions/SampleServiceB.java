package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;

@Component
@ServiceRanking(40)
public class SampleServiceB implements TestService {

}

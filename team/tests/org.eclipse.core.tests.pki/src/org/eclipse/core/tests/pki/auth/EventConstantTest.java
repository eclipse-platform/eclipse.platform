package org.eclipse.core.tests.pki.auth;



import java.util.Random;

import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.pki.auth.EventConstant;

import org.junit.Before;
import org.junit.Test;

public class EventConstantTest {
	
	@Mock
	EventConstant eventConstantMock = null;
	public EventConstantTest() {}
	int DONE=0;
	int CANCEL=2;
	int SETUP = Integer.valueOf(10).intValue();
	
	@Before
	public void Initialize() throws Exception {
		//MockitoAnnotations.initMocks(this);	
		eventConstantMock = mock(EventConstant.class);
	}
	@Test
	public void testEventConstantSetup() {
		
		//Random rand = new Random();
        //int ordinal = rand.nextInt(EventConstant.values().length);
        
        int returnValue = EventConstant.SETUP.getValue();
        assertEquals(SETUP, returnValue);
		
        //try ( MockedStatic<EventConstant> eventConstantMock = mockStatic(EventConstant.class)) { 
        //	eventConstantMock.when(() -> { EventConstant.SETUP.getValue(); })
        //	.mock.isOk().thenReturn(ordinal);
        //}
		 
	}
	@Test
	public void testEventConstantDone() {
		Random rand = new Random();
        int ordinal = rand.nextInt(EventConstant.values().length);
		when(eventConstantMock.getValue()).thenReturn(ordinal);
		try {
			int returnValue = EventConstant.DONE.getValue();
			assertEquals(DONE, returnValue);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testEventConstantClose() {
		Random rand = new Random();
        int ordinal = rand.nextInt(EventConstant.values().length);
		when(eventConstantMock.getValue()).thenReturn(ordinal);
		try {
			int returnValue = EventConstant.CANCEL.getValue();
			assertEquals(CANCEL, returnValue);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

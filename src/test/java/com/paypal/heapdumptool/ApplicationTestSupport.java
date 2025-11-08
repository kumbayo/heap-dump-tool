package com.paypal.heapdumptool;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static com.paypal.heapdumptool.fixture.MockitoTool.voidAnswer;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;

public class ApplicationTestSupport {

    public static int runApplication(final String... args) throws Exception {
        final ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(int.class);

        try (final MockedStatic<Application> applicationMock = mockStatic(Application.class)) {

            applicationMock.when(() -> Application.systemExit(anyInt()))
                           .thenAnswer(voidAnswer());

            applicationMock.when(() -> Application.main(args))
                           .thenCallRealMethod();

            applicationMock.when(Application::newCommandLine)
                           .thenCallRealMethod();

            Application.main(args);

            applicationMock.verify(() -> Application.systemExit(captor.capture()));
        }

        return captor.getValue();
    }

    private ApplicationTestSupport() {
        throw new AssertionError();
    }
}

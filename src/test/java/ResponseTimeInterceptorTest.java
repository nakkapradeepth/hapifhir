import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.util.StopWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ResponseTimeInterceptorTest {

    @Test
    public void testGetAvgRespTime() {
        // given
        ResponseTimeInterceptor interceptor = new ResponseTimeInterceptor();
        List<IHttpResponse> responses = new ArrayList<>(20);
        for (int i = 1; i <= 20; i++) {
            IHttpResponse response = mock(IHttpResponse.class);
            StopWatch stopWatch = mock(StopWatch.class);
            when(stopWatch.getMillis()).thenReturn((long) (800 + i * 10));
            when(response.getRequestStopWatch()).thenReturn(stopWatch);
            responses.add(response);
        }

        // when
        for (IHttpResponse response : responses) {
            interceptor.interceptResponse(response);
        }

        // then
        Assertions.assertEquals(905L, interceptor.getAvgRespTime());
    }
}
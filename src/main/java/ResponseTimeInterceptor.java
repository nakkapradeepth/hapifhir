import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

import java.util.ArrayList;
import java.util.List;

public class ResponseTimeInterceptor implements IClientInterceptor {

    private final List<Long> responseTimes = new ArrayList<>();

    public void clear() {
        responseTimes.clear();
    }

    public Long getAvgRespTime() {
        return Math.round(responseTimes.stream().mapToLong(i -> i).average().orElse(0));
    }

    @Override
    public void interceptRequest(IHttpRequest iHttpRequest) {

    }

    @Override
    public void interceptResponse(IHttpResponse iHttpResponse) {
        responseTimes.add(iHttpResponse.getRequestStopWatch().getMillis());
    }
}

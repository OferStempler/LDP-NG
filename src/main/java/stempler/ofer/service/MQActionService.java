package stempler.ofer.service;

import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.LdpResponse;
import stempler.ofer.model.MQldpResponse;

public interface MQActionService {

	public MQldpResponse execute (ExtendedService extendedService, String content, String id);
	public /*void */ LdpResponse sendRequestToMQ(ExtendedService extendedService, String requestContent, String id) throws  Exception;
}
package stempler.ofer.service;

import stempler.ofer.model.JsonRequestAndResponse;
import stempler.ofer.model.LdpResponse;

public interface GetSchemasFromWSDL {

	

	public LdpResponse getSchemasFromWSDL(String url, int serviceId);
	public LdpResponse createServiceRegexes(String url, int serviceId);
	public LdpResponse buildRegexFromJson(JsonRequestAndResponse requestAndResponse);

	
}

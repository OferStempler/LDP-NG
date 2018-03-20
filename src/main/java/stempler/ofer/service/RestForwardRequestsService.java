package stempler.ofer.service;

import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.LdpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RestForwardRequestsService {
	public LdpResponse restForwardRequest(ExtendedService extendedService, String content, String guid, HttpServletRequest servletRequest, HttpServletResponse servletResponse, String httpMethod) throws Exception;
//	public LdpResponse getForwardRequest(ExtendedService extendedService, String urlParameters, String guid, HttpServletRequest servletRequest, HttpServletResponse servletResponse, ContentTypes contentTypeEnum) throws Exception;

}

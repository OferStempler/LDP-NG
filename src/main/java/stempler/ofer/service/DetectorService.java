package stempler.ofer.service;

import org.springframework.stereotype.Service;
import stempler.ofer.model.ExtendedService;

import javax.servlet.http.HttpServletRequest;


@Service
public interface DetectorService {
	
	 boolean perform(ExtendedService extendedService, StringBuffer content, String messageType, String id, HttpServletRequest request);
	 void reload();
}

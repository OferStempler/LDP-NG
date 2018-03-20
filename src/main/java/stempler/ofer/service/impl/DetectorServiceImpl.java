package stempler.ofer.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import stempler.ofer.detectors.Detector;
import stempler.ofer.detectors.DetectorListHandler;
import stempler.ofer.detectors.ServiceLoader;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.service.DetectorService;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

@Service
@Log4j
public class DetectorServiceImpl implements DetectorService {

	@Autowired
	DetectorListHandler detectorListHandler;
	
	@Autowired
	ServiceLoader serviceLoader;

	private boolean isValid = true;

	@PostConstruct
	public void init(){
		log.debug("DetectorServiceImpl.CTOR()");
	}
	@Override
	public boolean perform(ExtendedService extendedService, StringBuffer requestContent, String messageType, String id, HttpServletRequest request){
				
//		log.debug("Performing validations:");
		for (Detector detector : detectorListHandler.getDetectors()) {
			isValid = detector.execute(extendedService, requestContent, messageType, id, request);
			if (!isValid){
				return isValid;
			}
		}
		return isValid;
	}

	@Override
	public void reload() {
		serviceLoader.init();
	}

}

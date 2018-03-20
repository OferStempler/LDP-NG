package stempler.ofer.detectors;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DetectorListHandler {

	private List<Detector> detectors;
	
	public DetectorListHandler() {
		this.detectors = new ArrayList<>();
	}

	public DetectorListHandler(List<Detector> detectors) {
		this.detectors = detectors;
	}

	public List<Detector> getDetectors() {
		return detectors;
	}

	public void setDetectors(List<Detector> detectors) {
		this.detectors = detectors;
	}
	
	
	
}

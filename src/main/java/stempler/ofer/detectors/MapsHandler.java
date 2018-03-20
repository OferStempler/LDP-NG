package stempler.ofer.detectors;

import stempler.ofer.model.ElementRegex;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.SchemaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
//Responsible for building the map of the service uri and ant correlative extended service
public class MapsHandler {

	private Map<String, ExtendedService>    serviceUriMap;
	private Map<String, List<ElementRegex>> serviceElementMap;
	private Map<String, List<SchemaType>>   schemaTypeMap;
	 
	public MapsHandler() {
		this.serviceUriMap     = new HashMap<>();
		this.serviceElementMap = new HashMap<>();
		this.schemaTypeMap     = new HashMap<>();
	}

	public Map<String, ExtendedService> getServiceUriMap() {
		return serviceUriMap;
	}

	public void setServiceUriMap(HashMap<String, ExtendedService> serviceUriMap) {
		this.serviceUriMap = serviceUriMap;
	}

	public Map<String, List<ElementRegex>> getServiceElementMap() {
		return serviceElementMap;
	}

	public void setServiceElementMap( Map<String, List<ElementRegex>> serviceElementMap ) {
		this.serviceElementMap = serviceElementMap;
	}

	public Map<String, List<SchemaType>> getSchemaTypeMap() {
		return schemaTypeMap;
	}

	public void setSchemaTypeMap( Map<String, List<SchemaType>> schemaTypeMap) {
		this.schemaTypeMap = schemaTypeMap;
	}
}

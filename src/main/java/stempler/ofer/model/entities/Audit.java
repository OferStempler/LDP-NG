package stempler.ofer.model.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;


@Document (collection = "audit")
@Data
public class Audit {

	@Id
	private String _id;
	//for request resonse audit
	private String serviceName;
	private String url;
	private String destination;
	private String date;
	private String request;
	private String response;
	private String status;
	private String message;
	private String guid;
	private String exception;
	
	//for general audit like import/export/getWsdl
	
	private String content;
	
}

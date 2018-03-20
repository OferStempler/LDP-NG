package stempler.ofer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportExportResponse {

	
	private String responseMessage;
	private boolean success;
}


package stempler.ofer.model;


import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LdpResponse {
	protected HttpStatus responseCode;
	protected String responseMessage;
	
	
}

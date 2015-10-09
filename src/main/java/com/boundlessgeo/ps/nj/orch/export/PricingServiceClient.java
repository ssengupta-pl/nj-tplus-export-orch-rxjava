/**
 *
 */
package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author ssengupta
 */
@FeignClient(url = "http://localhost:8082")
public interface PricingServiceClient {
	@RequestMapping(method = RequestMethod.POST, value = "/pricing",
			consumes = "application/json", produces = "application/json")
	public GenericResponse price(@RequestBody QueryMetadata queryMetadata);
}

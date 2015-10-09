/**
 *
 */
package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ssengupta
 */
@FeignClient(url = "http://localhost:8082")
public interface AirportDataServiceClient {
	@RequestMapping(method = RequestMethod.GET, value = "/data/arpt")
	public GenericResponse queryForAirportData(
			@RequestParam("query") String query);
}

/**
 *
 */
package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ssengupta
 */
@FeignClient(url = "http://apps.dev.flightopseng.com")
public interface BillingServiceClient {
	@RequestMapping(method = RequestMethod.GET, value = "/FlightOps-0.1/teamdbint/users/standing")
	public String isUserInGoodStanding(@RequestParam("userName") String userName);
}

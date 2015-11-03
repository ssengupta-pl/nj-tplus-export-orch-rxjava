package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(url = "https://auth-token.dev.flightopseng.com")
public interface TokenServiceClient {
	@RequestMapping(method = RequestMethod.GET,
			value = "/FlightOps_Token/token/create/{user}/{password}")
	public String get(@RequestParam("user") String user,
			@RequestParam("password") String password);

	@RequestMapping(method = RequestMethod.GET,
					 value = "/FlightOps_Token/token/isValid/{token}")
	public String isValid(@PathVariable("token") String token);
	
}

package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(url = "http://localhost:8082")
public interface SubscriptionServiceClient {
	@RequestMapping(method = RequestMethod.GET,
			value = "/mss/{level}/{resource}/{operation}/check")
	public GenericResponse get(@RequestParam("level") String level,
			@RequestParam("resource") String resource,
			@RequestParam("operation") String operation);
}

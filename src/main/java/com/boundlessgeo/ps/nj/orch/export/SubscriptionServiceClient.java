package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.flightopseng.dragonfly.tplus.internal.model.pricing.SubscriptionLevelAuthorization;

@FeignClient(url = "http://apps.dev.flightopseng.com")
public interface SubscriptionServiceClient {
	@RequestMapping(method = RequestMethod.GET,
			value = "/FlightOps-0.1/teamdbint/subscriptions/{subscription}/{resource}/{operation}",
			consumes = "application/json", produces = "application/json")
	public SubscriptionLevelAuthorization get(@PathVariable("subscription") String subscription,
			@PathVariable("resource") String resource,
			@PathVariable("operation") String operation);
}

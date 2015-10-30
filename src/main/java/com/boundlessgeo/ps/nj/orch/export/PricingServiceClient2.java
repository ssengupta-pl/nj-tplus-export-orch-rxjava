package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.flightopseng.dragonfly.tplus.internal.model.pricing.RowPrice;

@FeignClient(url = "http://apps.dev.flightopseng.com")
public interface PricingServiceClient2 {
	@RequestMapping(method = RequestMethod.GET, value = "/FlightOps-0.1/teamdbint/pricing/rowByICAO/{icao}",
			consumes = "application/json", produces = "application/json")
	public RowPrice getRowPrice(@PathVariable("icao") String icao);
}

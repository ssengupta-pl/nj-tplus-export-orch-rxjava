package com.boundlessgeo.ps.nj.orch.export;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(url = "http://apps.dev.flightopseng.com")

public interface DafifExportServiceClient {
	//@Headers("x-auth-token:{token}")
	@RequestMapping(method = RequestMethod.GET,
			value = "/FlightOps-0.1/teamdbint/export/dafifExportJob?teamDbAirportId={icao}&distance={distance}")
	public String get(@PathVariable("icao") String icao,
			@PathVariable("distance") Double distance);

	@RequestMapping(method = RequestMethod.GET,
			value = "/FlightOps-0.1/teamdbint/export/resultcount?jobid={jobid}")
	public String getCount(@PathVariable("jobid") String jobid);

	@RequestMapping(method = RequestMethod.GET,
			value = "/FlightOps-0.1/teamdbint/export/jobstatus?jobid={jobid}")
	public String getStatus(@PathVariable("jobid") String jobid);

}

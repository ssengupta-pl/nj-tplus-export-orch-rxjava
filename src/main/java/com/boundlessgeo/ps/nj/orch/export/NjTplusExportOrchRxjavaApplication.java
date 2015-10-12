package com.boundlessgeo.ps.nj.orch.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import rx.Observable;
import rx.Statement;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

@RestController
@RequestMapping(value = "/api/tplus")
@SpringBootApplication
@EnableFeignClients
public class NjTplusExportOrchRxjavaApplication {
	@Autowired
	private SubscriptionServiceClient subscriptionService;

	@Autowired
	private BillingServiceClient billingService;

	@Autowired
	private AirportDataServiceClient airportDataService;

	@Autowired
	private PricingServiceClient pricingService;

	@RequestMapping(value = "/exportJobs", method = RequestMethod.POST,
			consumes = "application/json", produces = "application/json")
	public GenericResponse createExportJob(@RequestBody ExportJob exportJob) {
		// return this.runActivityWithoutRxJava(exportJob);
		// return this.runActivityWithRxJavaSingleThreaded(exportJob);
		return this.runActivityWithRxJavaMultiThreaded(exportJob);
	}

	@SuppressWarnings({ "unchecked" })
	private GenericResponse runActivityWithRxJavaMultiThreaded(
			final ExportJob exportJob) {
		final GenericResponse genericResponse = new GenericResponse();
		genericResponse.setSource("exportJob");
		LinkedHashMap<String, String> messages = (LinkedHashMap<String, String>) genericResponse
				.getInformation().get("messages");
		if (messages == null) {
			messages = new LinkedHashMap<String, String>();
			genericResponse.getInformation().put("messages", messages);
		}

		Observable<GenericResponse> subscriptionCheckStream = Observable
				.just(subscriptionService.get(exportJob.getSubscriptionLevel(),
						exportJob.getRequestedResource(),
						exportJob.getRequestedOperation()))
				.subscribeOn(Schedulers.newThread());

		Observable<GenericResponse> billingStandingCheckStream = Observable
				.just(billingService.isUserInGoodStanding(exportJob.getUser()))
				.subscribeOn(Schedulers.newThread());

		Observable<GenericResponse> checks = Observable
				.merge(subscriptionCheckStream, billingStandingCheckStream)
				.all(new Func1<GenericResponse, Boolean>() {
					@Override
					public Boolean call(GenericResponse t) {
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put(t.getSource(), t.getMessage());

						System.out.println("checks.all -> " + genericResponse);

						return t.getResult()
								.equalsIgnoreCase(GenericResponse.SUCCESS);
					}
				}).map(new Func1<Boolean, GenericResponse>() {
					@Override
					public GenericResponse call(Boolean t) {
						if (t) {
							genericResponse.setResult(GenericResponse.SUCCESS);
							genericResponse.setMessage("Checks passed");
						} else {
							genericResponse.setResult(GenericResponse.ERROR);
							genericResponse.setMessage("Checks did not pass");
						}

						System.out.println("checks.map -> " + genericResponse);

						return new GenericResponse();
					}
				});

		Observable<GenericResponse> pricingStream = Observable
				.just(airportDataService
						.queryForAirportData(exportJob.getQuery()))
				.map(new Func1<GenericResponse, GenericResponse>() {
					@Override
					public GenericResponse call(GenericResponse t) {
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put(t.getSource(), t.getMessage());
						System.out.println(
								"pricingStream.map, 1 -> " + genericResponse);

						return t;
					}
				}).map(new Func1<GenericResponse, QueryMetadata>() {
					@Override
					public QueryMetadata call(GenericResponse t) {
						QueryMetadata queryMetadata = new QueryMetadata();
						queryMetadata.setNumRows(
								(int) t.getInformation().get("numRows"));
						queryMetadata.setFieldTypes(
								(ArrayList<LinkedHashMap<String, String>>) t
										.getInformation().get("fieldTypes"));
						System.out.println(
								"pricingStream.map, 2 -> " + genericResponse);

						return queryMetadata;
					}
				}).first().map(new Func1<QueryMetadata, GenericResponse>() {
					@Override
					public GenericResponse call(QueryMetadata t) {
						return pricingService.price(t);
					}
				}).map(new Func1<GenericResponse, GenericResponse>() {
					@Override
					public GenericResponse call(GenericResponse t) {
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put(t.getSource(), t.getMessage());
						System.out.println(
								"pricingStream.map, 4 -> " + genericResponse);

						return t;
					}
				}).map(new Func1<GenericResponse, GenericResponse>() {
					@Override
					public GenericResponse call(GenericResponse t) {
						genericResponse.setSource("exportJob");
						genericResponse.setResult(t.getResult());
						genericResponse.setMessage(t.getMessage());
						genericResponse.getInformation()
								.putAll(t.getInformation());
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put("exportJob", t.getMessage());
						System.out.println(
								"pricingStream.map, 5 -> " + genericResponse);

						return t;
					}
				});

		checks.subscribe();

		Statement.ifThen(new Func0<Boolean>() {
			@Override
			public Boolean call() {
				System.out.println("ifThen -> " + genericResponse);
				return genericResponse.getResult()
						.equalsIgnoreCase(GenericResponse.SUCCESS);
			}
		}, pricingStream).retry().subscribe();

		return genericResponse;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private GenericResponse runActivityWithRxJavaSingleThreaded(
			final ExportJob exportJob) {
		final GenericResponse genericResponse = new GenericResponse();
		LinkedHashMap<String, String> messages = (LinkedHashMap<String, String>) genericResponse
				.getInformation().get("messages");
		if (messages == null) {
			messages = new LinkedHashMap<String, String>();
			genericResponse.getInformation().put("messages", messages);
		}

		Observable<GenericResponse> subscriptionCheckStream = Observable
				.just(subscriptionService.get(exportJob.getSubscriptionLevel(),
						exportJob.getRequestedResource(),
						exportJob.getRequestedOperation()));

		Observable<GenericResponse> billingStandingCheckStream = Observable
				.just(billingService.isUserInGoodStanding(exportJob.getUser()));

		Observable<GenericResponse> checks = Observable
				.merge(subscriptionCheckStream, billingStandingCheckStream)
				.all(new Func1<GenericResponse, Boolean>() {
					@Override
					public Boolean call(GenericResponse t) {
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put(t.getSource(), t.getMessage());

						System.out.println("checks.all -> " + genericResponse);

						return t.getResult()
								.equalsIgnoreCase(GenericResponse.SUCCESS);
					}
				}).map(new Func1<Boolean, GenericResponse>() {
					@Override
					public GenericResponse call(Boolean t) {
						if (t) {
							genericResponse.setResult(GenericResponse.SUCCESS);
							genericResponse.setMessage("Checks passed");
						} else {
							genericResponse.setResult(GenericResponse.ERROR);
							genericResponse.setMessage("Checks did not pass");
						}

						System.out.println("checks.map -> " + genericResponse);

						return new GenericResponse();
					}
				});

		Observable<GenericResponse> pricingStream = Observable
				.just(airportDataService
						.queryForAirportData(exportJob.getQuery()))
				.map(new Func1<GenericResponse, GenericResponse>() {
					@Override
					public GenericResponse call(GenericResponse t) {
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put(t.getSource(), t.getMessage());
						System.out.println(
								"pricingStream.map, 1 -> " + genericResponse);

						return t;
					}
				}).map(new Func1<GenericResponse, QueryMetadata>() {
					@Override
					public QueryMetadata call(GenericResponse t) {
						QueryMetadata queryMetadata = new QueryMetadata();
						queryMetadata.setNumRows(
								(int) t.getInformation().get("numRows"));
						queryMetadata.setFieldTypes(
								(ArrayList<LinkedHashMap<String, String>>) t
										.getInformation().get("fieldTypes"));
						System.out.println(
								"pricingStream.map, 2 -> " + genericResponse);

						return queryMetadata;
					}
				}).first().map(new Func1<QueryMetadata, GenericResponse>() {
					@Override
					public GenericResponse call(QueryMetadata t) {
						return pricingService.price(t);
					}
				}).map(new Func1<GenericResponse, GenericResponse>() {
					@Override
					public GenericResponse call(GenericResponse t) {
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put(t.getSource(), t.getMessage());
						System.out.println(
								"pricingStream.map, 4 -> " + genericResponse);

						return t;
					}
				}).map(new Func1<GenericResponse, GenericResponse>() {
					@Override
					public GenericResponse call(GenericResponse t) {
						genericResponse.setSource("exportJob");
						genericResponse.setResult(t.getResult());
						genericResponse.setMessage(t.getMessage());
						genericResponse.getInformation()
								.putAll(t.getInformation());
						((LinkedHashMap<String, String>) genericResponse
								.getInformation().get("messages"))
										.put("exportJob", t.getMessage());
						System.out.println(
								"pricingStream.map, 5 -> " + genericResponse);

						return t;
					}
				});

		checks.subscribe();

		Statement.ifThen(new Func0<Boolean>() {
			@Override
			public Boolean call() {
				System.out.println("ifThen -> " + genericResponse);
				return genericResponse.getResult()
						.equalsIgnoreCase(GenericResponse.SUCCESS);
			}
		}, pricingStream).subscribe();

		return genericResponse;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private GenericResponse runActivityWithoutRxJava(ExportJob exportJob) {
		GenericResponse genericResponse = new GenericResponse();

		GenericResponse response = subscriptionService.get(
				exportJob.getSubscriptionLevel(),
				exportJob.getRequestedResource(),
				exportJob.getRequestedOperation());

		if (response != null
				&& response.getResult().equalsIgnoreCase("success")) {
			response = billingService.isUserInGoodStanding(exportJob.getUser());

			if (response != null
					&& !response.getResult().equalsIgnoreCase("error")) {
				response = airportDataService
						.queryForAirportData(exportJob.getQuery());

				if (response != null
						&& !response.getResult().equalsIgnoreCase("error")) {
					QueryMetadata queryMetadata = new QueryMetadata();
					queryMetadata.setNumRows(
							(int) response.getInformation().get("numRows"));
					queryMetadata.setFieldTypes(
							(ArrayList<LinkedHashMap<String, String>>) response
									.getInformation().get("fieldTypes"));

					response = pricingService.price(queryMetadata);

					if (response != null && !response.getResult()
							.equalsIgnoreCase("error")) {
						genericResponse.setResult(GenericResponse.SUCCESS);
						genericResponse
								.setMessage("Export job has been created");
						genericResponse
								.setInformation(response.getInformation());
					} else {
						genericResponse.setResult(GenericResponse.ERROR);
						genericResponse.setMessage(
								"Pricing for the query could not be determined");
					}
				} else {
					genericResponse.setResult(GenericResponse.ERROR);
					genericResponse.setMessage("Invalid query");
				}
			} else {
				genericResponse.setResult(GenericResponse.ERROR);
				genericResponse
						.setMessage("User has unacceptable billing status");
			}
		} else {
			genericResponse.setResult(GenericResponse.ERROR);
			genericResponse.setMessage("User has insufficient privileges");
		}

		return genericResponse;
	}

	public static void main(String[] args) {
		SpringApplication.run(NjTplusExportOrchRxjavaApplication.class, args);
	}
}

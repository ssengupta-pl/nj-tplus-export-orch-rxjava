package com.boundlessgeo.ps.nj.orch.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import feign.FeignException;

@RestController
@RequestMapping(value = "/api/tplus")
@SpringBootApplication
@EnableFeignClients
public class NjTplusExportOrchRxjavaApplication {
	private static final Logger logger = LoggerFactory.getLogger(NjTplusExportOrchRxjavaApplication.class);

	@Autowired
	private SubscriptionServiceClient subscriptionService;

	@Autowired
	private BillingServiceClient billingService;

	@Autowired
	private AirportDataServiceClient airportDataService;

	@Autowired
	private PricingServiceClient pricingService;

	@Autowired
	private TokenServiceClient tokenService;

	@Autowired
	private DafifExportServiceClient dafifService;

	@Autowired
	FeignConfiguration feignconfig;



	@RequestMapping(value = "/exportJobs", method = RequestMethod.POST,
			consumes = "application/json", produces = "application/json")
	public GenericResponse createExportJob(@RequestBody ExportJob exportJob) {
		// return this.runActivityWithoutRxJava(exportJob);
		// return this.runActivityWithRxJavaSingleThreaded(exportJob);
		feignconfig.setToken(exportJob.getToken());
		logger.error("Message logged at ERROR level");
		logger.warn("Message logged at WARN level");
		logger.info("Message logged at INFO level");
		logger.debug("Message logged at DEBUG level");
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
		Observable<String>export=null;

		Observable<String> export2 = Observable.create(new Observable.OnSubscribe() {
			@Override
			public void call(Object subscriber) {
				Subscriber<String> mySubscriber = (Subscriber<String>)subscriber;
				try {
					if (!mySubscriber.isUnsubscribed()) {
						System.out.println("Export2!");
						mySubscriber.onNext(dafifService.get(exportJob.getIcao(), exportJob.getDistance()));
						mySubscriber.onCompleted();
					}
				} catch (Exception e) {
					mySubscriber.onError(e);
				}


			}


		});


		export = Observable.create(new Observable.OnSubscribe() {
			@Override
			public void call(Object subscriber) {
				Subscriber mySubscriber = (Subscriber)subscriber;
				try {
					if (!mySubscriber.isUnsubscribed()) {
						System.out.println("Trying to call export with token: " + feignconfig.getToken());
						mySubscriber.onNext(dafifService.get(exportJob.getIcao(), exportJob.getDistance()));
						mySubscriber.onCompleted();
					}
				} catch (Exception e) {
					mySubscriber.onError(e);
				}


			}


		}).onErrorResumeNext(refreshTokenAndRetry(export2,exportJob.getUser(),exportJob.getPassword()));
		//.subscribeOn(Schedulers.computation());
		export.subscribe();

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

	public Observable<String> refreshToken(final String user, final String password) {
		return Observable.create(new Observable.OnSubscribe() {



			@Override
			public void call(Object t) {
				Subscriber mySubscriber = (Subscriber)t;
				try {
					mySubscriber.onNext(tokenService.get(user, password));
					mySubscriber.onCompleted();
				}catch(Exception e) {
					mySubscriber.onError(e);
				}
			}
		});
	}
	private <T> Func1<Throwable,? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed, final String user, final String password) {
		return new Func1<Throwable, Observable<? extends T>>() {
			@Override
			public Observable<? extends T> call(Throwable throwable) {
				// Here check if the error thrown really is a 403
				if (isHttp401Error(throwable)) {
					return refreshToken(user, password).flatMap(new Func1<String, Observable<? extends T>>() {
						@Override
						public Observable<? extends T> call(String tokenin) {
							feignconfig.setToken(tokenin);
							System.out.println("Token refreshed: " + tokenin);
							System.out.println(toBeResumed.toString());
							return toBeResumed;
						}
					});
				}
				// re-throw this error because it's not recoverable from here
				return Observable.error(throwable);
			}
		};
	}


	protected boolean isHttp401Error(Throwable throwable) {
		boolean out = false;
		if(throwable instanceof FeignException ) {
			FeignException e = (FeignException)throwable;
			if(e.getLocalizedMessage().contains("403"))
				out=true;
		}
		return out;
	}

	public static void main(String[] args) {
		SpringApplication.run(NjTplusExportOrchRxjavaApplication.class, args);
	}
}

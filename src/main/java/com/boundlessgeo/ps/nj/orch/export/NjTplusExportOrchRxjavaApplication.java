package com.boundlessgeo.ps.nj.orch.export;

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





		//Another export observable, required if token needs to be refreshed
		Observable<String> export2 = export2(exportJob);

		Observable<String>export = export(exportJob, genericResponse,export2);


		//Start the export job async.
		export.subscribeOn(Schedulers.newThread()).subscribe((value)->{genericResponse.setJobid(value);});
		//export.subscribe();


		Observable<String>jobstatus = exportjobstatus(exportJob,genericResponse);

		//Start the job status job once jobid is populated
		Statement.ifThen(()->{
			return Long.parseLong(genericResponse.getJobid())>0;
		},
		jobstatus).retry().subscribe();

		Observable<Integer>jobcount = exportjobcount(exportJob,genericResponse);

		Statement.ifThen(()->{
			return genericResponse.getJobstatus().endsWith(".zip");
		},
		jobcount).retry().subscribe();





		/*		Observable<GenericResponse> subscriptionCheckStream = subscription(exportJob);
		subscriptionCheckStream.subscribe();

		Observable<GenericResponse> billingStandingCheckStream = billingStanding(exportJob);
		billingStandingCheckStream.subscribe();

		Observable<GenericResponse> checks = standingSubscriptionMerge(genericResponse, subscriptionCheckStream,
				billingStandingCheckStream);

		Observable<GenericResponse> pricingStream = pricing(exportJob, genericResponse);
		checks.subscribe();

		Statement.ifThen(new Func0<Boolean>() {
			@Override
			public Boolean call() {
				System.out.println("ifThen -> " + genericResponse);
				return genericResponse.getResult()
						.equalsIgnoreCase(GenericResponse.SUCCESS);
			}
		}, pricingStream).retry().subscribe();*/

		return genericResponse;
	}





	/*	private Observable<String> exportjobstatus(final ExportJob exportJob) {
		return Observable
				.just(dafifService.getStatus(exportJob.getJobid()));
	}*/

	private Observable<Integer> exportjobcount(ExportJob exportJob,
			final GenericResponse genericResponse) {
		return Observable.create(subscriber->{

			try {
				if (!subscriber.isUnsubscribed()) {
					Integer count =Integer.parseInt(dafifService.getCount(genericResponse.getJobid()));
					genericResponse.setRecordcount(count);
					subscriber.onNext(count);
					subscriber.onCompleted();
				}
			} catch (Exception e) {
				subscriber.onError(e);
			}
		}



				);
	}

	private Observable<String> exportjobstatus(final ExportJob exportJob, final GenericResponse genericResponse) {
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				try {
					if (!subscriber.isUnsubscribed()) {
						String status =dafifService.getStatus(genericResponse.getJobid());
						genericResponse.setJobstatus(status);
						subscriber.onNext(status);
						subscriber.onCompleted();
					}
				} catch (Exception e) {
					subscriber.onError(e);
				}


			}




		})
		.repeat()
		.takeUntil(new Func1<String, Boolean>() {
			@Override
			public Boolean call(String response) {
				return response.endsWith(".zip");
			}
		});
	}




	private Observable<String> export(final ExportJob exportJob, final GenericResponse genericResponse, Observable<String> export2) {
		return Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String>subscriber) {
				try {
					if (!subscriber.isUnsubscribed()) {
						System.out.println("Trying to call export with token: " + feignconfig.getToken());
						String jobid =dafifService.get(exportJob.getIcao(), exportJob.getDistance());
						genericResponse.setJobid(jobid);
						subscriber.onNext(jobid);
						subscriber.onCompleted();
					}
				} catch (Exception e) {
					subscriber.onError(e);
				}


			}


		}).onErrorResumeNext(refreshTokenAndRetry(export2,exportJob.getUser(),exportJob.getPassword())).subscribeOn(Schedulers.computation());

	}

	private Observable<String> export2(final ExportJob exportJob) {
		Observable<String> export2 = Observable.create(subscriber-> {
			try {
				if (!subscriber.isUnsubscribed()) {
					System.out.println("Export2!");
					subscriber.onNext(dafifService.get(exportJob.getIcao(), exportJob.getDistance()));
					subscriber.onCompleted();
				}
			} catch (Exception e) {
				subscriber.onError(e);
			}





		});
		return export2;
	}

	public Observable<String> refreshToken(final String user, final String password) {
		return Observable.create(subscriber-> {
			try {
				subscriber.onNext(tokenService.get(user, password));
				subscriber.onCompleted();
			}catch(Exception e) {
				subscriber.onError(e);
			}

		});
	}
	private <T> Func1<Throwable,? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed, final String user, final String password) {
		return new Func1<Throwable, Observable<? extends T>>() {
			@Override
			public Observable<? extends T> call(Throwable throwable) {
				// Here check if the error thrown really is a 403
				if (isHttp401Error(throwable)) {
					return refreshToken(user, password).flatMap(tokenin-> {
						feignconfig.setToken(tokenin);
						System.out.println("Token refreshed: " + tokenin);
						System.out.println(toBeResumed.toString());
						return toBeResumed;

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

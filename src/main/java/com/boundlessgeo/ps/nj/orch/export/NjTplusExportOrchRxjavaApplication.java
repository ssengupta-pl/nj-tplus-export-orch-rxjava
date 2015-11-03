package com.boundlessgeo.ps.nj.orch.export;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

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

import com.flightopseng.dragonfly.tplus.internal.model.pricing.SubscriptionLevelAuthorization;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Statement;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.BlockingObservable;
import rx.observables.ConnectableObservable;
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
	private PricingServiceClient2 pricingService2;
	
	@Autowired
	SubscriptionServiceClient ssc;

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
		
		// TODO - General error handling.
		// TODO - Token error handling if the token expires at any point during the process?
		
		// Do an up front synchronous token refresh if necessary. All service calls depend on a valid token.
		if (!tokenCheck(exportJob.getToken())) {
			String token = tokenRefresh(exportJob.getUser(), exportJob.getPassword());
			exportJob.setToken(token);
		}

		// Asynchronously perform all authorization checks.
		Observable<GenericResponse> subscriptionCheckStream = subscriptionAuthorizationCheck(exportJob).subscribeOn(Schedulers.newThread());
		Observable<GenericResponse> goodStandingStream = subscriberIsInGoodStanding(exportJob.getUser()).subscribeOn(Schedulers.newThread());
		
		// Merge the check streams, reduce them into one GenericResponse, and then block for the result.
		Observable<GenericResponse> allChecksStream = Observable.merge(subscriptionCheckStream, goodStandingStream);
		Observable<GenericResponse> allChecksStreamReduced = reduceResponses(allChecksStream);
		GenericResponse allChecksResponse = allChecksStreamReduced.toBlocking().first();
		
		// Use the result of the authorization checks to guard the export job.
		if (allChecksResponse.getResult().equalsIgnoreCase(GenericResponse.SUCCESS)) {
			// Kick off the export job. jobStream won't notify until it has a jobId.
			Observable<ExportJob> startedJobStream = startJob(exportJob);
			
			// Block for the export job with jobId. Everything below is dependent on it.
			ExportJob exportJobWithId = startedJobStream.toBlocking().first(); 
			
			// Use the job with jobId for the pricing and status (includes filename) streams.
			Observable<GenericResponse> pricingStream = totalPrice(Observable.just(exportJobWithId)).subscribeOn(Schedulers.newThread());
			Observable<GenericResponse> jobStatusStream = jobStatusStream(Observable.just(exportJobWithId)).subscribeOn(Schedulers.newThread());;
			
			// Merge all result GenericResponse streams, reduce them into one GenericResponse, and then block to return that GenericResponse.
			Observable<GenericResponse> allResponsesStream = Observable.merge(Observable.just(allChecksResponse), pricingStream, jobStatusStream); 
			Observable<GenericResponse> reducedResponseStream = reduceResponses(allResponsesStream);
			GenericResponse reducedResult = reducedResponseStream.toBlocking().first();
			return reducedResult;
		} else {
			// Return the GenericResponse containing only the responses from the authorization checks. (It could contain an error message).
			return allChecksResponse;
		}

	}
	
	/**
	 * Returns the response from the tokenService.isValid call for the provided token.
	 */
	private boolean tokenCheck(String token) {
		return tokenService.isValid(token).equalsIgnoreCase("true");
	}

	private String tokenRefresh(String user, String password) {
		String token = tokenService.get(user, password);
		feignconfig.setToken(token);
		return token;
	}
	
	/**
	 * Takes a stream of GenericResponses and returns an observable for a reduced (merged) response.
	 * 
	 * The merged response has its result set to SUCCESS only if all the responses in the stream are also SUCCESS.
	 */
	private Observable<GenericResponse> reduceResponses(Observable<GenericResponse> responses) {
		
		GenericResponse initAccumulator = new GenericResponse();
		initAccumulator.setResult(GenericResponse.SUCCESS);
		Observable<GenericResponse> accumulatedResponse = responses.reduce(initAccumulator, (accumulator, nextResponse)-> {
				if ((accumulator.getResult().equalsIgnoreCase(GenericResponse.SUCCESS)) 
				   && nextResponse.getResult().equalsIgnoreCase(GenericResponse.SUCCESS)) {
					accumulator.setResult(GenericResponse.SUCCESS);
				} else {
					accumulator.setResult(GenericResponse.ERROR);
				}

				if (nextResponse.getSource() != null) {
					accumulator.getInformation().put(nextResponse.getSource(), nextResponse.getMessage());
				}
				
				accumulator.getInformation().putAll(nextResponse.getInformation());
				
				if(nextResponse.getJobid() != null) {
					accumulator.setJobid(nextResponse.getJobid());
				}
				
				if(nextResponse.getJobstatus() != null) {
					accumulator.setJobstatus(nextResponse.getJobstatus());
				}
				
				if(nextResponse.getRecordcount() != null) {
					accumulator.setRecordcount(nextResponse.getRecordcount());
				}
				
				if(nextResponse.getTotalprice() != null) {
					accumulator.setTotalprice(nextResponse.getTotalprice());
				}
				
				return accumulator;
		});
		
		return accumulatedResponse; 
	}
	
	/**
	 * Takes an exportJob, starts the job, and returns an Observable for the exportJob populated with a jobId.
	 */
	private Observable<ExportJob> startJob(ExportJob exportJob) {
		return Observable.just(exportJob).map(eJob -> {
			System.out.println("\t\t******** Firing dafifService.get()");
			String jobid = dafifService.get(exportJob.getIcao(), exportJob.getDistance()); 
			exportJob.setJobid(jobid);
			return exportJob;
		});
	}
	
		
	/**
	 * Takes an observable for an exportJob.
	 * Returns an observable for the job status of the observable, once it is *.zip
	 */
	private Observable<GenericResponse> jobStatusStream(Observable<ExportJob> jobStream) {
	
		// Query the dafif service repeatedly for the job status, but don't take the result until it ends in .zip,
		//       and map the jobStatus into a GenericResponse.
		Observable<GenericResponse> jobResponseStream = jobStream.subscribeOn(Schedulers.newThread())
			.single()
			.map(exportJob -> {
				String status = dafifService.getStatus(exportJob.getJobid());
				System.out.println("\t\t******** STATUS: " + status);
				return status;
			})
			.repeat()
			.skipWhile(jobStatus -> {return !jobStatus.endsWith(".zip");}) 
			.first()
			.map(jobStatus -> {
				// Map the .zip job status to a Generic Response
				GenericResponse r = new GenericResponse();
				r.setResult(GenericResponse.SUCCESS);
				r.setJobstatus(jobStatus);
				r.setSource("Job Status Check");
				r.setMessage("Job Status Returned: " + jobStatus);
				return r;
			});
			
		// Note: The above does not send more than one outstanding request at a time. 
		// (skipWhile does not ask for the next until it evaluates prev).
		
		return jobResponseStream;
	}

	/**
	 * Takes an observable for a exportJob.
	 * Returns an observable for a GenericResponse with the total price populated.
	 */
	private Observable<GenericResponse> totalPrice(Observable<ExportJob> jobStream) {
		
		Observable<Double> rowPriceStream = jobStream.map(exportJob -> {
			double rowPrice = pricingService2.getRowPrice(exportJob.getIcao()).getRowprice();
			System.out.println("\t\t******** Row Price = " + rowPrice);
			return rowPrice;
		});
		Observable<Integer> rowCountStream = jobStream.map(exportJob -> {
			int rowCount = Integer.parseInt(dafifService.getCount(exportJob.getJobid()));
			System.out.println("\t\t******** Row Count = " + rowCount);
			return rowCount;
		}).retry();
		
		return Observable.zip(rowPriceStream, rowCountStream,
				  (rowprice,rowcount) -> {
					GenericResponse r = new GenericResponse();
					Double price =  rowprice*rowcount;
					r.setSource("Row Count Service x Row Price Service");
					r.setMessage("Row Price: " + rowprice + " Row Count: " + rowcount);
					r.setResult(GenericResponse.SUCCESS);
					r.setTotalprice(price);
					r.setRecordcount(rowcount);
					System.out.println("\t\t******** Total Price = " + price);
					return r;
				 });
	}

	/**
	 * Takes an export job and returns an observable for a GenericResponse for the subscription level check.
	 * If the exportJob's subscription level has permission for the requested resource and operation,
	 * the GenericResponse's result will be SUCCESS, otherwise ERROR. 
	 * 
	 */
	private Observable<GenericResponse> subscriptionAuthorizationCheck(ExportJob exportJob) {
		return Observable.create(subscriber -> {
			try {
				GenericResponse response = new GenericResponse();
				response.setSource("Subscription Service");
				SubscriptionLevelAuthorization subCheckResult = ssc.get(exportJob.getSubscriptionLevel(), 
						exportJob.getRequestedResource(), exportJob.getRequestedOperation());
				if (subCheckResult != null && subCheckResult.isAuthorized()) {
					response.setResult(GenericResponse.SUCCESS);
					response.setMessage("Subscription level " + exportJob.getSubscriptionLevel() 
										+ " may " + exportJob.getRequestedOperation() 
										+ " " + exportJob.getRequestedResource());
				} else {
					response.setResult(GenericResponse.ERROR);
					response.setMessage("Subscription level " + exportJob.getSubscriptionLevel() 
										+ " may NOT " + exportJob.getRequestedOperation() 
										+ " " + exportJob.getRequestedResource());
				}
				
				if(!subscriber.isUnsubscribed()) {
					subscriber.onNext(response);
					subscriber.onCompleted();
				} 
			} catch (Exception e) {
				subscriber.onError(e);
			}		
		});	
	}
	
	/**
	 * Takes an export job and returns an observable for a GenericResponse for the billing service good standing check.
	 * If the exportJob's user is in good standing, the GenericResponse's result will be SUCCESS, otherwise ERROR. 
	 * 
	 */
	private Observable<GenericResponse> subscriberIsInGoodStanding(String userName) {
		return Observable.create(subscriber -> {
			try {
				GenericResponse response = new GenericResponse();
				response.setSource("Billing Service");
				String goodStanding = billingService.isUserInGoodStanding(userName);
				if (goodStanding != null && goodStanding.equals("TRUE")) {
					response.setResult(GenericResponse.SUCCESS);
					response.setMessage("User " + userName + " is in good standing.");
				} else {
					response.setResult(GenericResponse.ERROR);
					response.setMessage("User " + userName + " is not in good standing.");
				}
				
				if(!subscriber.isUnsubscribed()) {
					subscriber.onNext(response);
					subscriber.onCompleted();
				} 
			} catch (Exception e) {
				subscriber.onError(e);
			}		
		});
	}

	private Observable<String> export(final ExportJob exportJob, final GenericResponse genericResponse, Observable<String> export2) {
		return Observable.create((Subscriber<? super String>subscriber)-> {
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
		return (throwable)-> {

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

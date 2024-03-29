/**
 * Copyright Notice
 * 
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.util;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * {@link WorkExecutors}
 * 
 * Generally available thread pools for doing background processing in an ISAAC application.
 * 
 * The {@link #getForkJoinPoolExecutor()} that this provides is identical to the @{link {@link ForkJoinPool#commonPool()}
 * with the exception that it will bottom out at 3 processing threads, rather than 1, to help prevent
 * deadlock situations in common ISAAC usage patterns.  This has an unbounded queue depth, and LIFO behavior.
 * 
 * The {@link #getPotentiallyBlockingExecutor()} that this provides is a standard thread pool with (up to) the same number of threads
 * as there are cores present on the computer - with a minimum of 2 threads.  This executor has no queue - internally
 * it uses a {@link SynchronousQueue} - so if no thread is available to accept the task being queued, it will block 
 * submission of the task until a thread is available to accept the job.
 * 
 * The {@link #getExecutor()} that this provides is a standard thread pool with (up to) the same number of threads
 * as there are cores present on the computer - with a minimum of 2 threads.  This executor has an unbounded queue 
 * depth, and FIFO behavior.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Service
@RunLevel(value = -1)
//KEC: I converted these to use runlevels, but at a runlevel prior to the rest of the 
//ISAAC services. These workers where not shutting down when Isaac was shutdown, 
//now they cleanly shutdown and restart. 
public class WorkExecutors
{
	private ForkJoinPool forkJoinExecutor_;
	private ThreadPoolExecutor blockingThreadPoolExecutor_;
	private ThreadPoolExecutor threadPoolExecutor_;
	private final Logger log = LogManager.getLogger();

	private WorkExecutors()
	{
		//For HK2 only
	}

	@PostConstruct
	private void startMe()
	{
		log.info("Starting the WorkExecutors thread pools");
		//The java default ForkJoinPool.commmonPool starts with only 1 thread, on 1 and 2 core systems, which can get us deadlocked pretty easily.
		int procCount = Runtime.getRuntime().availableProcessors();
		int parallelism = ((procCount - 1) < 3 ? 3 : procCount - 1);  //set between 3 and 1 less than proc count (not less than 3)
		forkJoinExecutor_ = new ForkJoinPool(parallelism);

		int corePoolSize = 2;
		int maximumPoolSize = parallelism;
		int keepAliveTime = 60;
		TimeUnit timeUnit = TimeUnit.SECONDS;
		
		//The blocking executor
		blockingThreadPoolExecutor_ = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, new SynchronousQueue<Runnable>(),
				new NamedThreadFactory("ISAAC-B-work-thread", true));
		blockingThreadPoolExecutor_.setRejectedExecutionHandler((runnable, executor) -> 
		{
			try
			{
				executor.getQueue().offer(runnable, Long.MAX_VALUE, TimeUnit.HOURS);
			}
			catch (Exception e)
			{
				throw new RejectedExecutionException("Interrupted while waiting to enqueue");
			}
		});
		
		//The non-blocking executor - set core threads equal to max - otherwise, it will never increase the thread count
		//with an unbounded queue.
		threadPoolExecutor_ = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize, keepAliveTime, timeUnit, new LinkedBlockingQueue<Runnable>(),
				new NamedThreadFactory("ISAAC-Q-work-thread", true));
		threadPoolExecutor_.allowCoreThreadTimeOut(true);
		//Execute this once, early on, in a background thread - as randomUUID uses secure random - and the initial 
		//init of secure random can block on many systems that don't have enough entropy occuring.  The DB load process
		//should provide enough entropy to get it initialized, so it doesn't pause things later when someone requests a random UUID. 
		getExecutor().execute(() -> UUID.randomUUID());
		log.debug("WorkExecutors thread pools ready");
	}

	@PreDestroy
	private void stopMe()
	{
		log.info("Stopping WorkExecutors thread pools");
		if (forkJoinExecutor_ != null)
		{
			forkJoinExecutor_.shutdownNow();
			forkJoinExecutor_ = null;
		}

		if (blockingThreadPoolExecutor_ != null)
		{
			blockingThreadPoolExecutor_.shutdownNow();
			blockingThreadPoolExecutor_ = null;
		}
		
		if (threadPoolExecutor_ != null)
		{
			threadPoolExecutor_.shutdownNow();
			threadPoolExecutor_  = null;
		}
		log.debug("Stopped WorkExecutors thread pools");
	}
	
	/**
	 * @return the ISAAC common {@link ForkJoinPool} instance - (behavior described in the class docs)
	 * This is backed by an unbounded queue - it won't block / reject submissions because of being full.
	 */
	public ForkJoinPool getForkJoinPoolExecutor()
	{
		return forkJoinExecutor_;
	}
	
	/**
	 * @return The ISAAC common {@link ThreadPoolExecutor} - (behavior described in the class docs).
	 * This is a synchronous queue - if no thread is available to take a job, it will block until a thread 
	 * is available to accept the job.
	 */
	public ThreadPoolExecutor getPotentiallyBlockingExecutor()
	{
		return blockingThreadPoolExecutor_;
	}
	
	/**
	 * @return The ISAAC common {@link ThreadPoolExecutor} - (behavior described in the class docs).
	 * This is backed by an unbounded queue - it won't block / reject submissions because of being full.
	 */
	public ThreadPoolExecutor getExecutor()
	{
		return threadPoolExecutor_;
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		WorkExecutors we = new WorkExecutors();
		we.startMe();
		AtomicInteger counter = new AtomicInteger();
		
		for (int i = 0; i < 24; i++)
		{
			System.out.println("submit " + i);
			we.getPotentiallyBlockingExecutor().submit(new Runnable()
			{
				
				@Override
				public void run()
				{
					int id = counter.getAndIncrement();
					System.out.println(id + " started");
					try
					{
						Thread.sleep(5000);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println(id + " finished");
				}
			});
		}
		
		Thread.sleep(7000);
		System.out.println("Blocking test over");
		
		for (int i = 24; i < 48; i++)
		{
			System.out.println("submit " + i);
			we.getExecutor().submit(new Runnable()
			{
				@Override
				public void run()
				{
					int id = counter.getAndIncrement();
					System.out.println(id + " started");
					try
					{
						Thread.sleep(5000);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println(id + " finished");
				}
			});
		}
		
		while (we.getExecutor().getQueue().size() > 0)
		{
			Thread.sleep(1000);
		}
		
		Thread.sleep(7000);
	}
}

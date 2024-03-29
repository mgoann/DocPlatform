package com.logicaldoc.bm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.logicaldoc.util.config.ContextProperties;

/**
 * Executes a number of different load threads against one or more LogicalDOC
 * instances.
 * 
 * @author Marco Meschieri - Logical Objects
 * @author Alessandro Gasparini - Logical Objects
 * @since 6.5
 */
public class MultiLoader {

	private static Log log = LogFactory.getLog(MultiLoader.class);

	private final static long DEFAULTWORKSPACE = 4L;

	protected String folderProfile;

	protected long rootFolder = DEFAULTWORKSPACE;

	public void setRootFolder(long rootFolder) {
		this.rootFolder = rootFolder;
	}

	private static final String COLUMNS_SUMMARY = String.format("%40s\t%15s\t%15s\t%15s\t%15s\t%15s", "NAME", "COUNT",
			"TOTAL TIME", "AVERAGE TIME", "PER SECOND", "DESCRIPTION");

	protected long startTime;

	private LoadSession session;

	private AbstractLoader[] loaders;

	private Thread consoleThread;

	private boolean finished = false;

	private ContextProperties config;

	public static void main(String[] args) {
		try {
			ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "/context.xml" });

			MultiLoader app = (MultiLoader) context.getBean("MultiLoader");

			// Run
			app.start();

			// Now lower this threads priority
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

			while (!app.finished) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {

				}
				if (app.allThreadsFinished()) {
					System.out.println("The test is complete.");
					app.finished = true;
					break;
				}
			}

			// Finish off
			app.stopAll();
			app.dumpThreadSummaries();

		} catch (Throwable e) {
			System.err.println("A failure prevented proper execution.");
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	private boolean allThreadsFinished() {
		for (AbstractLoader alt : loaders) {
			if (!alt.isFinished())
				return false;
		}
		return true;
	}

	public synchronized void start() {

		if (session == null || loaders == null) {
			throw new RuntimeException("Application not initialized");
		}

		consoleThread.start();

		// Fire up the threads
		for (Thread thread : loaders) {
			thread.start();
		}
	}

	private void initConsole() {

		consoleThread = new Thread() {

			@Override
			public void run() {
				/*
				 * Waiting the end of the job
				 */
				System.out.println("   Enter 'q' to quit.");
				System.out.println("   Enter 's' to dump a thread summary.");

				while (!finished) {
					int keyPress = 0;
					try {
						keyPress = System.in.read();
					} catch (IOException e) {
						break;
					}

					if (keyPress == 'Q' || keyPress == 'q') {
						log.info("Requested stop");
						stopThreads();
						finished = true;
						break;
					} else if (keyPress == 'S' || keyPress == 's') {
						dumpThreadSummaries();
					}
				}
			}
		};

		consoleThread.setPriority(Thread.MIN_PRIORITY);
	}

	protected synchronized void stopThreads() {
		// Stop the threads
		for (AbstractLoader thread : loaders) {
			thread.setStop();
		}
		// Now join each thread to make sure they all finish their current
		// operation
		for (AbstractLoader thread : loaders) {
			// Notify any waits
			synchronized (thread) {
				thread.notifyAll();
			}
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	public void dumpThreadSummaries() {
		System.out.println("");
		System.out.println(COLUMNS_SUMMARY);
		// Dump each thread's summary
		for (AbstractLoader thread : loaders) {
			String summary = thread.getSummary();
			System.out.println(summary);
		}
	}

	public synchronized void stopAll() {

		consoleThread.interrupt();

		// Print and Log each thread's summary
		for (AbstractLoader thread : loaders) {
			String summary = thread.getSummary();
			session.logSummary(summary);
		}
		session.close();
	}

	public void setFolderProfile(String folderProfile) {
		this.folderProfile = folderProfile;
	}

	/**
	 * Initializes resources and connects to the WebService
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception {
		loaders = makeThreads(session);

		// Log the initial summaries
		String summary = session.getSummary();
		session.logSummary(summary);

		// Header the outputs
		session.logSummary(LoadSession.getLineEnding());
		session.logSummary(COLUMNS_SUMMARY);

		// Initialize control console
		initConsole();
	}

	private AbstractLoader[] makeThreads(LoadSession session) throws Exception {

		ArrayList<AbstractLoader> allLoaders = new ArrayList<AbstractLoader>(3);
		StringTokenizer tokenizer = new StringTokenizer(config.getProperty("loaders"), ",", false);
		ArrayList<String> loaders = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			String k = tokenizer.nextToken().trim();
			loaders.add(k);
		}

		for (String loader : loaders) {
			for (int i = 0; i < config.getInt(loader + ".threads"); i++) {
				Class clazz = Class.forName("com.logicaldoc.bm.loaders." + loader);
				// Try to instantiate the parser
				Object o = clazz.newInstance();
				if (!(o instanceof AbstractLoader))
					throw new Exception("The specified loader " + loader + " is not a loader");

				AbstractLoader ld = (AbstractLoader) o;
				ld.setIterations(Long.parseLong(config.getProperty(loader + ".iterations")));

				ld.init(session);
				allLoaders.add(ld);
			}
		}

		// Done
		AbstractLoader[] ret = new AbstractLoader[allLoaders.size()];
		return allLoaders.toArray(ret);
	}

	public void setSession(LoadSession session) {
		this.session = session;
	}

	public void setConfig(ContextProperties config) {
		this.config = config;
	}
}
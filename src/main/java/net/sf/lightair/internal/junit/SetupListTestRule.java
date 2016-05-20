package net.sf.lightair.internal.junit;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.runners.model.FrameworkMethod;

import net.sf.lightair.annotation.Setup;

/**
 * JUnit test rule to setup database with multiple <code>@Setup</code> annotations before test method execution.
 */
public class SetupListTestRule extends AbstractTestRule<Setup.List> {

	private SetupExecutor setupExecutor;

	/**
	 * Constructor.
	 *
	 * @param frameworkMethod
	 *            JUnit framework method on which the test rule is being applied
	 */
	public SetupListTestRule(FrameworkMethod frameworkMethod) {
		super(frameworkMethod, Setup.List.class, Setup.class);
	}

	// beans and their setters:

	/**
	 * If the method is annotated with @{@link Setup}, set up the database.
	 */
	@Override
	protected void before() {
		if (null != getAnnotation()) {
			Setup[] setups = getAnnotation().value();
			Map<String, List<Setup>> setupMap = new HashMap<String, List<Setup>>();
			for (Setup set : setups) {
				String profile = set.profile();
				if (setupMap.containsKey(profile)) {
					setupMap.get(profile).add(set);
				} else {
					ArrayList<Setup> listForProfile = new ArrayList<Setup>();
					listForProfile.add(set);
					setupMap.put(profile, listForProfile);
				}
			}
			ArrayList<Callable<Void>> callables = new ArrayList<Callable<Void>>();
			for (final Map.Entry<String, List<Setup>> entry : setupMap.entrySet()) {
				callables.add(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						for (Setup set : entry.getValue()) {
							setupExecutor.execute(set, getTestMethod());
						}
						return null;
					}
				});
			}
			ExecutorService execSvc = Executors.newCachedThreadPool();
			try {
				execSvc.invokeAll(callables);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Set setup executor.
	 * 
	 * @param setupExecutor
	 */
	public void setSetupExecutor(SetupExecutor setupExecutor) {
		this.setupExecutor = setupExecutor;
	}

}

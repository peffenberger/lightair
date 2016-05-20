package net.sf.lightair.internal.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.runners.model.FrameworkMethod;

import net.sf.lightair.annotation.Verify;

/**
 * JUnit test rule to verify database with multiple <code>@Verify</code> annotations after test method execution.
 */
public class VerifyListTestRule extends AbstractTestRule<Verify.List> {

	private VerifyExecutor verifyExecutor;

	/**
	 * Constructor.
	 *
	 * @param frameworkMethod
	 *            JUnit framework method on which the test rule is being applied
	 */
	public VerifyListTestRule(FrameworkMethod frameworkMethod) {
		super(frameworkMethod, Verify.List.class, Verify.class);
	}

	// beans and their setters:

	/**
	 * If the method is annotated with @{@link Verify}, set up the database.
	 */
	@Override
	protected void after() {
		if (null != getAnnotation()) {
			Verify[] verifies = getAnnotation().value();
			Map<String, List<Verify>> setupMap = new HashMap<String, List<Verify>>();
			for (Verify set : verifies) {
				String profile = set.profile();
				if (setupMap.containsKey(profile)) {
					setupMap.get(profile).add(set);
				} else {
					ArrayList<Verify> listForProfile = new ArrayList<Verify>();
					listForProfile.add(set);
					setupMap.put(profile, listForProfile);
				}
			}
			ArrayList<Callable<Void>> callables = new ArrayList<Callable<Void>>();
			for (final Map.Entry<String, List<Verify>> entry : setupMap.entrySet()) {
				callables.add(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						for (Verify set : entry.getValue()) {
							verifyExecutor.execute(set, getTestMethod());
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
	 * Set verify executor.
	 * 
	 * @param verifyExecutor
	 */
	public void setVerifyExecutor(VerifyExecutor verifyExecutor) {
		this.verifyExecutor = verifyExecutor;
	}

}

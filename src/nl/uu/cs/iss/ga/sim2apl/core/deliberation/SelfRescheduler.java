package nl.uu.cs.iss.ga.sim2apl.core.deliberation;
/**
 * Exposes to the agent the ability to reschedule its deliberation runnable. This 
 * is used to get out of sleep mode.
 * 
 * @author Bas Testerink
 */
public final class SelfRescheduler<T> {
	/** The deliberation runnable that can be rescheduled. */
	private final DeliberationRunnable<T> deliberationRunnable;
	
	public SelfRescheduler(final DeliberationRunnable<T> deliberationRunnable){
		this.deliberationRunnable = deliberationRunnable; 
	}
	
	/** Reschedule this deliberation runnable so it will be executed again in the future. */
	public final void wakeUp(){
		this.deliberationRunnable.reschedule();
	}
}

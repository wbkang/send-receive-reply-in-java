package srr;

import java.util.LinkedList;
import java.util.Queue;

final public class Task {
	private boolean isReceiveBlocked = false;
	private Object slot;
	private Queue<Task> receiveBlockedQueue = new LinkedList<Task>();
	private String name;
	private Runnable runnable;
	private static ThreadLocal<Task> curLocalTask = new ThreadLocal<Task>();
	
	public Task(String name, Runnable runnable) {
		this.name = name;
		this.runnable = runnable;
	}
	
	public void start() {
		Thread t = new Thread("Thread for " + this.name) {
			@Override
			public void run() {
				curLocalTask.set(Task.this);
				
				try {
					Task.this.runnable.run();
				} finally {
					curLocalTask.set(null);
				}
			}
		};
		t.start();
	}
	
	public String toString() {
		return "Task [" + this.name + "]";
	}
	
	@SuppressWarnings("unchecked")
	public static <T, V> V send(Task task, T msg) {
		Task curTask = curLocalTask.get();
		synchronized (task) {
			task.receiveBlockedQueue.add(curTask);
			curTask.isReceiveBlocked = true;
			curTask.slot = msg;
			task.notifyAll();
			while (curTask.isReceiveBlocked) {
				try { task.wait(); } catch(InterruptedException e) { e.printStackTrace(); }
			}
		}
		
		return (V) curTask.slot;
	}
	
	interface Message<T> {
		Task getSender();
		T getContent();
	}
	
	public static <T> Message<T> receive() {
		Task curTask = curLocalTask.get();
		synchronized (curTask) {
			Task top;
			
			while (null == (top = curTask.receiveBlockedQueue.poll())) {
				try { curTask.wait(); } catch(InterruptedException e) { e.printStackTrace(); };
			}
			
			final Task sender = top;

			return new Message<T>() {
				@SuppressWarnings("unchecked")
				@Override
				public T getContent() {
					return (T) sender.slot;
				}
				public Task getSender() {
					return sender;
				};
			};
		}
	}
	
	public static <T> void reply(Task task, T message) {
		Task curTask = curLocalTask.get();
		
		synchronized (curTask) {
			if (!task.isReceiveBlocked) throw new IllegalStateException("Target task not receive blocked.");
			
			task.slot = message;
			task.isReceiveBlocked = false;
			curTask.notifyAll();
		}
	}
}

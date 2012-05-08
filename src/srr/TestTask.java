package srr;

public class TestTask {

	public static void main(String[] args) {
		final Task numberProducer = new Task("NumberProducer", new Runnable() {
			@Override
			public void run() {
				int i = 0;

				while (true) {
					Task.reply(Task.receive().getSender(), i++);
				}
			}
		});
		numberProducer.start();
		
		final Runnable consumer = new Runnable() {
			public void run() {
				for (int i = 0; i < 1000; i++) {
					Task.send(numberProducer, null);
//					System.out.println(number);
				}
				
				// join
				Task.reply(Task.receive().getSender(), null);
			};
		};
		
		final int consumers = 1000;
		Runnable stats = new Runnable() {
			public void run() {
				long start = System.currentTimeMillis();
				Task[] tasks = new Task[consumers];
				
				for (int i = 0; i < tasks.length; i++) {
					tasks[i] = new Task("NumberConsumer " + i, consumer);
				}
				
				for (Task t : tasks) {
					t.start();
				}
				for (Task t : tasks) {
					Task.send(t, null); /// join
				}
				System.out.println(System.currentTimeMillis() - start);
				System.exit(0);
			};
		};
		
		new Task("Stats counter", stats).start();
	}
}

	
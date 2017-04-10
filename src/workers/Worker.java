package workers;

import java.util.concurrent.BlockingQueue;

public class Worker extends Thread {
	
	BlockingQueue<Runnable> queue;
	
	public Worker(BlockingQueue<Runnable> queue) {
		this.queue = queue;
	}
	
	public void run() {
		
		while(true) {
			
			try {
				this.queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

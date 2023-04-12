import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
class Data{
	public int minute;
	public int data;
	public Data(int minute, int data) {
		this.minute = minute;
		this.data = data;
	}
}
class Node {
	Data item;
	int key;
	Node next;
	Node prev;
	private Lock locks = new ReentrantLock();
	public Node(int key) {
		item = new Data(0, key);
		this.key = key;
		next = null;
		prev = null;
	}
	public Node(int key, int time) {
		item = new Data(time, key);
		this.key = key;
		next = null;
		prev = null;
	}
	public void lock() {
		locks.lock();
	}
	public void unlock() {
		locks.unlock();
	}
}
class ConLinkedList{
	private Node head;
	private Node tail;
	boolean uniqueValues = true;
	public ConLinkedList(){
		head = new Node(Integer.MIN_VALUE);
		head.next = new Node(Integer.MAX_VALUE);
		head.prev = null;
		
		tail = head.next;
		tail.prev = head;
	}
	public String getUpToIndexHead(int index) {
		String ret = "\n";
		Node curr = head;
		int counter = 0;
		while(curr.next != null && counter < index) {
			curr = curr.next;
			ret += curr.key + "F (Minute "+curr.item.minute+")\n";
			counter++;
		}
		return ret;
	}
	public String getUpToIndexTail(int index) {
		String ret = "\n";
		Node curr = tail;
		int counter = 0;
		while(curr.prev != null && counter < index) {
			curr = curr.prev;
			ret += curr.key + "F (Minute "+curr.item.minute+")\n";
			counter++;
		}
		return ret;
	}
	public boolean add(int item, int minute) {
		int key = item;
		head.lock();
		Node pred = head;
		try {
			//inital lock
			Node curr = pred.next;
			curr.lock();
			try {
				//seeking phase
				while(curr.key < key) {
					pred.unlock();
					pred = curr;
					curr = curr.next;
					curr.lock();
				}
				//if already in list (uncomment to get top 5 unique)
				if(curr.key == key && uniqueValues) {
					return false;
				}
				Node newNode = new Node(item, minute);
				newNode.next = curr;
				curr.prev = newNode;
				newNode.prev = pred;
				pred.next = newNode;
				return true;
			}finally {
				curr.unlock();
			}
		}
		finally {
			pred.unlock();
		}
	}
	public boolean remove(int item) {
		Node pred = null, curr = null;
		head.lock();
		try {
			pred = head;
			curr = pred.next;
			curr.lock();
			try {
				while(curr.key < item) {
					pred.unlock();
					pred = curr;
					curr = curr.next;
					curr.lock();
				}
				//the magic
				if(curr.key == item) {
					pred.next = curr.next;
					curr.prev = pred;
					return true;
				}
				return false;
			} finally {
				curr.unlock();
			}
		}finally {
			pred.unlock();
		}
	}
}

class TempMod implements Runnable{
	public int minute;
	public int ID;
	private int currentReading;
	public static int[] Memory;
	public static int N;
	private static int hours;
	public static AtomicInteger diff = new AtomicInteger(0);
	public static AtomicInteger timeDiff = new AtomicInteger(0);
	public static int IDContr = 0;
	private static AtomicInteger counter = new AtomicInteger(0);
	private static long timeThreasholdms = 0;
	private static long Started;
	private int startingIndex;
	private int endingIndex;
	public static String output;
	private static ConLinkedList ll;
	
	public TempMod(int ID) {
		minute = 0;
		this.ID = ID;
	}
	public static void InitTime(long time, int threads, int hour) {
		timeThreasholdms = time*1000000;
		Started = System.nanoTime();
		N = threads;
		Memory = new int[N * 60];
		hours = hour;
		output = "";
		ll = new ConLinkedList();
	}
	private void GetOperableRange() {
		startingIndex = ID * 60;
		endingIndex  = (ID + 1) * 60 - 1;
	}
	//compare and set with inequallities
	private boolean greaterThanCompareAndSet(int newValue, AtomicInteger oldval) {
		return oldval.updateAndGet(x -> x < newValue ? newValue : x) == newValue;
	}
	private void Calculate() {
		for(int i  = startingIndex; i <= endingIndex; i++)
		{
			ll.add(Memory[i], (i % 60) + 1);
			if(i + 10 <= endingIndex) {
				synchronized(this) {
					if(greaterThanCompareAndSet(Math.abs(Memory[i] - Memory[i + 10]), diff))
					{
						timeDiff.set((i % 60) + 1);
						IDContr = ID;
					}
				}
			}		
		}
	}
	public static void OutputReport(int count) {
		String bottom5 = ll.getUpToIndexHead(5);
		String top5 = ll.getUpToIndexTail(5);
		String out = "-----------------------HOUR " + count + " METRICS-----------------------\n" + "LOWEST: " + bottom5 + 
				"\nHIGHEST: " + top5 + "\nDIFFERENCE: " + diff + " (between " + timeDiff + " and " + (timeDiff.get() + 10) + " ID "+ IDContr +")\n";
		System.out.print(out);
		output += out;
	}
	@Override
	public void run() {
		int hourCount = 0;
        //get starting and ending index going to scan through
        GetOperableRange();
		while(hourCount < hours) {
			while(minute != 60) {
				
				//Stall while your time hasn't passed
				while((System.nanoTime() - Started)/((hourCount * 60)+(minute + 1)) < timeThreasholdms){}
				//generate from -100 to 70
				currentReading = new Random().nextInt(70 + 100 + 1) - 100;
				String out = "Thread " + ID + " produced " + currentReading + "F at minute " + (minute + 1) + ". Total time running: " + ((System.nanoTime() - Started)/1000000f + "ms\n");
				System.out.print(out);
				output += out;
				Memory[startingIndex + minute] = currentReading;
				minute++;
			}
			long CalcTime = System.nanoTime();
			Calculate();
			String out = "Thread "+ ID+" calculated in " + (System.nanoTime() - CalcTime)/1000000f + " ms\n";
			System.out.println(out);
			synchronized(this) {output += out;} 
			//spin until all threads have caught up
			if(counter.incrementAndGet() < N) {
				//insert short delay to not crash computer
				while(counter.get() != 0) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else
			{
				OutputReport(hourCount + 1);
				//reset values
				diff.set(0);
				ll = new ConLinkedList();
				counter.set(0);	
			}
			minute = 0;
			hourCount++;
		}
		
	}
}
public class Rover {
	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		System.out.println("How many hours to simulate?");
		int hours = s.nextInt();
		System.out.println("What's the simulated time (how often should the temp nodes grab data in ms)?");
		int time = s.nextInt();
		long startTime = System.nanoTime();
		//System.out.println("The Starting Value is: " + startingValue);
		int N = 8;
		Thread[] tList = new Thread[N];
		
		//setup all the threads
		for(int i = 0; i < N; i++) {
			tList[i] = new Thread(new TempMod(i));
		}
		TempMod.InitTime(time, N, hours);
		//start all threads
		for(Thread t: tList){
			t.start();
		}

		//Wait till all threads die
		try{
			for (Thread thread : tList) {
				thread.join();
			}
		}
		catch (InterruptedException e){
			System.out.println(e);
		}
		long endTime = System.nanoTime();
		String out = "Full program took " + ((endTime - startTime)/1000000f) + "ms to execute";
		System.out.println(out);
		TempMod.output += out;
		Path fileName = Path.of(Paths.get("").toAbsolutePath().toString() + "/_output.txt");
		try {
			Files.writeString(fileName, (TempMod.output));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}

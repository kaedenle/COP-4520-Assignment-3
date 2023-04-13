import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Node {
	int key;
	Node next;
	private Lock locks = new ReentrantLock();
	public Node(int key) {
		this.key = key;
		next = null;
	}
	public void lock() {
		locks.lock();
	}
	public void unlock() {
		locks.unlock();
	}
}

class Chain{
	private Node head;
	public Chain(){
		head = new Node(Integer.MIN_VALUE);
		head.next = new Node(Integer.MAX_VALUE);
	}

	public String getUpToIndexHead(int index) {
		String ret = "\n";
		Node curr = head;
		int counter = 0;
		while(curr.next != null && counter < index) {
			curr = curr.next;
			ret += curr.key + "F\n";
			counter++;
		}
		return ret;
	}
	public boolean add(int item) {
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
				if(curr.key == key) {
					return false;
				}
				Node newNode = new Node(item);
				newNode.next = curr;
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

	public int removeAtIndex(int index) {
		Node pred = null, curr = null;
		int count = 0;
		head.lock();
		try {
			pred = head;
			curr = pred.next;
			curr.lock();
			try {
				while(count < index && curr.next != null) {
					pred.unlock();
					pred = curr;
					curr = curr.next;
					curr.lock();
					count++;
				}
				//the magic
				if(count == index && curr.next != null) {
					pred.next = curr.next;
					return curr.key;
				}
				return -1;
			} finally {
				curr.unlock();
			}
		}finally {
			pred.unlock();
		}
	}
	public boolean seek(int item) {
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
class RandomBag{
	public ArrayList<Integer> list = new ArrayList<>();
	private int size;
	public RandomBag(int size) {
		this.size = size;
		randomize();
	}
	private void randomize() {
		for(int i = 1; i < size + 1; i++) {
			list.add(i);
		}
		Collections.shuffle(list);
	}
}
class Minotaur implements Runnable{
	public Minion[] minionList;
	public Minotaur() {
		
	}
	@Override
	public void run() {
		while(Minion.ThankYouMsg.get() < Minion.size) {
			//delay then choose which thread to stuff
			/*try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			int minionIndex = new Random().nextInt(minionList.length);
			if(Minion.output && minionList[minionIndex].check == false && Minion.ThankYouMsg.get() < Minion.size) System.out.println("Minotaur wants Thread " + minionIndex + " to check for a present\n");
			minionList[minionIndex].check = true;
			
		}
	}
	
}
class Minion implements Runnable{
	public static AtomicInteger ThankYouMsg;
	public static Minotaur mast;
	//how many gifts to insert before removing
	public static int RemoveNumber;
	public int ID;
	private static RandomBag bag;
	public static Chain chain;
	public boolean check;
	private int added = 0;
	public static int size = 500000;
	private static AtomicInteger index = new AtomicInteger(0);
	public static boolean output;
	
	public Minion(int ID) {
		this.ID = ID;
	}
	public static void init(int rn, Minion[] minList) {
		ThankYouMsg = new AtomicInteger(0);
		mast = new Minotaur();
		mast.minionList = minList;
		RemoveNumber = rn > 0 ? rn : 1;
		bag = new RandomBag(size);
		chain = new Chain();
	}

	@Override
	public void run() {
		while(ThankYouMsg.get() < size) {
			//minotaur told thread to check
			if(check == true)
			{
				int checkfor = new Random().nextInt(size);
				String out = "Thread " + ID + " checked for present " + checkfor + " and ";
				if(chain.seek(checkfor)) out += "FOUND it.\n";
				else out += "did NOT find it.\n";
				if(output) System.out.println(out);
				check = false;
			}
			else if(added >= RemoveNumber || index.get() >= size)
			{
				added = 0;
				//remove random present
				int present = chain.removeAtIndex(0);
				if(present != -1)
				{
					ThankYouMsg.incrementAndGet();
					if(output) System.out.println("Thread " + ID + " removed present " + present + " and wrote a THANK YOU!");
				}
			}
			else if(added < RemoveNumber)
			{
				//attempt to add
				int ind = index.getAndIncrement();
				int present = 0;
				if(ind < size) 
				{
					chain.add(present = bag.list.get(ind));
					if(output) System.out.println("Thread " + ID + " put present " + present + " into the list");
				}	
				//if(ind >= size && output) System.out.println("Thread " + ID + " sees an empty bag");
				added++;
			}
		}
	}
	
}
public class Present {

	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		System.out.println("Do you want output (y | n)?");
		String str = s.next();
		if(str.strip().toLowerCase().equals("y")) Minion.output = true;
		else Minion.output = false;
		int removal = 1;
		long startTime = System.nanoTime();
		//System.out.println("The Starting Value is: " + startingValue);
		int N = 4;
		Thread[] tList = new Thread[N + 1];
		Minion[] mList = new Minion[N];
		
		//setup all the threads
		for(int i = 0; i < N; i++) {
			mList[i] = new Minion(i);
			tList[i] = new Thread(mList[i]);
		}
		Minion.init(removal, mList);
		tList[N] = new Thread(Minion.mast);
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
		System.out.println("ALL " + Minion.size + " presents were signed for");
		String out = "Program took " + ((endTime - startTime)/1000000f) + "ms to execute";
		System.out.println(out);
		
		Path fileName = Path.of(Paths.get("").toAbsolutePath().toString() + "/presents_output.txt");
		try {
			Files.writeString(fileName, (out));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        s.close();
	}

}

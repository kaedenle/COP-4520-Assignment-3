import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

class TempMod implements Runnable{
	public int minute;
	public int ID;
	private int currentReading;
	public static int[] Memory;
	public static int N;
	private static int hours;
	//set numbers out of range
	public static AtomicInteger lowest = new AtomicInteger(200);
	public static AtomicInteger highest = new AtomicInteger(-200);
	public static AtomicInteger diff = new AtomicInteger(0);
	public static AtomicInteger timeDiff = new AtomicInteger(0);
	public static int IDContr = 0;
	private static AtomicInteger counter = new AtomicInteger(0);
	private static long timeThreasholdms = 0;
	private static long Started;
	private int startingIndex;
	private int endingIndex;
	public static String output;
	
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
	}
	private void GetOperableRange() {
		startingIndex = ID * 60;
		endingIndex  = (ID + 1) * 60 - 1;
	}
	//compare and set with inequallities
	private boolean greaterThanCompareAndSet(int newValue, AtomicInteger oldval) {
		return oldval.updateAndGet(x -> x < newValue ? newValue : x) == newValue;
	}
	private boolean lessThanCompareAndSet(int newValue, AtomicInteger oldval) {
		return oldval.updateAndGet(x -> x > newValue ? newValue : x) == newValue;
	}
	private void Calculate() {
		for(int i  = startingIndex; i <= endingIndex; i++)
		{
			greaterThanCompareAndSet(Memory[i], highest);
			lessThanCompareAndSet(Memory[i], lowest);
			//test the 10 minute intervals (if you have one)
			if(i + 10 < N*60) {
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
		String out = "-----------------------HOUR " + count + " METRICS-----------------------\n" + "LOWEST: " + lowest + "\nHIGHEST: " + highest + "\nDIFFERENCE: " + diff + " (between " + timeDiff + " and " + (timeDiff.get() + 10) + "ms ID "+ IDContr +")\n";
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
			System.out.print(out);
			output += out;
			//spin until all threads have caught up
			if(counter.incrementAndGet() < N) {
				while(counter.get() != 0) {}
			}
			else
			{
				OutputReport(hourCount + 1);
				lowest.set(200);
				highest.set(-200);
				diff.set(0);
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
		TempMod.output += "Full program took " + ((endTime - startTime)/1000000f) + "ms to execute";
		Path fileName = Path.of(Paths.get("").toAbsolutePath().toString() + "_output.txt");
		try {
			Files.writeString(fileName, (TempMod.output));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        s.close();
		
	}

}

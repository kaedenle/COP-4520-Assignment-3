#Present
## Running
1. Compile the file with javac 'Present.java'
2. Run with 'java Present'
3. There will be 1 prompt asking you if you want output or not

## Explaination
- The program starts by populating an ArrayList with values from 1 to 500000
- This ArrayList is then shuffled
- The minions will alternate between adding presents to the linked list and removing them and writing a thank you note
   * The minotaur will periodically ask a random minion to check for a random present
   * The minion will see if the present is there and output whether or not it exists in the list
   * The Linked List is a fine-grained implementation, making use of hand over hand locking.
- To take from the random bag, each minion will increment a shared index counter.
   * This shared counter is implemented with an AtomicInteger
   * To prevent race conditions with the AtomicInteger, a getAndIncrement is done when trying to access the next element of the shuffled ArrayList
   * The minion will take a random shuffled number form the beginning of the ArrayList
   * If this index counter is over size of the ArrayList, the minions will stop trying to pull presents and only remove.
## Efficiency
 - Instead of pulling a random gift from the Linked List, the first element of the linked list is pulled.
   * This prevents the minion from taking time looking down the line of a long Linked List.
   * A custom function, 'removeAtIndex(int index)' was created to supplement this. When removing we remove the first index by calling 'LinkedListInstance.removeAtIndex(0)'
## Correctness
 - The Linked List in this program makes use of Hand over Hand Locking
   * To prevent race conditions and incorrect results, 2 node objects will be locked in the linked list.
   * These are the predecessor and the current node. 
   * When moving onto the next node, only the predecessor is unlocked and the successor becomes locked. THe successor is the new current and the old current is the new predecessor
 - This is applied whether each minion is checking to see if a gift exists, removing the first element (the head is locked), or adding an element further down the chain.
## Progress
 - Due to the locking involved in the linked list, there is no contention that can occur among the minions and thus all can make progress.
 - Furthermore, due to how the shared index when pulling from the bag is implemented, each thread will recognize when there are no more gifts to pull from the random bag. Even if they were currently trying to add a new gift.
 
# Rover
## Running
1. Compile the file with 'javac Rover.java'
2. Run with 'java Rover'
3. There will be 2 prompts by the program
  * Hours: How many cycles of collection (60 minutes) would you like to simulate
  * Simulated Time (ms, integer): How long should the collection intervals be (this will simulate the minute given in the assignment prompt). 0ms will try to run the program as fast as possible, although it can't keep up with it.

## Explaination
### Shared Memory
- To handle the shared memory a simple array was used. The array was split into N sections, with N being the number of threads in the program (8 in this case).
- The total size of the shared memory array was N * 60.
- Each thread would get 60 array cells to store their data from minute 1 to minute 60.
- Things were initially set in an array as to make it easier to access later
### Simulating Time
- If the collection time hadn't passed yet (or the simulated time set above) the threads would be spun until it was their time to collect data.
- The random data point produced would be stored in the thread's memory section.
### Calculation
- A fine-grained Linked List was implemented to store all the values.
- Each thread has control over their 60 cells. As we go through the 60 cells we also check which 10 minute interval had the largest difference
- We get the interval by this equation: Math.abs(Memory[currentIndex] - Memory[currentIndex + 10])
  * We use a custom implemented greaterThanCompareThanSet to see if the current difference is larger than the largest difference. If it is replace the 'largest difference' with the next largest difference
  * We lock the section of the code that's doing the comparision then setting.
  * We obtain the minute we're currently at and the Thread that had the interval.
  * Optimizations can be made here
- Each thread dumps their portion of the array into the linked list (there's a static boolean in the linked list class that tells us if we want unique values or not)
- The linked list was modified to have access to the previous nodes as well as access to the tail.
- The top 5 and bottom 5 were calculated from the tail and head respectively.

## Efficiency
- The decision to use an array to store the list allows easy checking of the interval. 
- Instead of having one thread check all the array for the interval and put all the values of the array into the linked list, we use all 8 threads.
- Each thread scans their own 60 cells of the global array. This ensures each thread is doing the same amount of work.
- We use a Fine-Grained Linked List to efficiently add and access the top and bottom 5 temperatures for the given hour. To get the top 5 temperatures an optimization was made to the structure of the FGLL.
- The FGLL has a tail variable as well as a prev value. This allows easy access to the end of the LL to output the top 5 values.

## Correctness
- The lock around the section doing the interval checking, although could present slow downs, ensures there are no race conditions that can occur by only allowing one thread to set their largest interval value.
- The Fine-Grained Linked list locks both the current and the next when adding (the only relevant operation in this code). 
  * Since there's no point in which we're modifying the middle of the Linked List there's no errors that can occur with this Fine-Grained implementation
- Since each thread is only concerned about their own section when calculating the top 5, bottom 5 and interval, there's no collision that can occur.
- At the end of the hour, all threads are forced to wait until every single thread has finished putting the values in the linked list and determining the largest interval.
  * The reason for waiting till all threads finish is so that there's no missing values that could've been the largest interval or in the top/bottom 5.
  * Once the last thread finishes, it outputs the report and resets the global values like the interval diff and the linked list to prepare for the next hour. The last thread then frees all the waiting threads.
  * This order is important as freeing the waiting threads first then creating the report could result in the final thread being behind the rest of the threads for the next hour.
  
## Progress
- Progress is guarenteed by the seperation of threads in the array where the valaues are being stored.
- Since the threads don't have to talk to each other much, they can all finish their tasks individually
  * They don't have to rely on each other. This fact avoids any potential for deadlocks.
- The linked list ensures progress as there's no situation in the program where any disjointed access occurs on the linked list
  * This circumvents any errors associated with the fine-grained implementation of a concurrent linked list
- Each thread unlocks the nodes of the linked list after they're done, ensuring no thread is stuck waiting for a node in the Linked List that'll never unlock.

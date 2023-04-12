# Rover
## Running
1. Compile the file with javac Rover.java
2. Run with java Rover
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

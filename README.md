## Kotlin utils

This project is just a Kotlin port of my Java utils

#### Cleaner

`Cleaner` is an utility class to clean up some generic objects (can be files, DB) based on a fixed retention policy.
For example X yearly elaborations, Y monthly, Z daily etc.

#### Collections

* `Node` class to represent a tree with a Key and a Value
* `PriorityHashSet` for choosing objects with same hash based on a priority

### Dequeuer
`Dequeuer` is an utility for processing many items in parallel.

`ChainedDequeuer` for processing items in a chain of dequeuers

##### With coroutines
It has two implementations for massive concurrent execution and almost unlimited number of workers:
* `CoroutineDequeuer` for basic processing with a fixed number of workers
* `BalancedCoroutineDequeuer` for processing with a dynamic number of workers based on a rudimentary balancing algorithm

##### With classic threads executors
It has two implementations for massive parallel execution but limited number of threads:
* `ThreadDequeuer` for basic processing with a fixed number of workers
* `BalancedThreadDequeuer` for processing with a dynamic number of workers based on a rudimentary balancing algorithm

#### Delta

`DeltaCalculator` is a simple mechanism to compare two lists and find out common/additional/missing elements from both lists

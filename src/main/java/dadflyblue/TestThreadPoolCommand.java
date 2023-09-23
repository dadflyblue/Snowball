package dadflyblue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

@Command(name = "test-threadpool",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
public class TestThreadPoolCommand implements Runnable {
  @Option(names = {"--works", "-w"}, defaultValue = "10", description = "The number of workers.")
  int count;

  private static void testAndAutoClose(Collection<? extends Callable<Void>> workers, ExecutorService exec, String name) {
    try (exec) {
      var start = System.currentTimeMillis();
      var fs = exec.invokeAll(workers);
      waitAll(fs);
      var end = System.currentTimeMillis();
      System.out.printf("all workers(%d) from %s returns: %dms%n", workers.size(), name, end - start);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void waitAll(Collection<? extends Future<?>> fs) {
    fs.forEach(f -> {
      try {
        f.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void run() {
    final var random = new SecureRandom();
    System.out.printf("start to test with worker count: %d%n", count);
    var workers = new ArrayList<Worker>(count);
    for (int i = 0; i < count; i++) {
      workers.add(new Worker(1000 + random.nextInt(5000), i));
    }

    testAndAutoClose(workers, ForkJoinPool.commonPool(), "platform common thread pool");
    testAndAutoClose(workers, Executors.newCachedThreadPool(), "platform cached thread pool");
    testAndAutoClose(workers, Executors.newVirtualThreadPerTaskExecutor(), "virtual thread pool");
  }

  record Worker(long timeout, long id) implements Callable<Void> {

    @Override
    public Void call() {
      try {
        System.out.println(
            "worker - " + id + " is working on: " + Thread.currentThread());
        Thread.sleep(timeout);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return null;
    }
  }

}

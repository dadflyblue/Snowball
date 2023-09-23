package dadflyblue;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "snowball", mixinStandardHelpOptions = true,
    subcommands = {TestThreadPoolCommand.class, VertxCurlCommand.class},
    versionProvider = VersionProvider.class)
public class SnowballCommand implements Runnable {

  @Override
  public void run() {
    System.out.printf("Hello from %s!%n", "snowball");
  }

}

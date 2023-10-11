package dadflyblue;


import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.StaticHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.CountDownLatch;

import static io.vertx.ext.web.handler.FileSystemAccess.*;

@Command(name = "static-server",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class)
public class StaticServerCommand implements Runnable {

  @Parameters(paramLabel = "<path>", defaultValue = "./",
      description = "The serving path of static server, default: ./")
  String path;

  @Option(names = {"--port", "-p"}, defaultValue = "9000",
      description = "The serving port of static server, default: 9000")
  int port;

  @Option(names = {"--dir"}, defaultValue = "true",
      description = "If list directory? When it's false, will open \"index.html\", default: true")
  boolean listDir;

  @Option(names = {"--root"}, defaultValue = "false",
      description = "If can access full file system, starting at \"/\"?, default: false")
  boolean root;

  @Override
  public void run() {
    var vertx = Vertx.vertx();
    var r = Router.router(vertx);

    r.route("/*").handler(
        StaticHandler.create(root ? ROOT: RELATIVE, path)
            .setDirectoryListing(listDir));
    vertx.createHttpServer()
        .requestHandler(r)
        .listenAndAwait(port);

    System.out.printf("static-server is serving on: [:%d], at: %s%n", port, path);

    // Since vertx is serving the requests in an async way,
    // we need to hold the main thread to prevent the process from terminating.
    var l = new CountDownLatch(1);
    try {
      l.await();
    } catch (InterruptedException ignored) {
    }
  }
}

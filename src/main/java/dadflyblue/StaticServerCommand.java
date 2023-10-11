package dadflyblue;


import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.StaticHandler;
import picocli.CommandLine.*;

import java.util.concurrent.CountDownLatch;

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

  @Override
  public void run() {
    var vertx = Vertx.vertx();
    var r = Router.router(vertx);

    r.route("/*").handler(StaticHandler.create(path));
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

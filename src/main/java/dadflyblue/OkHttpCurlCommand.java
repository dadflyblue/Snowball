package dadflyblue;

import dadflyblue.unixsocket.UnixDomainSocketFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Command(name = "ok-curl",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class)
public class OkHttpCurlCommand implements Runnable {

  @Parameters(paramLabel = "<url>", defaultValue = "", description = "The http url.")
  String url;

  @Option(names = {"--data", "-d"}, description = "The data(payload) to send.")
  String data;

  @Option(names = {"--method", "-m"}, defaultValue = "GET", description = "The http method, default: GET.")
  String method;

  @Option(names = {"--unix-socket", "-x"}, description = "The unix socket address.")
  String unixSocketAddress;

  @Override
  public void run() {
    var client = new OkHttpClient.Builder()
      .callTimeout(Duration.ofDays(1))
      .readTimeout(Duration.ofDays(1))
      .writeTimeout(Duration.ofDays(1))
      .build();

    // It works for native-image of GraalVM.
    if (unixSocketAddress != null) {
      var b = client.newBuilder();
      b.setSocketFactory$okhttp(
          new UnixDomainSocketFactory(unixSocketAddress));
      client = b.build();
    }

    var request = new Request.Builder()
        .url(url)
        .method(method, (data != null) ?
          RequestBody.create(data.getBytes(StandardCharsets.UTF_8)) :
          null)
        .build();
    try (var response = client.newCall(request).execute()) {
      ResponseBody body = response.body();
      if (body != null) {
        try (body; var in = body.byteStream()) {
          in.transferTo(System.out);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

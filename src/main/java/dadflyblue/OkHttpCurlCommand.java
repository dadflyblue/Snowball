package dadflyblue;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.newsclub.net.unix.AFSocketFactory;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@CommandLine.Command(name = "ok-curl",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class)
public class OkHttpCurlCommand implements Runnable {

  @CommandLine.Parameters(paramLabel = "<url>", defaultValue = "", description = "The http url.")
  String url;

  @CommandLine.Option(names = {"--data", "-d"}, description = "The data(payload) to send.")
  String data;

  @CommandLine.Option(names = {"--method", "-m"}, defaultValue = "GET", description = "The http method, default: GET.")
  String method;

  @CommandLine.Option(names = {"--unix-socket", "-x"}, description = "The unix socket address.")
  String unixSocketAddress;

  @Override
  public void run() {
    var client = new OkHttpClient.Builder()
      .callTimeout(Duration.ofDays(1)).build();

    if (unixSocketAddress != null) {
      try {
        var b = client.newBuilder();
        b.setSocketFactory$okhttp(
            new AFSocketFactory.FixedAddressSocketFactory(
                AFUNIXSocketAddress.of(new File(unixSocketAddress))));
        client = b.build();
      } catch (SocketException e) {
        throw new RuntimeException(e);
      }
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

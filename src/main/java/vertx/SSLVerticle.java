package vertx;

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class SSLVerticle extends AbstractVerticle {

	private static Vertx vertx;

	public static void main(String[] args) {

		JsonObject conf = new JsonObject();
		conf.put("http.port", 8080);
		conf.put("ssl", true);
		DeploymentOptions options = new DeploymentOptions().setConfig(conf);

		vertx = Vertx.vertx();
		vertx.deployVerticle(SSLVerticle.class.getName(), options);
	}

	@Override
	public void start(Future<Void> startFuture) {
		JsonObject config = config();
		final Integer bindPort = config.getInteger("http.port");
		final String bindAddress = "localhost";
		System.out.println("vertx port: " + bindPort);

		Router router = Router.router(vertx);

		// welcome page
		router.get("/").handler(ctx -> {
			System.out.println("/ page r : " + ctx.request().query());
			ctx.response().putHeader("content-type", "text/html")
					.end("Hello<br><a href=\"/protected/somepage\">Protected by ssl</a>");
		});

		router.route("/account/token").handler(this::authenticate);

		// If SSL is requested, prepare the SSL configuration off of the event
		// bus to prevent blocking.
		if (config.containsKey("ssl") && config.getBoolean("ssl")) {

			System.out.println("Using existing certificate");

			vertx.executeBlocking(future -> {
				HttpServerOptions httpOpts = new HttpServerOptions();
				if (config.containsKey("certificate-path")) {
					String certPath = config.getString("certificate-path");

					// Use a Java Keystore File
					if (certPath.toLowerCase().endsWith("jks") && config.getString("certificate-password") != null) {
						httpOpts.setKeyStoreOptions(new JksOptions()
								.setPassword(config.getString("certificate-password")).setPath(certPath));
						httpOpts.setSsl(true);

						// Use a PKCS12 keystore
					}
					else if (config.getString("certificate-password") != null
							&& certPath.matches("^.*\\.(pfx|p12|PFX|P12)$")) {
						httpOpts.setPfxKeyCertOptions(new PfxOptions()
								.setPassword(config.getString("certificate-password")).setPath(certPath));
						httpOpts.setSsl(true);

						// Use a PEM key/cert pair
					}
					else if (certPath.matches("^.*\\.(pem|PEM)$")) {
						httpOpts.setPemKeyCertOptions(
								new PemKeyCertOptions().setCertPath(certPath).setKeyPath(certPath));
						httpOpts.setSsl(true);
					}
					else {
						startFuture.fail("A certificate file was provided, but a password for that file was not.");
					}
				}
				else {
					try {

						System.out.println("Generate certificate");

						// Generate a self-signed key pair and certificate.
						KeyStore store = KeyStore.getInstance("JKS");
						store.load(null, null);
						CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA256WithRSA", null);
						X500Name x500Name = new X500Name("localhost", "IT", "unknown", "unknown", "unknown", "unknown");
						keypair.generate(1024);
						PrivateKey privKey = keypair.getPrivateKey();
						X509Certificate[] chain = new X509Certificate[1];
						chain[0] = keypair.getSelfCertificate(x500Name, new Date(), (long) 365 * 24 * 60 * 60);
						store.setKeyEntry("selfsigned", privKey, "changeit".toCharArray(), chain);
						store.store(new FileOutputStream(".keystore"), "changeit".toCharArray());
						httpOpts.setKeyStoreOptions(new JksOptions().setPath(".keystore").setPassword("changeit"));
						httpOpts.setSsl(true);
					}
					catch (Exception ex) {
						System.out.println("Cannot generate certificate");
						ex.printStackTrace();
						startFuture.fail(ex);
					}
				}
				future.complete(httpOpts);
			}, (AsyncResult<HttpServerOptions> result) -> {
				if (!result.failed()) {
					vertx.createHttpServer(result.result()).requestHandler(router::accept).listen(bindPort,
							bindAddress);
					System.out.println("SSL Web server now listening");
					startFuture.complete();
				}
			});
		}
		else {
			// No SSL requested, start a non-SSL HTTP server.
			vertx.createHttpServer().requestHandler(router::accept).listen(bindPort, bindAddress);
			System.out.println("Web server now listening");
			startFuture.complete();
		}

	}

	private void authenticate(RoutingContext routingContext) {

		String login = routingContext.request().getParam("login");
		String passwd = routingContext.request().getParam("passwd");

		System.out.println("authenticate " + login + ", " + passwd);

		if (login == null) {
			routingContext.response().setStatusCode(400).end();

		}
		else {
			UUID token = UUID.randomUUID();
			routingContext.response().putHeader("content-type", "text/plain; charset=utf-8").end(token.toString());
		}

	}

}
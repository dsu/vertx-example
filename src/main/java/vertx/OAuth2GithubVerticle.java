package vertx;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class OAuth2GithubVerticle extends AbstractVerticle {

	private static final String CLIENT_ID = "45b87f94e1dfbbe2eb02";
	private static final String CLINET_SECRET = "37b285d253e21ddbca3c05a09bb538f4be8cfe2c";

	private static Vertx vertx;

	public static void main(String[] args) {

		JsonObject conf = new JsonObject();
		conf.put("http.port", 8080);
		DeploymentOptions options = new DeploymentOptions().setConfig(conf);

		vertx = Vertx.vertx();
		vertx.deployVerticle(OAuth2GithubVerticle.class.getName(), options);
	}

	@Override
	public void start(Future<Void> fut) {

		System.out.println("vertx port: " + config().getInteger("http.port"));

		Router router = Router.router(vertx);

		OAuth2Auth authProvider = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE,
				new OAuth2ClientOptions().setClientID(CLIENT_ID).setClientSecret(CLINET_SECRET)
						.setSite("https://github.com/login").setTokenPath("/oauth/access_token")
						.setAuthorizationPath("/oauth/authorize"));

		// when there is a need to access a protected resource or call a
		// protected method,
		// call the authZ url for a challenge

		// when working with web application use the above string as a redirect
		// url

		System.out.println("paths initialized : ");

		OAuth2AuthHandler oauth2 = OAuth2AuthHandler.create(authProvider, "http://localhost:8080");

		// these are the scopes
		oauth2.addAuthority("profile");
		// setup the callback handler for receiving the Google callback
		//oauth2.setupCallback(router.get("/callback"));
		// protect everything under /protected
		router.route("/protected/*").handler(oauth2);
		// mount some handler under the protected zone
		router.route("/callback").handler(rc -> {
			System.out.println("protected page r : " + rc.request().query());
			// code=1f66006da49c2541fcaa&redirect_uri=%2Fprotected%2Fsomepage
			// ode=bd16897a5ec286ba2c07&redirect_uri=%2Fprotected%2Fsomepage

			System.out.println("1. " + rc.pathParam("code") + " 2. " + rc.request().getParam("code") + " 3. "
					+ rc.get("code") + " 4. " + rc.request().getFormAttribute("code") + " 5. " + rc.request());

			System.out.println("params : " + rc.request().params().size());
			rc.request().params().forEach((x) -> System.out.println(x));

			Map<String, String> splitQuery = splitQuery(rc.request().query());

			String code = splitQuery.get("code");

			if (code == null) {
				throw new RuntimeException("code is null");
			}
			System.out.println("code : " + code);

			JsonObject tokenConfig = new JsonObject().put("code", code).put("redirect_uri",
					"http://localhost:8080/callback");

			authProvider.getToken(tokenConfig, res -> {
				if (res.failed()) {
					System.out.println("REQUEST FAILURE");
					System.err.println("Access Token Error: " + res.cause().getMessage());
				}
				else {
					System.out.println("OK");
					System.out.println("r : " + res.result().expired() + " , " + res.result().principal());
				}
			});

			rc.response().end("Welcome to the protected resource!");
		});
		// welcome pages
		router.route("/index").handler(ctx -> {
			
			System.out.println("request "  + ctx.request().path());
			
			String authorization_uri = authProvider
					.authorizeURL(new JsonObject().put("redirect_uri", "http://localhost:8080/callback")
							.put("scope", "notifications").put("state", "3(#0/!~"));
			
			
			System.out.println("redirect to : " + authorization_uri);

			// Redirect example using Vert.x
			ctx.response().putHeader("Location", authorization_uri).setStatusCode(302).end();
		});

		vertx.createHttpServer().requestHandler(router::accept).listen(
				// Retrieve the port from the configuration,
				// default to 8080.
				config().getInteger("http.port", 8080), result -> {
					if (result.succeeded()) {
						fut.complete();
					}
					else {
						fut.fail(result.cause());
					}
				});
	}

	public static Map<String, String> splitQuery(String query) {
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			try {
				query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
						URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
				System.out.println("pair : " + pair.substring(0, idx) + " " + pair.substring(idx + 1));
			}
			catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return query_pairs;
	}

}
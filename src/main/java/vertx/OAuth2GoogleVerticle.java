package vertx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class OAuth2GoogleVerticle extends AbstractVerticle {

	private static final String GOOGLE_CALLBACK = "http://localhost:8080/oauth2callback";
	private static final String GOOGLE_ID = "19722097440-463kmh1ithm2rsch9hfe1t34m4ft6i5c.apps.googleusercontent.com";
	private static final String GOOGLE_SECRET = "k9DNtZu8xQtY15hEyhkhJl0j";

	private static Vertx vertx;

	public static void main(String[] args) {

		JsonObject conf = new JsonObject();
		conf.put("http.port", 8080);
		DeploymentOptions options = new DeploymentOptions().setConfig(conf);

		vertx = Vertx.vertx();
		vertx.deployVerticle(OAuth2GoogleVerticle.class.getName(), options);
	}

	@Override
	public void start(Future<Void> fut) {

		System.out.println("vertx port: " + config().getInteger("http.port"));

		Router router = Router.router(vertx);

		OAuth2ClientOptions credentials = new OAuth2ClientOptions().setClientID(GOOGLE_ID)
				.setClientSecret(GOOGLE_SECRET).setSite("https://accounts.google.com")
				.setTokenPath("https://www.googleapis.com/oauth2/v3/token").setAuthorizationPath("/o/oauth2/auth");

		// Initialize the OAuth2 Library
		OAuth2Auth authProvider = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, credentials);
		

		
		OAuth2AuthHandler oauth2 = OAuth2AuthHandler.create(authProvider, "http://localhost:8080");
		// these are the scopes
		oauth2.addAuthority("profile");
		// setup the callback handler for receiving the Google callback
		oauth2.setupCallback(router.get("/oauth2callback"));

		// protect everything under /protected
		router.route("/protected/*").handler(oauth2);
		// mount some handler under the protected zone
		router.route("/protected/somepage").handler(rc -> {
			System.out.println("protected page r : " + rc.request().query());
			rc.response().end("Welcome to the protected resource!");
		});
		// welcome page
		router.get("/").handler(ctx -> {
			System.out.println("/ page r : " + ctx.request().query());
			ctx.response().putHeader("content-type", "text/html")
					.end("Hello<br><a href=\"/protected/somepage\">Protected by Google</a>");
		});

		System.out.println("paths initialized");

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

}
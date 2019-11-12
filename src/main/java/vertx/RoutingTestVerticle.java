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
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class RoutingTestVerticle extends AbstractVerticle {

	private static Vertx vertx;

	public static void main(String[] args) {

		JsonObject conf = new JsonObject();
		conf.put("http.port", 8080);
		DeploymentOptions options = new DeploymentOptions().setConfig(conf);

		vertx = Vertx.vertx();
		vertx.deployVerticle(RoutingTestVerticle.class.getName(), options);
	}

	@Override
	public void start(Future<Void> fut) {

		System.out.println("vertx port: " + config().getInteger("http.port"));

		Router router = Router.router(vertx);
		
		//generalnie to czy da sie / na koniec nie ma znaczenia.

		router.route("/").handler(routingContext -> {
			System.out.println("/");
			routingContext.response().putHeader("content-type", "text/html");
			routingContext.next();
		});

		router.route("/a").handler(routingContext -> {
			System.out.println("/a");
			routingContext.response().putHeader("content-type", "text/html").end("/a");
		});
		
		router.route("/a").handler(routingContext -> {
			System.out.println("/a");
			routingContext.response().putHeader("content-type", "text/html").end("/a2");
		});

		router.route("/a/a").handler(routingContext -> {
			routingContext.response().putHeader("content-type", "text/html").end("/a/a");
		});

		router.route("/b").handler(routingContext -> {
			System.out.println("/b");
			routingContext.response().putHeader("content-type", "text/html");
			routingContext.next();
		});
		
		
		router.route("/b").handler(routingContext -> {
			System.out.println("/b2");
			routingContext.response().putHeader("content-type", "text/html").end("/b2");
		});


		System.out.println("paths initialized ");

		vertx.createHttpServer().requestHandler(router::accept).listen(
				// Retrieve the port from the configuration,
				// default to 8080.
				config().getInteger("http.port", 8080), result -> {
					if (result.succeeded()) {
						fut.complete();
						System.out.println("OK");
					}
					else {
						fut.fail(result.cause());
						System.out.println("failure");
					}
				});

	}

}
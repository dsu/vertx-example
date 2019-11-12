package vertx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import vertx.model.Whisky;

public class MyFirstWebVerticle extends AbstractVerticle {

	@Override
	public void start(Future<Void> fut) {

		System.out.println("vertx port: " + config().getInteger("http.port"));
		createSomeData();
		// 10.1.2.4
		Router router = Router.router(vertx);
		router.route("/").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html").end("<h1>Witam from my first Vert.x 3 application</h1>");
		});

	
		// Serve static resources from the /assets directory
		router.route("/assets/*").handler(StaticHandler.create("assets"));

		// read response body only for this patch,
		// router.route().handler(BodyHandler.create()). enables it globally
		router.route().handler(BodyHandler.create());

		router.route("/account/token").handler(this::authenticate);

		router.get("/api/games").handler(this::getAll);

		router.post("/api/games").handler(this::addOne);

		router.delete("/api/games/:id").handler(this::deleteOne);

		router.get("/api/games/:id").handler(this::getOne);
		router.put("/api/games/:id").handler(this::updateOne);

		router.get("/hiscores").handler(this::getHiscore);
		router.get("/hiscore/:val").handler(this::updateHiscore);

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

	// Store our product
	private Map<Integer, Whisky> products = new LinkedHashMap<>();

	// Create some product
	private void createSomeData() {
		Whisky bowmore = new Whisky("Duże piwo", "Jan Kowalski");
		products.put(bowmore.getId(), bowmore);

		Whisky talisker = new Whisky("500 $", "Andrzej Nowak");
		products.put(talisker.getId(), talisker);

		Whisky polish = new Whisky("Ściółka", "Roman Polak");
		products.put(polish.getId(), polish);
	}

	/**
	 * ~ 1.3 ms ..., lokalny serwer 13 - 16 ms
	 * 
	 * @param routingContext
	 */
	private void getAll(RoutingContext routingContext) {
		long nanoTime = System.nanoTime();

		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(products.values()));

		System.out.println("exec time " + (System.nanoTime() - nanoTime));
	}

	private void addOne(RoutingContext routingContext) {
		System.out.println("received: " + routingContext.getBodyAsString());
		final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(), Whisky.class);
		products.put(whisky.getId(), whisky);
		routingContext.response().setStatusCode(201).putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(whisky)); // status 201 mens "created"
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

	private void deleteOne(RoutingContext routingContext) {
		String id = routingContext.request().getParam("id");
		if (id == null) {
			routingContext.response().setStatusCode(400).end();
			//
		}
		else {
			Integer idAsInteger = Integer.valueOf(id);
			products.remove(idAsInteger);
		}
		routingContext.response().setStatusCode(204).end();
		// 204 - NO CONTENT
	}

	private Integer hiscore = null;

	private void getHiscore(RoutingContext routingContext) {

		System.out.println("getHiscore");

		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(hiscore));

	}

	private void updateHiscore(RoutingContext routingContext) {

		System.out.println("updateHiscore : " + routingContext.request().getParam("val"));
		hiscore = Integer.valueOf(routingContext.request().getParam("val"));

		routingContext.response().setStatusCode(201).putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(null));
	}

	private void getOne(RoutingContext routingContext) {
		final String id = routingContext.request().getParam("id");
		if (id == null) {
			routingContext.response().setStatusCode(400).end();
		}
		else {
			final Integer idAsInteger = Integer.valueOf(id);
			Whisky whisky = products.get(idAsInteger);
			if (whisky == null) {
				routingContext.response().setStatusCode(404).end();
			}
			else {
				routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(whisky));
			}
		}
	}

	private void updateOne(RoutingContext routingContext) {
		final String id = routingContext.request().getParam("id");
		JsonObject json = routingContext.getBodyAsJson();

		System.out.println("update One req:  " + id + ", json: " + json);

		if (id == null || json == null) {

			System.out.println("Invalid parameters");

			routingContext.response().setStatusCode(400).end();
		}
		else {
			final Integer idAsInteger = Integer.valueOf(id);
			Whisky whisky = products.get(idAsInteger);
			if (whisky == null) {
				routingContext.response().setStatusCode(404).end();

				System.out.println("Invalid id");
			}
			else {
				whisky.setName(json.getString("name"));
				whisky.setOrigin(json.getString("origin"));
				routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(whisky));
			}
		}
	}
}
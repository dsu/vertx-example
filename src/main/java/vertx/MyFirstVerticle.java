package vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class MyFirstVerticle extends AbstractVerticle {

	@Override
	public void start(Future<Void> fut) {
		
		//the same handler for every path
		vertx.createHttpServer().requestHandler(r -> {
			r.response().end("<h1>Hello from my first " + "Vert.x 3 application</h1>");
		}).listen(config().getInteger("http.port", 8080), result -> {
			if (result.succeeded()) {
				fut.complete();
			}
			else {
				fut.fail(result.cause());
			}
		});
	}
}
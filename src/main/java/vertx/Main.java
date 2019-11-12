package vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

/**
 * Verx schould be run with with Verx Main class deifned at Maven build : java -jar target/my-first-app-1.0-SNAPSHOT-fat.jar
 * -conf src/main/conf/my-application-conf.json
 * 
 * This class if for quick test purposes
 * 
 * @author dsu
 *
 */
public class Main {

	private static Vertx vertx;

	public static void main(String[] args) {

		JsonObject conf = new JsonObject();
		conf.put("http.port", 8899);
		vertx = Vertx.vertx(new VertxOptions(conf).setWorkerPoolSize(1));
		vertx.deployVerticle(MyFirstVerticle.class.getName());
	}
}

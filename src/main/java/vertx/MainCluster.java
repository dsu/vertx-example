package vertx;

import java.util.Map;
import java.util.Set;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * Verx schould be run with with Verx Main class deifned at Maven build : java
 * -jar target/my-first-app-1.0-SNAPSHOT-fat.jar -conf
 * src/main/conf/my-application-conf.json
 * 
 * This class if for quick test purposes
 * 
 * @author dsu
 *
 */
public class MainCluster {

	public static void main(String[] args) {

		Config hazelcastConfig = new Config();

		ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

		VertxOptions options = new VertxOptions().setClusterManager(mgr);

		Vertx.clusteredVertx(options, res -> {
			if (res.succeeded()) {
				Vertx vertx = res.result();

				EventBus eventBus = vertx.eventBus();
				System.out.println("We now have a clustered event bus: " + eventBus);

				Set<HazelcastInstance> instances = Hazelcast.getAllHazelcastInstances();
				HazelcastInstance hz = instances.stream().findFirst().get();
				Map map = hz.getMap("mapName"); // shared distributed map

				System.out.println("MAP : " + map);

				JsonObject conf = new JsonObject();
				conf.put("http.port", 8080);
				DeploymentOptions weboptions = new DeploymentOptions().setConfig(conf);

				vertx.deployVerticle(MyFirstWebVerticle.class.getName(), weboptions);

			}
			else {
				System.out.println("Failed: " + res.cause());
			}
		});

	}
}

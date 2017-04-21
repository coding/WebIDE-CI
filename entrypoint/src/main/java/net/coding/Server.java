package net.coding;

/**
 * Created by vangie on 2017/4/16.
 */

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import io.vertx.rxjava.ext.web.templ.ThymeleafTemplateEngine;
import rx.Observable;
import rx.Subscriber;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends AbstractVerticle {

    private final Map<String, JsonObject> pullMap = new ConcurrentHashMap<>();

    final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

    public void start() {
        vertx.createHttpServer()
                .requestHandler(router(vertx)::accept)
                .listen(8080);
        vertx.setPeriodic(1000 * 60 * 5, timerID -> updatePR());
        updatePR();
    }

    private Router router(Vertx vertx) {
        Router router = Router.router(vertx);
        router.route().handler(CookieHandler.create());
        router.get("/").handler(this::index);
        router.get("/:owner/:repo/pull/:id").handler(this::deployment);
        router.route().handler(StaticHandler.create());
        return router;
    }

    private void index(RoutingContext ctx) {

        ctx.put("pullMap", pullMap);

        engine.render(ctx, "templates/index.html", res -> {
            if (res.succeeded()) {
                ctx.response().end(res.result());
            } else {
                ctx.fail(res.cause());
            }
        });


    }

    private WebClient client() {
        WebClientOptions options = new WebClientOptions()
                .setDefaultHost("api.github.com")
                .setDefaultPort(443)
                .setSsl(true)
                .setMaxPoolSize(1)
                .setKeepAlive(false);

        return WebClient.create(vertx, options);
    }

    private void updatePR() {
        Observable.merge(
                pullRequests("Coding", "WebIDE-Frontend"),
                pullRequests("Coding", "WebIDE-Backend")
        )
                .map(e ->
                        new SimpleEntry<>(e.getString("html_url").substring("https://github.com/".length()), e)
                )
                .subscribe(new Subscriber<SimpleEntry<String, JsonObject>>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(SimpleEntry<String, JsonObject> e) {
                        pullMap.put(e.getKey(), e.getValue());
                    }

                    @Override
                    public void onStart() {
                        pullMap.clear();
                    }
                });
    }

    private Observable<JsonObject> pullRequests(String owner, String repo) {
        return client().get(String.format("/repos/%s/%s/pulls", owner, repo))
                .addQueryParam("state", "open")
                .addQueryParam("sort", "updated")
                .addQueryParam("direction", "desc")
                .as(BodyCodec.jsonArray())
                .rxSend()
                .map(res -> res.body())
                .flatMapObservable(jsonArray -> Observable.from(jsonArray))
                .map(obj -> (JsonObject) obj);
    }

    private void deployment(RoutingContext ctx) {
        String owner = ctx.request().getParam("owner");
        String repo = ctx.request().getParam("repo");
        String id = ctx.request().getParam("id");
        String scheme = ctx.request().scheme();
        String host = ctx.request().host();

        JsonObject pull = pullMap.get(String.format("/repos/%s/%s/pulls/%s", owner, repo, id));

        String revision = "latest";

        if (repo.equals("WebIDE-Frontend")) {
            revision = revision(pull);
        }

        String url = formatUrl(scheme, "frontend", revision, host);

        revision = "latest";
        if (repo.equals("WebIDE-Backend")) {
            revision = revision(pull);
        }
        Cookie cookie = Cookie.cookie("BACKEND_URL", formatUrl(scheme, "backend", revision, host));
        cookie.setPath("/");
        cookie.setDomain("." + host.replaceAll(":.*", ""));

        ctx.addCookie(cookie)
                .response()
                .setStatusCode(307)
                .putHeader("location", url)
                .end();

    }

    private String revision(JsonObject pull) {
        if (pull == null) {
            return "latest";
        }
        return pull.getJsonObject("head").getString("sha").substring(0, 8);
    }

    private String formatUrl(String scheme, String module, String revision, String host) {
        return String.format("%s://%s-%s.%s", scheme, module, revision, host);
    }
}
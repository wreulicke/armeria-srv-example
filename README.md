Armeriaで始めるDNSのSRV Recordを使った クライアントサイドロードバランシング

# 概要

今回は、Armeriaを使って、DNSのSRV Recordを使ったクライアントサイドロードバランシングをやってみました。

ソースコードは[このリポジトリ](https://github.com/wreulicke/armeria-srv-example)においています。

クライアントサイドロードバランシングの必要性については以下の記事を読むと良いかもしれません。
いくつか現実とそぐわない点があるかもしれませんが。

* [Microservices: Client Side Load Balancing](https://www.linkedin.com/pulse/microservices-client-side-load-balancing-amit-kumar-sharma/)
  * 自分が和訳したもので恐縮ですが、リンクを貼っておきます。 

今回動かす構成としては以下の通りです。

* server: Hello Worldを返すエンドポイントを持つHTTPサーバ
* client: Armeriaの機能を使って、クライアントサイドロードバランシングしながらserverに接続する

serverは、シンプルなもgolangで書かれたサーバです。
clientは、クライアントサイドロードバランシングしながらserverに接続します。
マイクロサービスの文脈で使おうと思っているので、今回はclientもHTTPサーバとして起動し、APIを持っています。

また、動作確認には、AWSのECSで、Fargateタイプでコンテナを起動しながら、動作確認をしました。
ECSではService Discoveryの機能を使ってDNSのSRVレコードを生成することが出来るので、便利です。
今回はECSコンテナインスタンスを立てるのが面倒なので、Fargateを使います。

Service Discoveryの設定等については以下のブログを参考にしました。
SRVレコードが登録されるように、設定してください。

* https://dev.classmethod.jp/cloud/aws/ecs-service-discovery/

## server

golangで書きます。

```go
package main

import (
	"log"
	"net/http"
	"time"
)

func main() {
	m := http.NewServeMux()
	m.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("Hello World!\n"))
	})
	srv := &http.Server{
		Addr:         ":8080",
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		Handler:      m,
	}
	log.Println("started 8080")
	log.Println(srv.ListenAndServe())
}
```

Hello Worldを返すエンドポイントが生えてるだけですね。
今回のメインはこちらではないので、さっくり流します。

## client

このclientでは、ArmeriaのService Discovery機能を使って クライアントサイドロードバランシングをします。
[ドキュメント](https://line.github.io/armeria/client-service-discovery.html)を見ると他の例も書かれているので
詳しいことが知りたければそちらを見ると良さそうです。

APIとクライアントの利用例を先に下に示します。

```java
@RestController
public class RootController {

    private final ExternalService externalService;

    RootController(ExternalService externalService) {
        this.externalService = externalService;
    }

    @GetMapping("/")
    public CompletableFuture<String> index() {
        // serverに通信したレスポンスをそのまま返却する
        return externalService.get().thenApply(r -> r.content().toString(StandardCharsets.UTF_8));
    }

}

/**
 * 色々例としてはよくないけど、通信するためのService。 
 */
@Service
public class ExternalService {

    
    public CompletableFuture<AggregatedHttpResponse> get() {
        // 本当はフィールドに持っておいたほうが良さそう
        HttpClient httpClient = HttpClient.of("http://group:backend/"); // URLも外部化しておくべき。
        return httpClient.get("/").aggregate();
    }
}
```

ControllerとServiceをざっと書きましたがすごくシンプルなものです。

見てほしいのは下の部分です。

```java
        HttpClient httpClient = HttpClient.of("http://group:backend/");
```

ここで、Armeriaに対して、`backend` という、EndpointGroupを解決してリクエストを送ってほしい旨を表しています。
では、backendというEndpointGroupはどこで登録しているかというと下の部分です。

```java
@Configuration
public class ArmetriaConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // DNSのSRVレコードでService Discoveryするエンドポイントのグループ
        // backend.internal.localという名前でDNSに問い合わせる
        DnsServiceEndpointGroup group = new DnsServiceEndpointGroupBuilder("backend.internal.local")
                // Custom backoff strategy.
                .backoff(Backoff.exponential(1000, 16000).withJitter(0.3))
                .build();
        // Wait until the initial DNS queries are finished
        group.awaitInitialEndpoints();
        
        // backendという名前で エンドポイントのグループを登録
        EndpointGroupRegistry.register("backend", group, EndpointSelectionStrategy.WEIGHTED_ROUND_ROBIN);
    }
}
```

ドキュメントからほぼコピペです。

これだけで動きます。

Fargateにデプロイするくだりは省略します。
Docker Imageをいい感じに作っていい感じにService Discoveryの機能を有効にして
Fargateでデプロイしてください。

（Fargateにデプロイ後）
すごい！簡単に動きました！便利！
本当に使いやすくて楽だった。

## まとめ

Armeriaで、DNSのSRVレコードのサービスディスカバリを試しました。
`spotify/dns-java` などを自前で使って実装する場合、ラウンドロビンの実装などが必要になってしまうため
もう少し簡単に利用できないものかな？と考えていました。
今回試した、Armeriaは、すごい簡単にService Discoveryを使ったクライアントサイドロードバランシングが可能で
本番に導入していきたいと思います。

今回はマイクロサービス間での利用を考えていますが、Androidでクライアントサイドロードバランシングしたい要求とかってあるのかな？
あんまり聞いたことないんですが、[Envoy Mobile](https://www.publickey1.jp/blog/19/envoy_mobile.html)みたいなのも出てきているので
需要はあるのかな？

今回の記事は、こんなところで。

Armeria、クライアントライブラリとして利用するだけでも非常に便利なので使っていきたい。
OAuth2周りの連携部分を組み込んで、本番導入を狙っていきたい。
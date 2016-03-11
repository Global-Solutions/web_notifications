gsol-web-notifications
======================

HTML5 WebNotification & WebSocket utilities on intra-mart Accel Platform.

* Version 2014.8

## 概要
intra-mart Accel Platform(以下、iAP)において、HTML5 WebNotification & WebSocketのUtilityとWebSocket pushイベントサービスを提供します。
主な技術:
* HTML5 WebNotifications
* HTML5 WebSocket
* HTML5 Shared WebWorker
* HTML5 Promises
* jdk 1.7 concurrency
    * Future
    * ForkJoinPool
* guava Optional
* iAP 非同期タスク

## ファイル構成
* doc: Javadoc
* docs: notifications.coffee's documents
* src
    * main
        * conf/
            * gsol-websocket-taker/: 実装するpluginの設定ファイル群
                * broadcast-taker.xml: broadcastサービスの設定
                * echo-taker.xml: echoサービスの設定
            * im-ehcache-config/web-notifications-broadcast-cache.xml: broadcastサービスで使用
        * java/jp/co/gsol/oss/notifications/
            * impl/
                * contrib/: サンプルの実装など
                    * BroadcastDeferringTask.java: broadcastサービスがメッセージを受信した際にサービスイベントを発火させる
                    * BroadcastManager.java: broadcastサービスのメッセージを管理
                    * BroadcastTakerImpl.java: broadcastサービスでWebSocketメッセージ受信イベントハンドラ
                    * BroadcastTask.java: broadcastサービスのメッセージ配信イベントループ
                    * EchoTakerImpl.java: echoサービス
                    * IntervalDeferringTask.java: 定期発火イベントを発火させる
                * AbstractDeferringTask.java: サービスイベント発火
                * AbstractTakerImpl.java: 空のWebSocketイベントハンドラ
                * AbstractWebSocketTask.java: pushイベントループ
                * ForkjoinCommonPool.java: ForkJoinPoolの共通pool
             * ResinWebSocketListener.java: WebSocketContextをWebSocketTakerに設定する
             * ResinWebSocketServlet.java: WebSocketServlet
             * WebSocketContextManager.java: pushイベントループに使用しているWebSocketContextを管理
             * WebSocketTaker.java: WebSocketイベントハンドラinterface
             * WebSocketTakerManager.java: 実装されているpluginの管理
        * public/notifications/
            * coffee/: WebNotification、WebSocketクライアント側実装
            * js/: coffeeをコンパイルしたjsとsourcemap
        * resources/jp/co/gsol/oss/notifications/impl/contrib/: サンプル実装のdiconファイルなど
        * schema/gsol-websocket-taker.xsd
    * Gruntfile.coffee: coffeeファイルコンパイル設定
    * package.json: npm依存開発パッケージ設定
* LICENSE
* README.md

## 動作環境
* iAP 8.0.7 or later
* resin pro 4.0.37 or later
* jre 1.7 or later
* Google Chrome

## インストール
1. module assembllyを設定し、ビルドパスを通す
2. ユーザモジュールとしてエクスポート
3. jugglingプロジェクトにユーザモジュールを追加、resin-web.xmlに以下を追加
    ```xml
         <servlet-mapping url-pattern="/notifications/websocket" 
            servlet-class="jp.co.gsol.oss.notifications.ResinWebSocketServlet"/>
    ```
4. Warを作成し、Resinにdeploy
5. 任意のpluginを作成

## pluginの作成の仕方
1. gsol-websocket-taker/以下に、実装するサービスの設定ファイルを配置
2. 実装するWebSocketTakerImplのdiconファイルを任意のpathに配置
3. WebSocketTakerを実装(または、AbstractWebSocketTakerを継承)したクラスを作成
    1. ループイベントを実装する場合は、実装したWebSocketTakerクラスのprocessClassが、ループイベントを実行するAbstractWebSocketTaskを継承したクラスの正則パスを返すように実装
    2. ループイベントを処理するAbstractWebSocketTaskを継承したクラスを作成
    3. 任意のイベントをトリガーにしたい場合は、AbstractDeferringTaskを継承したクラスを作成し、AbstractWebSocketTaskを継承したクラスのdeferringTaskがそのクラスの正則パスを返すように実装
4. クライアント側で、notification/js/notifications.min.jsを読み込ませ、notifications.onLoadHandler()、webSockets.onLoadHandler()が実装されるように実装
5. クライアント側の処理を実装

## 設定ファイル conf/gsol-websocket-taker/*.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<tns:gsol-websocket-taker
    xmlns:tns="http://global-solutions.co.jp/notifications/config/gsol-websocket-taker"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://global-solutions.co.jp/notifications/config/gsol-websocket-taker ../../schema/gsol-websocket-taker.xsd ">
    <tns:protocol>protocol-name</tns:protocol>
    <tns:taker>
        <tns:dicon-path>path/to/your.dicon</tns:dicon-path>
        <tns:component-name>ArbitraryComponentName</tns:component-name>
    </tns:taker>
</tns:gsol-websocket-taker>
```

## 著作権および特記事項
このライブラリの著作権は、Global Solutionsが所有しています。
利用者は、GPL version 3にて、本ライブラリを使用することができます。
詳しくは、LICENSEを参照してください。
intra-mart は株式会社 NTT データ イントラマートの登録商標です。

## 連絡先
* github : https://github.com/Global-Solutions

# notifications.coffee は、WebNotificationsとWebSocketのUtilitiesライブラリです。
#
# Copyright 2014 Global Solutions company limited
#
# notifications.coffee may be freely distributed under the GPL-3.0 license.
# - - -


# local jQuery object
$ = @jQuery


# ## WebNotifications implements
# - @$: jQueryの名前が違う場合に、jQueryを設定する
# - @defaultIcon: WebNotificationのデフォルトで表示するアイコンのpathを設定する
# - @requestPermission: WebNotificationの表示権限取得
# - @onLoadHandler: 起動時の処理
# - @showNotification: Notificationの表示
@notifications = (->
    # Notificationが使えるかどうか
    available = Notification?
    # default icon path
    icon = ''

    # ### $({_$}) -> $
    # 内部jQueryオブジェクトの設定 or 取得
    @$ = (_$ = $) ->
        $ = _$
    # ### defaultIcon({_icon}) -> icon
    # デフォルトアイコンpathの設定 or 取得
    @defaultIcon = (_icon = icon) ->
        icon = _icon


    # 表示権限がないかどうか
    permissionNotGranted = (_Notification) ->
        _Notification.permission isnt 'granted'

    # ### requestPermission() -> $.prototype.promise
    # WebNotificationの表示権限取得(非同期)  
    # 表示権限があれば、promiseがresolveされ、
    # なければ、rejectされる。
    @requestPermission = ->
        $deferred = $.Deferred()
        if not available
            return $deferred.reject().promise()
        if permissionNotGranted Notification
            Notification.requestPermission (status) ->
                if Notification.permission isnt status
                    Notification.permission = status
                if permissionNotGranted Notification
                    $deferred.reject()
                else
                    $deferred.resolve()
                return
        else
            $deferred.resolve()
        $deferred.promise()

    # ### onLoadHandler() -> $.prototype.promise
    # 起動時の処理
    # - 表示権限の取得
    # - デフォルトアイコンの設定
    #
    # 表示権限が取得できれば、promiseがresolveされる。
    @onLoadHandler = =>
        $deferred = $.Deferred()
        @requestPermission().done -> $deferred.resolve()
        $('body').one 'click', =>
            @requestPermission().done -> $deferred.resolve()
        icon = $('link[rel=icon]').attr 'href'
        $deferred.promise()

    # ### showNotification(title {, kwArgs}) -> $.prototype.promise
    # WebNotificationの表示
    # #### arguments
    # - title: WebNotificationのタイトル
    # - 名前付き引数:
    #   - options: WebNotificationのoptionsの指定  
    #   iconを変更したい場合は、options:{icon:}に指定
    #   - events: WebNotificationにイベントハンドラを登録  
    #   clickイベントにバインドしたい場合は、events:{click:}に指定
    #   - timeout: WebNotificationを指定時間で閉じたい場合に時間を指定
    #   - closeOnClick: WebNotificationをクリックすると閉じたい場合にtrueを指定
    #
    # WebNotificationが表示されると、promiseがresolveされ、
    # 表示できなかった場合、rejectされる。  
    # promiseハンドラには、WebNotificationオブジェクトとtimeoutIdが渡される。
    #
    # 例: タイトル'test'、メッセージ'test'の120秒で消えるWebNotificationを表示する。
    # (クリックでも閉じられ、close時は、コンソールに出力する。)
    # <pre>
    #       notifications.showNotification('test', {
    #           options: {
    #               icon: 'icon/icon.png',
    #               body: 'test'
    #           },
    #           events: {close: function() {
    #               console.log('close');
    #           },
    #           timeout: 120000,
    #           closeOnClick: true
    #       });
    # </pre>
    @showNotification = (title, {options, events, timeout, closeOnClick} = {}) ->
        $deferred = $.Deferred()

        # iconが指定されてないときは、デフォルトicon
        options?.icon ?= icon
        # WebNotificationを表示
        n = new Notification title, options or icon: icon

        # タイマー設定
        startTimer = ->
            setTimeout (-> n.close()), timeout if timeout?

        # 表示できたかどうか
        $n = $(n).on
            'show': ->
                $deferred.resolve @, startTimer()
                return
            'error': ->
                $deferred.reject @, startTimer()
                return
        # custom events
        $n.on k, v for k, v of events if events
        # クリックしたら閉じる?
        $n.on 'click', (->
            n.close()
            return
        ) if closeOnClick

        # 設定された値をWebNotificationのプロパティに設定
        n.events = events
        n.timeout = timeout
        n.closeOnClick = closeOnClick or false

        $deferred.promise()

    @
).call {}


# WebSocketのwrapper
webSocketConnector = ->
    # WebSocketが使えるかどうか
    available = WebSocket?
    # WebSocket接続の実装
    connect = (server, protocol, messageHandler, events, reconnect, keepalive) ->
        $deferred = $.Deferred()
        # 接続
        ws = new WebSocket server, protocol

        timeoutId = {}
        # 再接続設定などのクリア
        clearConnectionKeeper = ->
            if reconnect
                clearTimeout timeoutId.reconnect if timeoutId.reconnect?
                $(ws).off 'close.reconnect'
                reconnect = false
            if keepalive
                clearTimeout timeoutId.keepalive if timeoutId.keepalive?
                keepalive = false
            return

        # WebSocket操作オブジェクト
        op =
            send: (message) ->
                if ws.readyState is WebSocket.OPEN
                    ws.send message
                    return true
                false
            close: ->
                clearConnectionKeeper()
                ws.close()
                return
        $ws = $(ws).on
            'open.promise': (e) ->
                $deferred.resolve op, e
                return
            'error.promise': (e) ->
                # 接続完了後は、このハンドラでは処理を行わない
                if $deferred.state is 'pending'
                    clearConnectionKeeper()
                    $deferred.reject e
                return
        # メッセージイベント設定
        (attachMessageEvent = ($s) ->
            $s.on 'message.handler', ((e) ->
                messageHandler? e.originalEvent.data
                return
            ) if messageHandler
            return
        ) $ws
        # 関数なら適用、数値なら計算
        callOrVal = (fnOrVal, toVal, count, defaultValue) -> +(fnOrVal?(count) ? toVal(fnOrVal ? defaultValue, count))
        retryCount = 0
        # 再接続イベント設定
        (reconnector = ($s) ->
            if reconnect
                interval = callOrVal reconnect.interval, ((v) -> v), retryCount, 0
                deceleration = callOrVal reconnect.deceleration, ((v, c) -> Math.min(v * c, 600000)), retryCount, 1000
                timeout = interval + deceleration + Math.random() * 5000
                $s.on 'close.reconnect', ->
                    timeoutId.reconnect = setTimeout ->
                        ws = new WebSocket server, protocol
                        $rews = $(ws).on 'open.reconnect', (e) ->
                            retryCount = 0
                            reconnect.done? op, e
                        reconnector $rews
                        attachEvents $rews
                        return
                    , (Math.max 0, timeout)
                    ++retryCount
                    return
                return
        ) $ws
        # カスタムイベント設定
        (attachEvents = ($s, es) ->
            $s.on k, v for k, v of es if es
            return
        ) $ws, events 
        # 接続維持イベント設定
        (->
            pingCount = 0
            (pingSender = ->
                if keepalive
                    interval = callOrVal keepalive.interval, ((v) -> v), pingCount, 60000
                    message = keepalive.message?(pingCount) ? (keepalive.message ? '')
                    timeoutId.keepalive = setTimeout ->
                        op.send message
                        ++pingCount
                        pingSender()
                        return
                    , interval
                return
            )()
            return
        )()

        $deferred.promise()


    # サーバに接続できるか確認
    available: (server, protocol) ->
        $deferred = $.Deferred()
        return $deferred.reject().promise() if not available
        return $deferred.resolve().promise() if available[server]
        connect(server, protocol)
            .done((op) ->
                op.close()
                available[server] = true
                $deferred.resolve()
                return
            ).fail ->
                $deferred.reject()
                return
        $deferred.promise()
    register: (server, protocol, messageHandler, events, reconnect, keepalive) ->
        connect server, protocol, messageHandler, events, reconnect, keepalive

# ### WebSockets implements
# - @contextPath: context pathを設定 or 取得
# - @servletPath: WebSocket servletのpathを設定 or 取得
# - @available: Serverが使えるかどうか
# - @onLoadHandler: 起動時の処理
# - @defaultServer: デフォルトのサーバのpathを設定 or 取得
# - @connect: WebSocketの接続
@webSockets = ((con)->
    # server pathを生成
    serverPath = (proto, host, contextPath = '', servletPath = '') -> "#{proto}//#{host}#{contextPath}#{servletPath}"
    # http: -> ws:, https: -> wss:
    proto = {'http:': 'ws:', 'https:': 'wss:'}[location.protocol]
    host = location.host
    contextPath = ''
    servletPath = '/notifications/websocket'
    defaultServer = serverPath proto, host, contextPath, servletPath
    echoProtocol = 'websocket-echo-protocol'
    # IE does not have location.origin
    location.origin ?= "#{location.protocol}//#{location.host}"

    # ## contextPath({path}) -> contextPath
    # context pathを設定 or 取得
    @contextPath = (path = contextPath) ->
        contextPath = path
    # ## servletPath({path}) -> servletPath
    # WebSocket servletのpathを設定 or 取得
    @servletPath = (path = servletPath) ->
        servletPath = path
    # ## available({server}) -> bool
    # Serverが使えるかどうか
    @available = (server) ->
        con.available server, echoProtocol

    # ## onLoadHandler() -> $.prototype.promise
    # 起動時の処理
    # - デフォルトサーバの設定
    # - デフォルトサーバに接続できるか
    #
    # 接続できれば、promiseがresolveされる
    @onLoadHandler = =>
        $deferred = $.Deferred()
        # base urlから、context pathを設定
        @defaultServer serverPath proto, host, contextPath or $('base').attr('href')?.replace(new RegExp("#{location.origin}(.+)/$"), '$1'), servletPath
        @available(defaultServer).done ->
            $deferred.resolve()
            return
        $deferred.promise()

    # ## defaultServer({server}) -> defaultServer
    # デフォルトのサーバのpathを設定 or 取得
    @defaultServer = (server = defaultServer) ->
        defaultServer = server

    # ## connect({kwArgs}) -> $.prototype.promise
    # - 名前付き引数:
    #   - {server}: 接続先サーバ、未指定の場合はデフォルトサーバに接続
    #   - {protocol}: 接続プロトコル、未指定の場合はプロトコルを指定しない
    #   - {messageHandler}: messageを受信したときのハンドラ  
    #   ハンドラには、メッセージが渡される。
    #   - {events}: WebSocketにイベントハンドラを登録  
    #   closeイベントにバインドしたい場合は、events:{close:}に指定
    #   - {reconnect}: 再接続設定
    #       - {interval}: 再接続までの時間を返す関数(再接続回数が引数に渡される)か数値。デフォルト0
    #       - {deceleration}: 再接続までの時間を返す関数(再接続回数が引数に渡される)か数値、減衰項として使用。デフォルト1000
    #       - {done}: 再接続完了時のイベントハンドラ
    #   - {keepalive}: 接続維持設定
    #       - {interval}: 接続維持するまでの時間を返す関数(接続維持回数が引数に渡される)か数値。デフォルト60000
    #       - {message}: 接続維持する際のpingメッセージを返す関数(接続維持回数が引数に渡される)か文字列。デフォルトは空文字
    #
    # WebSocketが接続されると、promiseがresolveされ、
    # 接続できなかった場合、rejectされる。
    # promiseハンドラには、WebSocket操作オブジェクトが渡される。  
    # WebSocket操作オブジェクト
    # - send(message): messageをサーバに送信する。
    # - close(): WebSocketを閉じる(再接続処理は行わない)。
    #
    # 例: デフォルトサーバに'websocket-test-protocol'で接続し、受け取ったメッセージをコンソールに出力する例  
    # (90秒毎に接続維持を行い、切断された場合は、1秒+[0, 10]分毎に再接続を行う。)
    # <pre>
    #       webSockets.connect({
    #           protocol: 'websocket-test-protocol',
    #           messageHandler: function(m) {
    #               console.log(m);
    #           },
    #           events: {close: function() {
    #               console.log('close');
    #           }},
    #           reconnect: {
    #               interval: 1000,
    #               deceleration: function(c) {
    #                   return Math.min(c * c, 600000);
    #               },
    #               done: function() {
    #                   console.log('done');
    #               }
    #           },
    #           keepalive: {
    #               interval: 900000,
    #               message: function(c) {
    #                   return c:
    #               }
    #           }
    #       }).done(function(ws) {
    #           ws.send('test');
    #       });
    # </pre>
    @connect = ({server, protocol, messageHandler, events, reconnect, keepalive}) ->
        $deferred = $.Deferred()
        server ?= defaultServer
        con.register(server, protocol, messageHandler, events, reconnect, keepalive) 
        .done((o) ->
            $deferred.resolve o
            return
        ).fail (o) ->
            $deferred.reject()
            return
        $deferred.promise()

    @
).call @, webSocketConnector()


# not implemented yet
@sharedWebSockets = (->

).call {} 

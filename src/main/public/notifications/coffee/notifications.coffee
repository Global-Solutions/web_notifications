$ = @jQuery

@notifications = (->
    available = Notification?
    availableSchedule = false
    icon = ''

    @$ = (_$) ->
        $ = _$ if _$?
    @defaultIcon = (_icon) ->
        icon = _icon if _icon?

    @scheduleRequestingPermissionOnLoad = (required) ->
        availableSchedule = available and required ? available

    availableNotPermission = (_Notification) ->
        availableSchedule and _Notification.permission isnt 'granted'

    @requestPermission = ->
        if availableNotPermission Notification
            Notification.requestPermission (status) ->
                if Notification.permission isnt status
                    Notification.permission = status
                return
        return

    @onLoadHandler = =>
        @requestPermission()
        $('body').one 'click', @requestPermission
        icon = $('link[rel=icon]').attr 'href'
        return

    @scheduleOnLoadEvent = =>
        if @scheduleRequestingPermissionOnLoad()
            $(window).on 'load', @onLoadHandler
        return

    @showNotification = (title, {options, events, timeout, closeOnClick} = {}) ->
        $deferred = $.Deferred()

        options?.icon ?= icon
        n = new Notification title, options or icon: icon

        startTimer = ->
            setTimeout (-> n.close()), timeout if timeout?

        $n = $(n).on
            'show': ->
                $deferred.resolve @, startTimer()
                return
            'error': ->
                $deferred.reject @, startTimer()
                return
        $n.on k, v for k, v of events if events
        $n.on 'click', (->
            n.close()
            return
        ) if closeOnClick

        n.events = events
        n.timeout = timeout
        n.closeOnClick = closeOnClick or false

        $deferred.promise()

    @
).call {}

@webSockets =(->
    available = WebSocket?
    serverPath = (proto, host, contextPath = '', servletPath = '') -> "#{proto}//#{host}#{contextPath}#{servletPath}"
    proto = {'http:': 'ws:', 'https:': 'wss:'}[location.protocol]
    host = location.host
    contextPath = ''
    servletPath = '/notifications/websocket'
    defaultServer = serverPath proto, host, contextPath, servletPath
    echoProtocol = 'websocket-echo-protocol'

    @contextPath = (path = contextPath) ->
        contextPath = path
    @servletPath = (path = servletPath) ->
        servletPath = path
    @available = (server) ->
        $deferred = $.Deferred()
        return $deferred.reject().promise() if not available
        return $deferred.resolve().promise() if available[server]
        @connect(server: server, protocol: echoProtocol)
        .done((op) ->
            op.close()
            available[server] = true
            $deferred.resolve()
        ).fail -> $deferred.reject()
        return $deferred.promise()

    @onLoadHandler = =>
        @defaultServer serverPath proto, host, contextPath or $('base').attr('href')?.replace(new RegExp("#{location.origin}(.+)/$"), '$1'), servletPath
        @available(defaultServer)

    @defaultServer = (server = defaultServer) ->
        defaultServer = server

    @connect = ({server, protocol, messageHandler, events, reconnect, keepalive}) ->
        $deferred = $.Deferred()
        server ?= defaultServer
        ws = new WebSocket server, protocol

        timeoutId = {}
        clearConnectionKeeper = ->
            if reconnect
                clearTimeout timeoutId.reconnect if timeoutId.reconnect?
                $(ws).off 'close.reconnect'
                reconnect = false
            if keepalive
                clearTimeout timeoutId.keepalive if timeoutId.keepalive?
                keepalive = false

        op =
            'send': (message) ->
                if ws.readyState is WebSocket.OPEN
                    ws.send message
                    return true
                false
            'close': ->
                clearConnectionKeeper()
                ws.close()
                return

        $ws = $(ws).on
            'open.promise': (e) ->
                $deferred.resolve op, e
                return
            'error.promise': (e) ->
                if $deferred.state is 'pending'
                    clearConnectionKeeper()
                    $deferred.reject e
                return
        (attachMessageEvent = ($s) ->
            $s.on 'message.handler',((e) ->
                messageHandler? e.originalEvent.data
                return
            ) if messageHandler
            return
        ) $ws
        callOrVal = (fnOrVal, toVal, count, defaultValue) -> +(fnOrVal?(count) ? toVal(fnOrVal ? defaultValue, count))
        retryCount = 0
        (reconnector = ($s) ->
            $s.on 'close.reconnect', (->
                timeout = callOrVal(reconnect.interval, ((v) -> v), retryCount, 0) + callOrVal(reconnect.deceleration, ((v, c) -> v * c), retryCount, 1000) + Math.random() * 5000
                timeoutId.reconnect = setTimeout ->
                    ws = new WebSocket server, protocol
                    $rews = $(ws).on 'open.reconnect', (e) ->
                        retryCount = 0
                        reconnect.done? op, e
                    attachMessageEvent $rews
                    reconnector $rews
                    attachEvents $rews, reconnect.events
                    return
                , (Math.max 0, timeout)
                ++retryCount
                return
            ) if reconnect
            return
        ) $ws
        (attachEvents = ($s, es) ->
            $s.on k, v for k, v of es if es
            return
        ) $ws, events
        (->
            pingCount = 0
            (pingSender = ->
                timeoutId.keepalive = setTimeout ->
                    op.send keepalive.message?(pingCount) ? (keepalive.message ? '')
                    ++pingCount
                    pingSender()
                    return
                , (callOrVal keepalive.interval, ((v) -> v), pingCount, 60000) if keepalive
                return
            )()
            return
        )()
        $deferred.promise()

    @
).call {}


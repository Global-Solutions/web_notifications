$ = @jQuery
@notifications = (->
    available = Notification?
    #availableSchedule = false
    icon = ''

    @$ = (_$) ->
        $ = _$ if _$?
    @defaultIcon = (_icon) ->
        icon = _icon if _icon?

    #@scheduleRequestingPermissionOnLoad = (required) ->
    #    availableSchedule = available and required ? available

    permissionNotGranted = (_Notification) ->
        _Notification.permission isnt 'granted'

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

    @onLoadHandler = =>
        $deferred = $.Deferred()
        @requestPermission().done -> $deferred.resolve()
        $('body').one 'click', =>
            @requestPermission().done -> $deferred.resolve()
        icon = $('link[rel=icon]').attr 'href'
        $deferred.promise()

    #@scheduleOnLoadEvent = =>
    #    if @scheduleRequestingPermissionOnLoad()
    #        $(window).on 'load', @onLoadHandler
    #    return

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

webSocketConnector = ->
    available = WebSocket?
    connect = (server, protocol, messageHandler, events, reconnect, keepalive) ->
        $deferred = $.Deferred()
        ws = new WebSocket server, protocol

        #$ws = $(ws)
        #listeners = broadcastEvents.reduce (l, r) ->
        #    l[r] = $.Deferred()
        #, {}
        #(attachEvents = ($s) ->
        #    Object.keys(listeners).forEach (v) ->
        #        $s.on v, (e) ->
        #            listeners[v]?.notify e
        #            return
        #        return
        #    return
        #) $ws

        timeoutId = {}
        clearConnectionKeeper = ->
            if reconnect
                clearTimeout timeoutId.reconnect if timeoutId.reconnect?
                $(ws).off 'close.reconnect'
                reconnect = false
            if keepalive
                clearTimeout timeoutId.keepalive if timeoutId.keepalive?
                keepalive = false
            return

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
            #on: (event, callback) ->
            #    listeners[event]?.progress callback.bind @, @
            #    @
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
            if reconnect
                interval = callOrVal reconnect.interval, ((v) -> v), retryCount, 0
                deceleration = callOrVal reconnect.deceleration, ((v, c) -> v * c), retryCount, 1000
                timeout = interval + deceleration + Math.random() * 5000
                $s.on 'close.reconnect', ->
                    #timeout = callOrVal(reconnect.interval, ((v) -> v), retryCount, 0) + callOrVal(reconnect.deceleration, ((v, c) -> v * c), retryCount, 1000) + Math.random() * 5000
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
        (attachEvents = ($s, es) ->
            $s.on k, v for k, v of es if es
            return
        ) $ws, events 
        (->
            pingCount = 0
            (pingSender = ->
                if keepalive
                    interval = callOrVal keepalive.interval, ((v) -> v), pingCount, 60000
                    message = keepalive.message?(pingCount) ? (keepalive.message ? '')
                    timeoutId.keepalive = setTimeout ->
                        op.send message #keepalive.message?(pingCount) ? (keepalive.message ? '')
                        ++pingCount
                        pingSender()
                        return
                    , interval #(callOrVal keepalive.interval, ((v) -> v), pingCount, 60000) if keepalive
                return
            )()
            return
        )()

        $deferred.promise()


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




@webSockets = ((con)->
    #available = WebSocket?
    serverPath = (proto, host, contextPath = '', servletPath = '') -> "#{proto}//#{host}#{contextPath}#{servletPath}"
    proto = {'http:': 'ws:', 'https:': 'wss:'}[location.protocol]
    host = location.host
    contextPath = ''
    servletPath = '/notifications/websocket'
    defaultServer = serverPath proto, host, contextPath, servletPath
    echoProtocol = 'websocket-echo-protocol'
    # IE does not have location.origin
    location.origin ?= "#{location.protocol}//#{location.host}"

    @contextPath = (path = contextPath) ->
        contextPath = path
    @servletPath = (path = servletPath) ->
        servletPath = path
    @available = (server) ->
        con.available server, echoProtocol
    #    $deferred = $.Deferred()
    #    return $deferred.reject().promise() if not available
    #    return $deferred.resolve().promise() if available[server]
    #    @connect(server: server, protocol: echoProtocol)
    #    .done((op) ->
    #        op.close()
    #        available[server] = true
    #        $deferred.resolve()
    #        return
    #    ).fail ->
    #        $deferred.reject()
    #        return
    #    $deferred.promise()

    @onLoadHandler = =>
        $deferred = $.Deferred()
        @defaultServer serverPath proto, host, contextPath or $('base').attr('href')?.replace(new RegExp("#{location.origin}(.+)/$"), '$1'), servletPath
        @available(defaultServer).done ->
            $deferred.resolve()
            return
        $deferred.promise()

    @defaultServer = (server = defaultServer) ->
        defaultServer = server

    @connect = ({server, protocol, messageHandler, events, reconnect, keepalive}) ->
        $deferred = $.Deferred()
        server ?= defaultServer
        #ws = new WebSocket server, protocol
        #candEvents = ['open', 'error', 'close', 'message']
        #    .concat(Object.keys events)
        #    .filter (v, i, s) -> s.indexOf(v) is i
        con.register(server, protocol, messageHandler, events, reconnect, keepalive) 
        .done((o) ->
            $deferred.resolve o
            return
        ).fail (o) ->
            $deferred.reject()
            return
        #timeoutId = {}
        #clearConnectionKeeper = ->
        #    if reconnect
        #        clearTimeout timeoutId.reconnect if timeoutId.reconnect?
        #        $(ws).off 'close.reconnect'
        #        reconnect = false
        #    if keepalive
        #        clearTimeout timeoutId.keepalive if timeoutId.keepalive?
        #        keepalive = false

        #op =
        #    'send': (message) ->
        #        if ws.readyState is WebSocket.OPEN
        #            ws.send message
        #            return true
        #        false
        #    'close': ->
        #        clearConnectionKeeper()
        #        ws.close()
        #        return

        #op.on('open', (o, m) ->
        #    $deferred.resolve op, m
        #    return
        #).on('error', (o, m) ->
        #    if $deferred.state is 'pending'
        #        $deferred.reject m
        #    return
        #)
        #$ws = $(ws).on
        #    'open.promise': (e) ->
        #        $deferred.resolve op, e
        #        return
        #    'error.promise': (e) ->
        #        if $deferred.state is 'pending'
        #            clearConnectionKeeper()
        #            $deferred.reject e
        #        return
        #(attachMessageEvent = ($s) ->
        #    $s.on 'message.handler',((e) ->
        #        messageHandler? e.originalEvent.data
        #        return
        #    ) if messageHandler
        #    return
        #) $ws
        #callOrVal = (fnOrVal, toVal, count, defaultValue) -> +(fnOrVal?(count) ? toVal(fnOrVal ? defaultValue, count))
        #retryCount = 0
        #(reconnector = ($s) ->
        #    $s.on 'close.reconnect', (->
        #        timeout = callOrVal(reconnect.interval, ((v) -> v), retryCount, 0) + callOrVal(reconnect.deceleration, ((v, c) -> v * c), retryCount, 1000) + Math.random() * 5000
        #        timeoutId.reconnect = setTimeout ->
        #            ws = new WebSocket server, protocol
        #            $rews = $(ws).on 'open.reconnect', (e) ->
        #                retryCount = 0
        #                reconnect.done? op, e
        #            attachMessageEvent $rews
        #            reconnector $rews
        #            attachEvents $rews, reconnect.events
        #            return
        #        , (Math.max 0, timeout)
        #        ++retryCount
        #        return
        #    ) if reconnect
        #    return
        #) $ws
        #(attachEvents = ($s, es) ->
        #    $s.on k, v for k, v of es if es
        #    return
        #) $ws, events
        #(->
        #    pingCount = 0
        #    (pingSender = ->
        #        timeoutId.keepalive = setTimeout ->
        #            op.send keepalive.message?(pingCount) ? (keepalive.message ? '')
        #            ++pingCount
        #            pingSender()
        #            return
        #        , (callOrVal keepalive.interval, ((v) -> v), pingCount, 60000) if keepalive
        #        return
        #    )()
        #    return
        #)()
        $deferred.promise()

    @
).call @, webSocketConnector()

@sharedWebSockets = (->

).call {} 

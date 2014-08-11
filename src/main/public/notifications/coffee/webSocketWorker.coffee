# in production
ports = []
resolvers = {}

availables = {} if WebSocket?
connections = {}

connect = (server, protocol, messageHandler, events, reconnect, keepalive) ->
    new Promise (resolve, reject) -> 
        ws = new WebSocket server, protocol

        timeoutId = {}
        reconnectHandler = undefined
        clearConnectionKeeper = ->
            if reconnect
                clearTimeout timeoutId.reconnect if timeoutId.reconnect?
                ws.removeEventListener 'close', reconnectHandler if reconnectHandler
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
        promiseState = 'pending'
        ws.addEventListener 'open', (e) ->
            resolve op, e
            promiseState = 'resolved'
            return
        ws.addEventListener 'error', (e) ->
            if promiseState is 'pending'
                clearConnectionKeeper()
                reject e
                promiseState = 'rejected'
            return

        (attachMessageEvent = (sock) ->
            sock.addEventListener 'message', ((e) ->
                messageHandler? e.data
                return
            ) if messageHandler
            return
        ) ws
        callOrVal = (fnOrVal, toVal, count, defaultValue) -> +(fnOrVal?(count) ? toVal(fnOrVal ? defaultValue, count))
        retryCount = 0
        (reconnector = (sock) ->
            if reconnect
                interval = callOrVal reconnect.interval, ((v) -> v), retryCount, 0
                deceleration = callOrVal reconnect.deceleration, ((v, c) -> v * c), retryCount, 1000
                timeout = interval + deceleration + Math.random() * 5000
                sock.addEventListener 'close', (reconnectHandler = ->
                    timeoutId.reconnect = setTimeout ->
                        ws = new WebSocket server, protocol
                        ws.addEventListener 'open', (e) ->
                            retryCount = 0
                            reconnect.done? op, e
                            return
                        reconnector ws
                        attachEvents ws
                        return
                    , (Math.max 0, timeout)
                    ++retryCount
                    return
                )
                return
        ) ws
        (attachEvents = (sock, es) ->
            sock.addEventListener k, v for k, v of es if es
            return
        ) ws, events 
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
        return

receivableCommands =
    pong: (a) ->
        resolvers[a]?()
        delete resolvers[a]
        return
    available: (a) ->
        new Promise (resolve, reject) ->
            if not availables?
                reject()
                return
            server = a.server
            protocol = a.protocol
            key = "#{server}-#{protocol}"
            if availables[key]
                resolve()
                return

            connect(server, protocol).then(
                (op) ->
                    op.close()
                    availables[key] = true
                    resolve()
                    return
                , ->
                    reject()
                    return
            )
            return
    register: (a, p) ->
        new Promise (resolve, reject) ->
            server = a.server
            protocol = a.protocol
            
            key = "#{server}-#{protocol}"
            connections[key] ?= con: undefined, ports: []
            if connections[key].ports.indexOf(p) > -1
                resolve 'registered'
                return
            connections[key].ports.push p

            resolve 'tobe'
            return

    send: (a) ->
        return
    close: (a) ->
        return
    keepalive: (a) ->
        return
    reconnect: (a) ->
        return
    message: (a) ->
        ps = fetchAlivePorts()
        sendTo1stResolved ps, a
        maintainAlivePort ps
        return

sendTo1stResolved = (ports, data) ->
    Promise.race(ports).then (v) ->
        v.postMessage data
        return
    return
maintainAlivePort = (ports) ->
    Promise.all(ports.map (v) -> new Promise (resolve, reject) ->
        v.then (d) -> 
            resolve d
            return
        ,(d) ->
            d.close()
            resolve false
            return
        return
    ).then (vs) ->
        ports = vs.filter (v) -> v
        return
    return

fetchAlivePorts = -> 
   ports.map (v, i) -> new Promise (resolve, reject) ->
        key = "#{Math.random()}:#{i}"
        v.postMessage ping: key
        resolvers[key] = resolve.bind @, v
        setTimeout ->
            reject v
            delete resolvers[key]
            return
        , 5000
        return

responseData = (key, result, args, data) ->
    o = {}
    o[key] =
        result: result
        args: args
        data: data

@onconnect = (e) ->
    port = e.ports[0]
    ports.push port
    port.postMessage 'yo:' + ports.length
    console.log 'yo'
    port.onmessage = (ev) ->
        console.log ev
        for k, v of ev.data
            receivableCommands[k]?(v, port)?.then(
                (r) ->
                    port.postMessage responseData k, true, v, r
                    return
                , (r) ->
                    port.postMessage responseData k, false, v, r
                    return
            )
        
        return

    return

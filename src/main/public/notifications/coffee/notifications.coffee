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
        n = new Notification title, options || icon: icon

        $(n).on
            'show': -> $deferred.resolve @
            'error': -> $deferred.reject @
        $(n).on k, v for k, v of events if events
        $(n).on 'click', (-> n.close()) if closeOnClick

        setTimeout (-> n.close()), timeout if timeout?

        n.events = events
        n.timeout = timeout
        n.closeOnClick = closeOnClick or false

        $deferred.promise()

    @
).call {}

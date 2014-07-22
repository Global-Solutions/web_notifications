(function() {
  var $;

  $ = this.jQuery;

  this.notifications = (function() {
    var available, availableNotPermission, availableSchedule, icon;
    available = typeof Notification !== "undefined" && Notification !== null;
    availableSchedule = false;
    icon = '';
    this.$ = function(_$) {
      if (_$ != null) {
        return $ = _$;
      }
    };
    this.defaultIcon = function(_icon) {
      if (_icon != null) {
        return icon = _icon;
      }
    };
    this.scheduleRequestingPermissionOnLoad = function(required) {
      var _ref;
      return availableSchedule = (_ref = available && required) != null ? _ref : available;
    };
    availableNotPermission = function(_Notification) {
      return availableSchedule && _Notification.permission !== 'granted';
    };
    this.requestPermission = function() {
      if (availableNotPermission(Notification)) {
        Notification.requestPermission(function(status) {
          if (Notification.permission !== status) {
            Notification.permission = status;
          }
        });
      }
    };
    this.onLoadHandler = (function(_this) {
      return function() {
        _this.requestPermission();
        $('body').one('click', _this.requestPermission);
        icon = $('link[rel=icon]').attr('href');
      };
    })(this);
    this.scheduleOnLoadEvent = (function(_this) {
      return function() {
        if (_this.scheduleRequestingPermissionOnLoad()) {
          $(window).on('load', _this.onLoadHandler);
        }
      };
    })(this);
    this.showNotification = function(title, _arg) {
      var $deferred, $n, closeOnClick, events, k, n, options, startTimer, timeout, v, _ref;
      _ref = _arg != null ? _arg : {}, options = _ref.options, events = _ref.events, timeout = _ref.timeout, closeOnClick = _ref.closeOnClick;
      $deferred = $.Deferred();
      if (options != null) {
        if (options.icon == null) {
          options.icon = icon;
        }
      }
      n = new Notification(title, options || {
        icon: icon
      });
      startTimer = function() {
        if (timeout != null) {
          return setTimeout((function() {
            return n.close();
          }), timeout);
        }
      };
      $n = $(n).on({
        'show': function() {
          $deferred.resolve(this, startTimer());
        },
        'error': function() {
          $deferred.reject(this, startTimer());
        }
      });
      if (events) {
        for (k in events) {
          v = events[k];
          $n.on(k, v);
        }
      }
      if (closeOnClick) {
        $n.on('click', (function() {
          n.close();
        }));
      }
      n.events = events;
      n.timeout = timeout;
      n.closeOnClick = closeOnClick || false;
      return $deferred.promise();
    };
    return this;
  }).call({});

  this.webSockets = (function() {
    var available, contextPath, defaultServer, echoProtocol, host, proto, serverPath, servletPath;
    available = typeof WebSocket !== "undefined" && WebSocket !== null;
    serverPath = function(proto, host, contextPath, servletPath) {
      if (contextPath == null) {
        contextPath = '';
      }
      if (servletPath == null) {
        servletPath = '';
      }
      return "" + proto + "//" + host + contextPath + servletPath;
    };
    proto = {
      'http:': 'ws:',
      'https:': 'wss:'
    }[location.protocol];
    host = location.host;
    contextPath = '';
    servletPath = '/notifications/websocket';
    defaultServer = serverPath(proto, host, contextPath, servletPath);
    echoProtocol = 'websocket-echo-protocol';
    this.contextPath = function(path) {
      if (path == null) {
        path = contextPath;
      }
      return contextPath = path;
    };
    this.servletPath = function(path) {
      if (path == null) {
        path = servletPath;
      }
      return servletPath = path;
    };
    this.available = function(server) {
      var $deferred;
      $deferred = $.Deferred();
      if (!available) {
        return $deferred.reject().promise();
      }
      if (available[server]) {
        return $deferred.resolve().promise();
      }
      this.connect({
        server: server,
        protocol: echoProtocol
      }).done(function(op) {
        op.close();
        available[server] = true;
        return $deferred.resolve();
      }).fail(function() {
        return $deferred.reject();
      });
      return $deferred.promise();
    };
    this.onLoadHandler = (function(_this) {
      return function() {
        var _ref;
        _this.defaultServer(serverPath(proto, host, contextPath || ((_ref = $('base').attr('href')) != null ? _ref.replace(new RegExp("" + location.origin + "(.+)/$"), '$1') : void 0), servletPath));
        return _this.available(defaultServer);
      };
    })(this);
    this.defaultServer = function(server) {
      if (server == null) {
        server = defaultServer;
      }
      return defaultServer = server;
    };
    this.connect = function(_arg) {
      var $deferred, $ws, attachEvents, attachMessageEvent, callOrVal, clearConnectionKeeper, events, keepalive, messageHandler, op, protocol, reconnect, reconnector, retryCount, server, timeoutId, ws;
      server = _arg.server, protocol = _arg.protocol, messageHandler = _arg.messageHandler, events = _arg.events, reconnect = _arg.reconnect, keepalive = _arg.keepalive;
      $deferred = $.Deferred();
      if (server == null) {
        server = defaultServer;
      }
      ws = new WebSocket(server, protocol);
      timeoutId = {};
      clearConnectionKeeper = function() {
        if (reconnect) {
          if (timeoutId.reconnect != null) {
            clearTimeout(timeoutId.reconnect);
          }
          $(ws).off('close.reconnect');
          reconnect = false;
        }
        if (keepalive) {
          if (timeoutId.keepalive != null) {
            clearTimeout(timeoutId.keepalive);
          }
          return keepalive = false;
        }
      };
      op = {
        'send': function(message) {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(message);
            return true;
          }
          return false;
        },
        'close': function() {
          clearConnectionKeeper();
          ws.close();
        }
      };
      $ws = $(ws).on({
        'open.promise': function(e) {
          $deferred.resolve(op, e);
        },
        'error.promise': function(e) {
          if ($deferred.state === 'pending') {
            clearConnectionKeeper();
            $deferred.reject(e);
          }
        }
      });
      (attachMessageEvent = function($s) {
        if (messageHandler) {
          $s.on('message.handler', (function(e) {
            if (typeof messageHandler === "function") {
              messageHandler(e.originalEvent.data);
            }
          }));
        }
      })($ws);
      callOrVal = function(fnOrVal, toVal, count, defaultValue) {
        var _ref;
        return +((_ref = typeof fnOrVal === "function" ? fnOrVal(count) : void 0) != null ? _ref : toVal(fnOrVal != null ? fnOrVal : defaultValue, count));
      };
      retryCount = 0;
      (reconnector = function($s) {
        if (reconnect) {
          $s.on('close.reconnect', (function() {
            var timeout;
            timeout = callOrVal(reconnect.interval, (function(v) {
              return v;
            }), retryCount, 0) + callOrVal(reconnect.deceleration, (function(v, c) {
              return v * c;
            }), retryCount, 1000) + Math.random() * 5000;
            timeoutId.reconnect = setTimeout(function() {
              var $rews;
              ws = new WebSocket(server, protocol);
              $rews = $(ws).on('open.reconnect', function(e) {
                retryCount = 0;
                return typeof reconnect.done === "function" ? reconnect.done(op, e) : void 0;
              });
              attachMessageEvent($rews);
              reconnector($rews);
              attachEvents($rews, reconnect.events);
            }, Math.max(0, timeout));
            ++retryCount;
          }));
        }
      })($ws);
      (attachEvents = function($s, es) {
        var k, v;
        if (es) {
          for (k in es) {
            v = es[k];
            $s.on(k, v);
          }
        }
      })($ws, events);
      (function() {
        var pingCount, pingSender;
        pingCount = 0;
        (pingSender = function() {
          if (keepalive) {
            timeoutId.keepalive = setTimeout(function() {
              var _ref, _ref1;
              op.send((_ref = typeof keepalive.message === "function" ? keepalive.message(pingCount) : void 0) != null ? _ref : (_ref1 = keepalive.message) != null ? _ref1 : '');
              ++pingCount;
              pingSender();
            }, callOrVal(keepalive.interval, (function(v) {
              return v;
            }), pingCount, 60000));
          }
        })();
      })();
      return $deferred.promise();
    };
    return this;
  }).call({});

}).call(this);

//# sourceMappingURL=notifications.js.map

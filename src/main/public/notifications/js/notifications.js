(function() {
  var $, webSocketConnector;

  $ = this.jQuery;

  this.notifications = (function() {
    var available, icon, permissionNotGranted;
    available = typeof Notification !== "undefined" && Notification !== null;
    icon = '';
    this.$ = function(_$) {
      if (_$ == null) {
        _$ = $;
      }
      return $ = _$;
    };
    this.defaultIcon = function(_icon) {
      if (_icon == null) {
        _icon = icon;
      }
      return icon = _icon;
    };
    permissionNotGranted = function(_Notification) {
      return _Notification.permission !== 'granted';
    };
    this.requestPermission = function() {
      var $deferred;
      $deferred = $.Deferred();
      if (!available) {
        return $deferred.reject().promise();
      }
      if (permissionNotGranted(Notification)) {
        Notification.requestPermission(function(status) {
          if (Notification.permission !== status) {
            Notification.permission = status;
          }
          if (permissionNotGranted(Notification)) {
            $deferred.reject();
          } else {
            $deferred.resolve();
          }
        });
      } else {
        $deferred.resolve();
      }
      return $deferred.promise();
    };
    this.onLoadHandler = (function(_this) {
      return function() {
        var $deferred;
        $deferred = $.Deferred();
        _this.requestPermission().done(function() {
          return $deferred.resolve();
        });
        $('body').one('click', function() {
          return _this.requestPermission().done(function() {
            return $deferred.resolve();
          });
        });
        icon = $('link[rel=icon]').attr('href');
        return $deferred.promise();
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

  webSocketConnector = function() {
    var available, connect;
    available = typeof WebSocket !== "undefined" && WebSocket !== null;
    connect = function(server, protocol, messageHandler, events, reconnect, keepalive) {
      var $deferred, $ws, attachEvents, attachMessageEvent, callOrVal, clearConnectionKeeper, op, reconnector, retryCount, timeoutId, ws;
      $deferred = $.Deferred();
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
          keepalive = false;
        }
      };
      op = {
        send: function(message) {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(message);
            return true;
          }
          return false;
        },
        close: function() {
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
        var deceleration, interval, timeout;
        if (reconnect) {
          interval = callOrVal(reconnect.interval, (function(v) {
            return v;
          }), retryCount, 0);
          deceleration = callOrVal(reconnect.deceleration, (function(v, c) {
            return Math.min(v * c, 600000);
          }), retryCount, 1000);
          timeout = interval + deceleration + Math.random() * 5000;
          $s.on('close.reconnect', function() {
            timeoutId.reconnect = setTimeout(function() {
              var $rews;
              ws = new WebSocket(server, protocol);
              $rews = $(ws).on('open.reconnect', function(e) {
                retryCount = 0;
                return typeof reconnect.done === "function" ? reconnect.done(op, e) : void 0;
              });
              reconnector($rews);
              attachEvents($rews);
            }, Math.max(0, timeout));
            ++retryCount;
          });
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
          var interval, message, _ref, _ref1;
          if (keepalive) {
            interval = callOrVal(keepalive.interval, (function(v) {
              return v;
            }), pingCount, 60000);
            message = (_ref = typeof keepalive.message === "function" ? keepalive.message(pingCount) : void 0) != null ? _ref : (_ref1 = keepalive.message) != null ? _ref1 : '';
            timeoutId.keepalive = setTimeout(function() {
              op.send(message);
              ++pingCount;
              pingSender();
            }, interval);
          }
        })();
      })();
      return $deferred.promise();
    };
    return {
      available: function(server, protocol) {
        var $deferred;
        $deferred = $.Deferred();
        if (!available) {
          return $deferred.reject().promise();
        }
        if (available[server]) {
          return $deferred.resolve().promise();
        }
        connect(server, protocol).done(function(op) {
          op.close();
          available[server] = true;
          $deferred.resolve();
        }).fail(function() {
          $deferred.reject();
        });
        return $deferred.promise();
      },
      register: function(server, protocol, messageHandler, events, reconnect, keepalive) {
        return connect(server, protocol, messageHandler, events, reconnect, keepalive);
      }
    };
  };

  this.webSockets = (function(con) {
    var contextPath, defaultServer, echoProtocol, host, proto, serverPath, servletPath;
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
    if (location.origin == null) {
      location.origin = "" + location.protocol + "//" + location.host;
    }
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
      return con.available(server, echoProtocol);
    };
    this.onLoadHandler = (function(_this) {
      return function() {
        var $deferred, _ref;
        $deferred = $.Deferred();
        _this.defaultServer(serverPath(proto, host, contextPath || ((_ref = $('base').attr('href')) != null ? _ref.replace(new RegExp("" + location.origin + "(.+)/$"), '$1') : void 0), servletPath));
        _this.available(defaultServer).done(function() {
          $deferred.resolve();
        });
        return $deferred.promise();
      };
    })(this);
    this.defaultServer = function(server) {
      if (server == null) {
        server = defaultServer;
      }
      return defaultServer = server;
    };
    this.connect = function(_arg) {
      var $deferred, events, keepalive, messageHandler, protocol, reconnect, server;
      server = _arg.server, protocol = _arg.protocol, messageHandler = _arg.messageHandler, events = _arg.events, reconnect = _arg.reconnect, keepalive = _arg.keepalive;
      $deferred = $.Deferred();
      if (server == null) {
        server = defaultServer;
      }
      con.register(server, protocol, messageHandler, events, reconnect, keepalive).done(function(o) {
        $deferred.resolve(o);
      }).fail(function(o) {
        $deferred.reject();
      });
      return $deferred.promise();
    };
    return this;
  }).call(this, webSocketConnector());

  this.sharedWebSockets = (function() {}).call({});

}).call(this);

//# sourceMappingURL=notifications.js.map

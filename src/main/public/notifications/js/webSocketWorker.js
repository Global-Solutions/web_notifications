(function() {
  var availables, connect, connections, fetchAlivePorts, maintainAlivePort, ports, receivableCommands, resolvers, responseData, sendTo1stResolved;

  ports = [];

  resolvers = {};

  if (typeof WebSocket !== "undefined" && WebSocket !== null) {
    availables = {};
  }

  connections = {};

  connect = function(server, protocol, messageHandler, events, reconnect, keepalive) {
    return new Promise(function(resolve, reject) {
      var attachEvents, attachMessageEvent, callOrVal, clearConnectionKeeper, op, promiseState, reconnectHandler, reconnector, retryCount, timeoutId, ws;
      ws = new WebSocket(server, protocol);
      timeoutId = {};
      reconnectHandler = void 0;
      clearConnectionKeeper = function() {
        if (reconnect) {
          if (timeoutId.reconnect != null) {
            clearTimeout(timeoutId.reconnect);
          }
          if (reconnectHandler) {
            ws.removeEventListener('close', reconnectHandler);
          }
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
      promiseState = 'pending';
      ws.addEventListener('open', function(e) {
        resolve(op, e);
        promiseState = 'resolved';
      });
      ws.addEventListener('error', function(e) {
        if (promiseState === 'pending') {
          clearConnectionKeeper();
          reject(e);
          promiseState = 'rejected';
        }
      });
      (attachMessageEvent = function(sock) {
        if (messageHandler) {
          sock.addEventListener('message', (function(e) {
            if (typeof messageHandler === "function") {
              messageHandler(e.data);
            }
          }));
        }
      })(ws);
      callOrVal = function(fnOrVal, toVal, count, defaultValue) {
        var _ref;
        return +((_ref = typeof fnOrVal === "function" ? fnOrVal(count) : void 0) != null ? _ref : toVal(fnOrVal != null ? fnOrVal : defaultValue, count));
      };
      retryCount = 0;
      (reconnector = function(sock) {
        var deceleration, interval, timeout;
        if (reconnect) {
          interval = callOrVal(reconnect.interval, (function(v) {
            return v;
          }), retryCount, 0);
          deceleration = callOrVal(reconnect.deceleration, (function(v, c) {
            return v * c;
          }), retryCount, 1000);
          timeout = interval + deceleration + Math.random() * 5000;
          sock.addEventListener('close', (reconnectHandler = function() {
            timeoutId.reconnect = setTimeout(function() {
              ws = new WebSocket(server, protocol);
              ws.addEventListener('open', function(e) {
                retryCount = 0;
                if (typeof reconnect.done === "function") {
                  reconnect.done(op, e);
                }
              });
              reconnector(ws);
              attachEvents(ws);
            }, Math.max(0, timeout));
            ++retryCount;
          }));
        }
      })(ws);
      (attachEvents = function(sock, es) {
        var k, v;
        if (es) {
          for (k in es) {
            v = es[k];
            sock.addEventListener(k, v);
          }
        }
      })(ws, events);
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
    });
  };

  receivableCommands = {
    pong: function(a) {
      if (typeof resolvers[a] === "function") {
        resolvers[a]();
      }
      delete resolvers[a];
    },
    available: function(a) {
      return new Promise(function(resolve, reject) {
        var key, protocol, server;
        if (availables == null) {
          reject();
          return;
        }
        server = a.server;
        protocol = a.protocol;
        key = "" + server + "-" + protocol;
        if (availables[key]) {
          resolve();
          return;
        }
        connect(server, protocol).then(function(op) {
          op.close();
          availables[key] = true;
          resolve();
        }, function() {
          reject();
        });
      });
    },
    register: function(a, p) {
      return new Promise(function(resolve, reject) {
        var key, protocol, server;
        server = a.server;
        protocol = a.protocol;
        key = "" + server + "-" + protocol;
        if (connections[key] == null) {
          connections[key] = {
            con: void 0,
            ports: []
          };
        }
        if (connections[key].ports.indexOf(p) > -1) {
          resolve('registered');
          return;
        }
        connections[key].ports.push(p);
        resolve('tobe');
      });
    },
    send: function(a) {},
    close: function(a) {},
    keepalive: function(a) {},
    reconnect: function(a) {},
    message: function(a) {
      var ps;
      ps = fetchAlivePorts();
      sendTo1stResolved(ps, a);
      maintainAlivePort(ps);
    }
  };

  sendTo1stResolved = function(ports, data) {
    Promise.race(ports).then(function(v) {
      v.postMessage(data);
    });
  };

  maintainAlivePort = function(ports) {
    Promise.all(ports.map(function(v) {
      return new Promise(function(resolve, reject) {
        v.then(function(d) {
          resolve(d);
        }, function(d) {
          d.close();
          resolve(false);
        });
      });
    })).then(function(vs) {
      ports = vs.filter(function(v) {
        return v;
      });
    });
  };

  fetchAlivePorts = function() {
    return ports.map(function(v, i) {
      return new Promise(function(resolve, reject) {
        var key;
        key = "" + (Math.random()) + ":" + i;
        v.postMessage({
          ping: key
        });
        resolvers[key] = resolve.bind(this, v);
        setTimeout(function() {
          reject(v);
          delete resolvers[key];
        }, 5000);
      });
    });
  };

  responseData = function(key, result, args, data) {
    var o;
    o = {};
    return o[key] = {
      result: result,
      args: args,
      data: data
    };
  };

  this.onconnect = function(e) {
    var port;
    port = e.ports[0];
    ports.push(port);
    port.postMessage('yo:' + ports.length);
    console.log('yo');
    port.onmessage = function(ev) {
      var k, v, _ref, _ref1;
      console.log(ev);
      _ref = ev.data;
      for (k in _ref) {
        v = _ref[k];
        if (typeof receivableCommands[k] === "function") {
          if ((_ref1 = receivableCommands[k](v, port)) != null) {
            _ref1.then(function(r) {
              port.postMessage(responseData(k, true, v, r));
            }, function(r) {
              port.postMessage(responseData(k, false, v, r));
            });
          }
        }
      }
    };
  };

}).call(this);

//# sourceMappingURL=webSocketWorker.js.map

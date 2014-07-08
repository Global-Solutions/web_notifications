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

}).call(this);

//# sourceMappingURL=notifications.js.map

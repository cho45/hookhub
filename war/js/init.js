Global = (function () {
	var _http = this._http;
	this.http = function (opts) {
		return _http.request(opts);
	}

	http.get   = function (url)       {
		return http({
			method : "get",
			url    : url
		})
	};

	http.post  = function (url, params) {
		return http({
			method  : "post",
			url     : url,
			data    : http.data(params),
			headers : {
				"Content-Type":"application/x-www-form-urlencoded"
			}
		})
	};

	http.data = function (params) {
		var ret = [];
		for (var key in params) if (params.hasOwnProperty(key)) {
			var val = params[key];
			if (typeof(val) == "string") {
				ret.push(encodeURIComponent(key) + "=" + encodeURIComponent(val));
			} else
			if (val instanceof Array) {
				for (var i = 0, len = val.length; i < len; i++) {
					ret.push(encodeURIComponent(key) + "=" + encodeURIComponent(val[i]));
				}
			}
		}
		return ret.join('&');
	}

	delete this._http;
	delete stash.params.token;
	return this;
})();


Global = (function () {
	var _proxy = this._proxy;

	this.http = function (opts) {
		return _proxy.request(opts);
	}

	this.http.get   = function (url)       {
		return http({
			method : "get",
			url    : url
		})
	};

	this.http.post  = function (url, params) {
		return http({
			method  : "post",
			url     : url,
			data    : http.data(params),
			headers : {
				"Content-Type":"application/x-www-form-urlencoded"
			}
		})
	};

	this.http.data = function (params) {
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

	this.mail = function (title, body) {
		_proxy.mail(String(title), String(body))
	}

	this.util = {
		base64 : {
			encode : function (data) {
				return String(_proxy.base64_encode(data))
			}
		},
		digest : {
			md5 : function (data) {
				return String(_proxy.digest_md5(data))
			},
			sha1 : function (data) {
				return String(_proxy.digest_sha1(data))
			}
		},
//		hmac : {
//			md5 : function (key, data) {
//				return String(_proxy.hmac_md5(data))
//			},
//			sha1 : function (key, data) {
//				return String(_proxy.hmac_sha1(data))
//			}
//		}
	};

	delete this._proxy;
	delete stash.params.token;
	return this;
})();

